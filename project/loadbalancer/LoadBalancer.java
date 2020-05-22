package loadbalancer;
/*
 * Copyright 2012-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.concurrent.Executors;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.MonitorInstancesRequest;
import com.amazonaws.services.ec2.model.UnmonitorInstancesRequest;



import pt.ulisboa.tecnico.cnv.server.WebServer;
import awsclient.AmazonDynamoDBSample;
import util.ServerHelper;

public class LoadBalancer {
    static AmazonEC2 ec2Client;
    static AmazonCloudWatch cloudWatch;
    static String ownInstanceIp;
    static Random rand = new Random();

    enum MetricLevel {
        HIGH,
        MEDIUM,
        LOW
    }

    private static MetricLevel getMetricLevel(float metricValue) {
        if (metricValue == 0.0) return MetricLevel.MEDIUM; // if the request is not in the db yet
        if (metricValue > 0.02) return MetricLevel.HIGH;
        if (metricValue < 0.01) return MetricLevel.LOW;
        return MetricLevel.MEDIUM;
    }

    // function to sort hashmap by values 
    private static List<String> sortByValue(HashMap<String, Double> hm) 
    { 
        // Create a list from elements of HashMap 
        List<Map.Entry<String, Double> > list = new LinkedList<Map.Entry<String, Double> >(hm.entrySet()); 
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() { 
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        });
        // put data from sorted list to hashmap  
        List<String> temp = new ArrayList<String>();
        for (Map.Entry<String, Double> aa : list) { 
            temp.add(aa.getKey());
        } 
        return temp; 
    }

	public static void main(final String[] args) throws Exception {
        if (args.length > 0) {
            ownInstanceIp = args[0];

            System.out.println("[LoadBalancer] " + "Own instance ip = " + ownInstanceIp);

            initEc2Client();    
            deployLoadBalancer();
        } else {
            System.out.println("[LoadBalancer] " + "AutoScaler requires the public ip4 of the instance on which it is running as argument");
        }
    }

    public static void initEc2Client() throws Exception {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        ec2Client = AmazonEC2ClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("us-east-1")
            .build();

        cloudWatch = AmazonCloudWatchClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("us-east-1")
            .build();
    }

    public static void deployLoadBalancer() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

            server.createContext("/sudoku", new RequestHandler());

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            System.out.println("[LoadBalancer] " + server.getAddress().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
        Check DB with request to see how heavy it is approximately.
        Then pass the request on to a solver instance e.g.:
            - Solver running a light request can handle another light request
            - Heavy request would require another instance
    */
    static class RequestHandler implements HttpHandler {
		@Override
		public void handle(final HttpExchange t) throws IOException {
            // Get the query.
			final String query = t.getRequestURI().getQuery();
			System.out.println("[LoadBalancer] " + "Incoming query:\t" + query);

			// Break it down into String[].
			final String[] params = query.split("&");

			// Store args by name
			final HashMap<String, String> newArgs = new HashMap<String, String>();
			for (final String p : params) {
				final String[] splitParam = p.split("=");
				newArgs.put(splitParam[0], splitParam[1]);
            }

            // Get metric from Dynamo DB using args
            float metric = AmazonDynamoDBSample.getMetric(
                "cnv_sudoku",
                newArgs.get("s"),
                newArgs.get("un"),
                newArgs.get("n1"),
                newArgs.get("n2"),
                newArgs.get("i")
            );
            MetricLevel metricLevel = getMetricLevel(metric);
            System.out.println("[LoadBalancer] " + "Metric value of this request is: " + metric + ", which is metric level: " + metricLevel.toString());

            // Get designated instance based on the metric
            Instance designatedInstance = getDesignatedInstance(metricLevel, ownInstanceIp);

            // If a running instance was found, send request to instance
            // and await the response.
            String response = "";
            if (designatedInstance != null) {
                System.out.println("[LoadBalancer] " + "Redirecting request to: " + designatedInstance.getPublicIpAddress());
                final String body = ServerHelper.parseRequestBody(t.getRequestBody());
                response = createAndExecuteRequest(t, designatedInstance, t.getRequestURI().toString(), body);
                System.out.println("[LoadBalancer] " + designatedInstance.getPublicIpAddress() + " response: " +  response);
            } else {
                System.out.println("[LoadBalancer] " + "No running instance was found");
            }

			// Send response to browser.
			final Headers hdrs = t.getResponseHeaders();
            hdrs.add("Content-Type", "application/json");
			hdrs.add("Access-Control-Allow-Origin", "*");
            hdrs.add("Access-Control-Allow-Credentials", "true");
			hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
			hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            t.sendResponseHeaders(200, response.length());

            final OutputStream os = t.getResponseBody();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(response);
            osw.flush();
            osw.close();
			os.close();

			System.out.println("[LoadBalancer] " + "Sent response to " + t.getRemoteAddress().toString());
		}
    }

    /**
     * Find the most suitable instance, given the metric of the request.
     * Exclude the instance with LoadBalancer and AutoScaler running from consideration.
     * 
     * Request metric:
     * above 0.05 = heavy
     * else: not heavy
     */
    private static Instance getDesignatedInstance(MetricLevel metricLevel, String ownInstanceIp) {
        Instance designatedInstance = null;
        try {
            Set<Instance> instances = ServerHelper.getInstances(ec2Client, ownInstanceIp);
            HashMap<String, Double> averageCpuUsagePerInstance = ServerHelper.getAverageCpuUsagePerInstance(cloudWatch, instances);
            System.out.println("[LoadBalancer] " + averageCpuUsagePerInstance.toString());

            // List of sorted instance ids, from lowest to highest cpu usage
            List<String> sortedInstancesByUsage = sortByValue(averageCpuUsagePerInstance);

            int highestUsageRank = sortedInstancesByUsage.size() - 1;
            int randomInstance = rand.nextInt(highestUsageRank + 1);

            for (Instance instance : instances) {
                String curInstanceId = instance.getInstanceId();
                // Code 16 = instance is running
                if (instance.getState().getCode() == 16) {
                    System.out.println("[LoadBalancer] Id: " + curInstanceId + ", Usage: " + averageCpuUsagePerInstance.get(curInstanceId));
                    
                    int usageRankInstance = sortedInstancesByUsage.indexOf(curInstanceId);

                    if (metricLevel == MetricLevel.HIGH && usageRankInstance == 0) designatedInstance = instance;
                    else if (metricLevel == MetricLevel.LOW && usageRankInstance == highestUsageRank) designatedInstance = instance;
                    else if (metricLevel == MetricLevel.MEDIUM && usageRankInstance == randomInstance) designatedInstance = instance; 

                    if (designatedInstance != null) break;
                }
            }
            // no instance chosen, just pick one, should not happen
            if (designatedInstance == null) designatedInstance = instances.iterator().next();
            System.out.println("[LoadBalancer] " + "Designated instance: " + designatedInstance.getPublicIpAddress().toString() + ", id: " + designatedInstance.getInstanceId());
        } catch (AmazonServiceException ase) {
            System.out.println("[LoadBalancer] " + "Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("[LoadBalancer] " + "Error Message:    " + ase.getMessage());
            System.out.println("[LoadBalancer] " + "HTTP Status Code: " + ase.getStatusCode());
            System.out.println("[LoadBalancer] " + "AWS Error Code:   " + ase.getErrorCode());
            System.out.println("[LoadBalancer] " + "Error Type:       " + ase.getErrorType());
            System.out.println("[LoadBalancer] " + "Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("[LoadBalancer] " + "Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("[LoadBalancer] " + "Error Message: " + ace.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return designatedInstance;
    }

    /**
     * Use the HttpExchange object to create a new request which is then delegated to the passed instance.
     * Returns the response of that instance.
     * 
     * SOURCE: https://stackoverflow.com/a/1359700
     */
    private static String createAndExecuteRequest (HttpExchange he, Instance instance, String requestURI, String body) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL("http://" + instance.getPublicDnsName() + ":8000" + requestURI);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(he.getRequestMethod());

            final Headers requestHdrs = he.getRequestHeaders();
            Set<Map.Entry<String,List<String>>> s = requestHdrs.entrySet();
            for (Map.Entry<String, List<String>> e : s) {
                for (String val : e.getValue()) {
                    connection.setRequestProperty(e.getKey(), val);
                }
            }

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (
                connection.getOutputStream());
            wr.writeBytes(body);
            wr.close();
        
            //Get Response  
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            System.out.println("[LoadBalancer] " + "Connection failed");
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
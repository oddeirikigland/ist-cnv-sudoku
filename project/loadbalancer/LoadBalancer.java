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

            System.out.println(server.getAddress().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public static void main(final String[] args) throws Exception {
        initEc2Client();    
        deployLoadBalancer();
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
			System.out.println("> Query:\t" + query);

			// Break it down into String[].
			final String[] params = query.split("&");

			// Store as if it was a direct call to SolverMain.
			final ArrayList<String> newArgs = new ArrayList<>();
			for (final String p : params) {
				final String[] splitParam = p.split("=");
				newArgs.add("-" + splitParam[0]);
				newArgs.add(splitParam[1]);
            }
            
            final String body = WebServer.parseRequestBody(t.getRequestBody());
            newArgs.add("-b");
			newArgs.add(body);
			newArgs.add("-d");

			// Store from ArrayList into regular String[].
			final String[] args = new String[newArgs.size()];
			int i = 0;
			for(String arg: newArgs) {
				args[i] = arg;
				i++;
            }

			// Calling instrumentation tool to check the params
            // Get metric from Dynamo DB
            int metric = AmazonDynamoDBSample.getMetric();
            System.out.println("Metric value of this request is: " + metric);

            // Get designated instance based on the metric
            Instance designatedInstance = getDesignatedInstance(metric, t.getLocalAddress().toString());

            // If a running instance was found, send request to instance
            // and await the response.
            String response = "";
            if (designatedInstance != null) {
                System.out.println("Redirecting request to: " + designatedInstance.getPublicIpAddress());
                response = createAndExecuteRequest(t, designatedInstance, t.getRequestURI().toString(), body);
                System.out.println(designatedInstance.getPublicIpAddress() + " response: " +  response);
            } else {
                System.out.println("No running instance was found");
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

			System.out.println("Sent response to " + t.getRemoteAddress().toString());
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
    private static Instance getDesignatedInstance(int metric, String dedicatedInstanceId) {
        Instance designatedInstance = null;
        try {
            Set<Instance> instances = ServerHelper.getInstances(ec2Client);
            HashMap<String, Double> averageCpuUsagePerInstance = ServerHelper.getAverageCpuUsagePerInstance(cloudWatch, instances);

            System.out.println(averageCpuUsagePerInstance.toString());

            Double highestUsage = 100.0;
            for (Instance instance : instances) {
                String curInstanceId = instance.getInstanceId();
                // Code 16 = instance is running
                if (instance.getState().getCode() == 16) {
                    System.out.println(curInstanceId);

                    System.out.println("Instance usage: " + averageCpuUsagePerInstance.get(curInstanceId));
                    if (averageCpuUsagePerInstance.get(curInstanceId) < highestUsage) {
                        designatedInstance = instance;
                    }
                }
            }

            System.out.println("Designated instance: " + designatedInstance.getPublicIpAddress().toString());
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
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
            // TODO: Not sure on the port we decided for the solver instances, but I thought it was 80.
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
            System.out.println("Connection failed");
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
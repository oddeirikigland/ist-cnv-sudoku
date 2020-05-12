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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.MonitorInstancesRequest;
import com.amazonaws.services.ec2.model.UnmonitorInstancesRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import pt.ulisboa.tecnico.cnv.server.WebServer;

public class LoadBalancer {
    static AmazonEC2 ec2Client;

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
    }

    private static void createDedicatedInstance() {
        try {
            RunInstancesRequest runInstancesRequest =
                new RunInstancesRequest();

            /* TODO: configure to use your AMI, key and security group */
            runInstancesRequest.withImageId("ami-0889dae1607d06a32")
                                .withInstanceType("t2.micro")
                                .withMinCount(1)
                                .withMaxCount(1)
                                .withKeyName("CNV-AWS-lab")
                                .withSecurityGroups("CNV-Project+http");
            
            RunInstancesResult runInstancesResult =
                ec2Client.runInstances(runInstancesRequest);
            
            String newInstanceId = runInstancesResult.getReservation().getInstances()
                                        .get(0).getInstanceId();
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
        
    }

    public static void deployLoadBalancer() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

            server.createContext("/redirectrequest", new RequestHandler());

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            System.out.println(server.getAddress().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public static void main(final String[] args) throws Exception {
        initEc2Client();
        createDedicatedInstance();
        // deployLoadBalancer();
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

			// Calling instrumentation tool to check the params
			// TODO: use result from checkparams (aka: metrics) to decide where to run the sudoku solver
			Integer metricNumber = 1;//InstrumentationTool.checkParams(params);
			System.out.println("Metric value of this request is: " + metricNumber);

			// Store as if it was a direct call to SolverMain.
			final ArrayList<String> newArgs = new ArrayList<>();
			for (final String p : params) {
				final String[] splitParam = p.split("=");
				newArgs.add("-" + splitParam[0]);
				newArgs.add(splitParam[1]);
			}
			newArgs.add("-b");
			newArgs.add(WebServer.parseRequestBody(t.getRequestBody()));

			newArgs.add("-d");

			// Store from ArrayList into regular String[].
			final String[] args = new String[newArgs.size()];
			int i = 0;
			for(String arg: newArgs) {
				args[i] = arg;
				i++;
			}

			// // Send response to browser.
			// final Headers hdrs = t.getResponseHeaders();
            // //t.sendResponseHeaders(200, responseFile.length());

			// ///hdrs.add("Content-Type", "image/png");
            // hdrs.add("Content-Type", "application/json");

			// hdrs.add("Access-Control-Allow-Origin", "*");

            // hdrs.add("Access-Control-Allow-Credentials", "true");
			// hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
			// hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            // t.sendResponseHeaders(200, "OK");

            // final OutputStream os = t.getResponseBody();
            // OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            // osw.write(solution.toString());
            // osw.flush();
            // osw.close();

			// os.close();

			// System.out.println("> Sent response to " + t.getRemoteAddress().toString());
		}
	}

}
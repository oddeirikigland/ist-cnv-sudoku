package autoscaler;
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
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.*;

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

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;

import util.ServerHelper;

public class AutoScaler {
    static AmazonEC2 ec2Client;
    static AmazonCloudWatch cloudWatch;

    static String ownInstanceIp;

    static Double minCpuUsage;
    static Double maxCpuUsage;
    static Integer minInstances;
    static Integer maxInstances;

    public static void main(final String[] args) throws Exception {
        if (args.length > 0) {
            ownInstanceIp = args[0];

            System.out.println("[AutoScaler] " + "Own instance ip = " + ownInstanceIp);

            // TODO: Parametrize
            // max CPU, min CPU, min Instances, max Instances
            init(40.0, 60.0, 1, 3);

            ScheduledExecutorService autoScalerService = new ScheduledThreadPoolExecutor(1);

            // Delay of a minute
            autoScalerService.scheduleWithFixedDelay(new InstanceChecker(), 0, 60, SECONDS);
        } else {
            System.out.println("[AutoScaler] " + "AutoScaler requires the public ip4 of the instance on which it is running as argument");
        }
    }

    /**
     * Runnable task that checks instances and creates / terminates instances based
     * on the results.
     */ 
    private static class InstanceChecker implements Runnable { 
        public void run() 
        { 
            System.out.println("[AutoScaler] " + "Checking instance CPU usage...");
            checkInstanceCapacities();
        } 
    } 

    public static void init(Double minCpu, Double maxCpu, Integer minInst, Integer maxInst) throws Exception {
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

        minCpuUsage = minCpu;
        maxCpuUsage = maxCpu;
        minInstances = minInst;
        maxInstances = maxInst;
    }

    /**
     * Check the CPU usage of running instances and create new or terminate instances accordingly.
     * Also takes into account the min- and maximum amount of instances allowed.
     */
    public static void checkInstanceCapacities() {
        try {
            Set<Instance> instances = ServerHelper.getInstances(ec2Client, ownInstanceIp);
            HashMap<String, Double> averageCpuUsagePerInstance = ServerHelper.getAverageCpuUsagePerInstance(cloudWatch, instances);
            double totalAverageCpuUsage = calculateAverage(averageCpuUsagePerInstance);
            
            System.out.println("[AutoScaler] " + "Total average CPU usage = " + totalAverageCpuUsage);
            
            if (totalAverageCpuUsage > maxCpuUsage && instances.size() < maxInstances) {
                System.out.println("[AutoScaler] " + "CPU usage is higher than threshold of " + maxCpuUsage);
                System.out.println("[AutoScaler] " + "Creating new instance...");
                createInstance();
            }
            else if (totalAverageCpuUsage < minCpuUsage && instances.size() > minInstances) {
                System.out.println("[AutoScaler] " + "CPU usage is lower than threshold of " + minCpuUsage);
                System.out.println("[AutoScaler] " + "Terminating one of the instances...");
                // TODO: Decide which one?
                terminateInstance(((Instance)instances.toArray()[0]).getInstanceId());
            }
            else if (instances.size() < minInstances) {
                System.out.println("[AutoScaler] " + "Amount of instances is below min of " + minInstances);
                System.out.println("[AutoScaler] " + "Creating new instance...");
                createInstance();
            }
            else if (instances.size() > maxInstances) {
                System.out.println("[AutoScaler] " + "Amount of instances is above max of " + maxInstances);
                System.out.println("[AutoScaler] " + "Terminating one of the instances...");
                // TODO: Decide which one?
                terminateInstance(((Instance)instances.toArray()[0]).getInstanceId());
            }
            else {
                if (totalAverageCpuUsage >= minCpuUsage && totalAverageCpuUsage <= maxCpuUsage) {
                    System.out.println("[AutoScaler] " + "CPU Usage is currently within bounds of " + minCpuUsage + " < usage < " + maxCpuUsage);
                }
                else if (instances.size() == maxInstances) {
                    System.out.println("[AutoScaler] " + "Amount of instances is currently at max of " + maxInstances);
                }
                else if (instances.size() == minInstances) {
                    System.out.println("[AutoScaler] " + "Amount of instances is currently at min of " + minInstances);
                }
                System.out.println("[AutoScaler] " + "No action required");
            }
        } catch (AmazonServiceException ase) {
            System.out.println("[AutoScaler] " + "Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("[AutoScaler] " + "Error Message:    " + ase.getMessage());
            System.out.println("[AutoScaler] " + "HTTP Status Code: " + ase.getStatusCode());
            System.out.println("[AutoScaler] " + "AWS Error Code:   " + ase.getErrorCode());
            System.out.println("[AutoScaler] " + "Error Type:       " + ase.getErrorType());
            System.out.println("[AutoScaler] " + "Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("[AutoScaler] " + "Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("[AutoScaler] " + "Error Message: " + ace.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[AutoScaler] " + "-----------------------------------------");
    }

    private static void createInstance() {
        try {
            RunInstancesRequest runInstancesRequest =
                new RunInstancesRequest();

            /* TODO: configure to use your AMI, key and security group */
            runInstancesRequest.withImageId("ami-01d6c61c700a0bb63")
                                .withInstanceType("t2.micro")
                                .withMinCount(1)
                                .withMaxCount(1)
                                .withKeyName("CNV-AWS-lab")
                                .withSecurityGroups("CNV-Project+http");
            
            RunInstancesResult runInstancesResult =
                ec2Client.runInstances(runInstancesRequest);

            String newInstanceId = runInstancesResult.getReservation().getInstances()
                .get(0).getInstanceId();

            System.out.println("[AutoScaler] " + "New instance with id: " + newInstanceId + " was created");
        } catch (AmazonServiceException ase) {
            System.out.println("[AutoScaler] " + "Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("[AutoScaler] " + "Error Message:    " + ase.getMessage());
            System.out.println("[AutoScaler] " + "HTTP Status Code: " + ase.getStatusCode());
            System.out.println("[AutoScaler] " + "AWS Error Code:   " + ase.getErrorCode());
            System.out.println("[AutoScaler] " + "Error Type:       " + ase.getErrorType());
            System.out.println("[AutoScaler] " + "Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("[AutoScaler] " + "Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("[AutoScaler] " + "Error Message: " + ace.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void terminateInstance(String instanceId) {
        try {
            TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
            termInstanceReq.withInstanceIds(instanceId);
            ec2Client.terminateInstances(termInstanceReq);
            System.out.println("[AutoScaler] " + "Instance with id: " + instanceId + " was removed");
        } catch (AmazonServiceException ase) {
            System.out.println("[AutoScaler] " + "Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("[AutoScaler] " + "Error Message:    " + ase.getMessage());
            System.out.println("[AutoScaler] " + "HTTP Status Code: " + ase.getStatusCode());
            System.out.println("[AutoScaler] " + "AWS Error Code:   " + ase.getErrorCode());
            System.out.println("[AutoScaler] " + "Error Type:       " + ase.getErrorType());
            System.out.println("[AutoScaler] " + "Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("[AutoScaler] " + "Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("[AutoScaler] " + "Error Message: " + ace.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // HELPER
    private static double calculateAverage(HashMap<String, Double> vals) {
        Double sum = 0.0;
        if(vals.size() > 0) {
            for (Double val : vals.values()) {
                sum += val;
            }
            return sum.doubleValue() / vals.size();
        }
        return sum;
    }
}
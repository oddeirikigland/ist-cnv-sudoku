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
import java.util.Set;
import java.util.List;
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
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import pt.ulisboa.tecnico.cnv.server.WebServer;

public class AutoScaler {
    static AmazonEC2 ec2Client;
    static AmazonCloudWatch cloudWatch;
    static Double minCpuUsage;
    static Double maxCpuUsage;
    static Integer minInstances;
    static Integer maxInstances;

    public static void main(final String[] args) throws Exception {
        // TODO: Parametrize
        // max CPU, min CPU, min Instances, max Instances
        init(40.0, 60.0, 1, 3);

        ScheduledExecutorService autoScalerService = new ScheduledThreadPoolExecutor(1);

        // Delay of a minute
        autoScalerService.scheduleWithFixedDelay(new InstanceChecker(), 0, 60, SECONDS);
    }

    private static class InstanceChecker implements Runnable { 
  
        public void run() 
        { 
            System.out.println("Checking instance CPU usage...");
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
     */
    public static void checkInstanceCapacities() {
        try {
            DescribeInstancesResult describeInstancesRequest = ec2Client.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();

            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }

            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");

            long offsetInMilliseconds = 1000 * 60 * 10;
            Dimension instanceDimension = new Dimension();
            instanceDimension.setName("InstanceId");
            List<Dimension> dims = new ArrayList<Dimension>();
            dims.add(instanceDimension);
            
            ArrayList<Double> averageCpuUsagePerInstance = new ArrayList<Double>();
            for (Instance instance : instances) {
                String name = instance.getInstanceId();
                String state = instance.getState().getName();
            
                if (state.equals("running")) { 
                    System.out.println("running instance id = " + name);
                    instanceDimension.setValue(name);
                    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                        .withStartTime(new Date(new Date().getTime() - offsetInMilliseconds))
                        .withNamespace("AWS/EC2")
                        .withPeriod(60)
                        .withMetricName("CPUUtilization")
                        .withStatistics("Average")
                        .withDimensions(instanceDimension)
                        .withEndTime(new Date());
                    GetMetricStatisticsResult getMetricStatisticsResult = 
                        cloudWatch.getMetricStatistics(request);
                    List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
                
                    for (Datapoint dp : datapoints) {
                        System.out.println(" CPU utilization for instance " + name +
                        " = " + dp.getAverage());
                        averageCpuUsagePerInstance.add(dp.getAverage());
                    }

                }
                else {
                    System.out.println("instance id = " + name);
                }
                System.out.println("Instance State : " + state +".");
            }

            double totalAverageCpuUsage = calculateAverage(averageCpuUsagePerInstance);
            System.out.println("Total average CPU usage = " + totalAverageCpuUsage);
            if (totalAverageCpuUsage > maxCpuUsage && instances.size() < maxInstances) {
                System.out.println("CPU usage is higher than threshold of " + maxCpuUsage);
                System.out.println("Creating new instance...");
                createInstance();
            }
            else if (totalAverageCpuUsage < minCpuUsage && instances.size() > minInstances) {
                System.out.println("CPU usage is lower than threshold of " + minCpuUsage);
                System.out.println("Terminating one of the instances...");
                // TODO: Decide which one?
                terminateInstance(((Instance)instances.toArray()[0]).getInstanceId());
            }
        }
        catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a new instance.
     * 
     * returns: a new instance id
     */
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
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void terminateInstance(String instanceId) {
        try {
            TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
            termInstanceReq.withInstanceIds(instanceId);
            ec2Client.terminateInstances(termInstanceReq);
        }catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // HELPER
    private static double calculateAverage(List<Double> vals) {
        Double sum = 0.0;
        if(!vals.isEmpty()) {
          for (Double val : vals) {
              sum += val;
          }
          return sum.doubleValue() / vals.size();
        }
        return sum;
      }
}
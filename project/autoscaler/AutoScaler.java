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

public class AutoScaler {
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

    public static void checkInstanceCapacities() {
        DescribeInstancesResult describeInstancesRequest = ec2Client.describeInstances();
        List<Reservation> reservations = describeInstancesRequest.getReservations();
        Set<Instance> instances = new HashSet<Instance>();

        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances());
        }

        System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");

        // TODO: Logic for determining if workload requires a new instance
        // if workload > available capacity:
        //      createInstance()
        //      send Request (or loadbalancer does this?)
        // else if available capacity = much larger than workload
        //      terminateInstance()
        // else
        //      wait for next check (5 sec?)
    }

    /**
     * Create a new instance.
     * 
     * returns: a new instance id
     */
    private static String createInstance() {
        String newInstanceId = "";
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
            
            newInstanceId = runInstancesResult.getReservation().getInstances()
                        .get(0).getInstanceId();
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newInstanceId;
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

    public static void main(final String[] args) throws Exception {
        // TODO: Should use same EC2 client as load balancer I think       
        initEc2Client();
    }
}
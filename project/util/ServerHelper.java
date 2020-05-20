package util;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

import java.io.*;


import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

/**
 * Server methods, to be used by both the LoadBalancer and AutoScaler.
 */
public class ServerHelper {
    /**
     * Get running instances from Amazon EC2 client
     */
    public static Set<Instance> getInstances(AmazonEC2 ec2Client, String ownInstanceIp) {
        Set<Instance> instances = new HashSet<Instance>();
        try {
            DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances();
            List<Reservation> reservations = describeInstancesResult.getReservations();
            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }

            // Remove instance that process is running on and
            // instances that aren't runnning from consideration
            Iterator<Instance> iterator = instances.iterator();
            while(iterator.hasNext()) {
                Instance instance = iterator.next();
                if(instance.getState().getCode() != 16 ||
                   instance.getPublicIpAddress() == ownInstanceIp) {
                    iterator.remove();
                }
            }
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
        return instances;
    }

    public static HashMap<String, Double> getAverageCpuUsagePerInstance (AmazonCloudWatch cloudWatch, Set<Instance> instances) {
        HashMap<String, Double> averageCpuUsagePerInstance = new HashMap<String, Double>();

        try {
            long offsetInMilliseconds = 1000 * 60 * 10;
            Dimension instanceDimension = new Dimension();
            instanceDimension.setName("InstanceId");
            List<Dimension> dims = new ArrayList<Dimension>();
            dims.add(instanceDimension);
    
            for (Instance instance : instances) {
                String name = instance.getInstanceId();
                String state = instance.getState().getName();
            
                if (state.equals("running")) { 
                    System.out.println("running instance id = " + name);
                    instanceDimension.setValue(name);
                    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                        .withStartTime(new Date(new Date().getTime() - offsetInMilliseconds))
                        .withNamespace("AWS/EC2")
                        .withPeriod(60) // 60 30 10 5 1
                        .withMetricName("CPUUtilization")
                        .withStatistics("Average")
                        .withDimensions(instanceDimension)
                        .withEndTime(new Date());
                    GetMetricStatisticsResult getMetricStatisticsResult = 
                        cloudWatch.getMetricStatistics(request);
                    List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
                
                    System.out.println(datapoints.size());

                    Double avg = 0.0;
                    for (Datapoint dp : datapoints) {
                        avg = dp.getAverage();
                    }

                    System.out.println("CPU utilization for instance " + name +" = " + avg);

                    averageCpuUsagePerInstance.put(name, avg);
                }
                else {
                    System.out.println("instance: " + name + " is not running");
                }
            }
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

        return averageCpuUsagePerInstance;
    }

    public static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr =  new InputStreamReader(is,"utf-8");
        BufferedReader br = new BufferedReader(isr);

        // From now on, the right way of moving from bytes to utf-8 characters:
        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);

        }

        br.close();
        isr.close();

        return buf.toString();
	}
}
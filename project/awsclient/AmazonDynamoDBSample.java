package awsclient;
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
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import BIT.InstrumentationThreadStatistics;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class AmazonDynamoDBSample {

    /*
     * Before running the code:
     *      Fill in your AWS access credentials in the provided credentials
     *      file template, and be sure to move the file to the default location
     *      (~/.aws/credentials) where the sample code will load the
     *      credentials from.
     *      https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING:
     *      To avoid accidental leakage of your credentials, DO NOT keep
     *      the credentials file in your source directory.
     */

    static AmazonDynamoDB dynamoDB;

    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.ProfilesConfigFile
     * @see com.amazonaws.ClientConfiguration
     */
    private static synchronized void init() throws Exception {
        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
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
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("us-east-1")
            .build();
    }

    private static synchronized void createTableIfMissing(AmazonDynamoDB dynamoDB, String tableName) throws Exception {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
            .withKeySchema(new KeySchemaElement().withAttributeName("parameters").withKeyType(KeyType.HASH))
            .withAttributeDefinitions(new AttributeDefinition().withAttributeName("parameters").withAttributeType(ScalarAttributeType.S))
            .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
        // wait for the table to move into ACTIVE state
        TableUtils.waitUntilActive(dynamoDB, tableName);

        // Describe our new table
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
        System.out.println("Table Description: " + tableDescription);
    }

    public static synchronized void updateSudokuDynamoDB(String tableName, InstrumentationThreadStatistics instrumentedThreadStats) {
        try {
            init();

            createTableIfMissing(dynamoDB, tableName);

            // Add stats from sudoku compute
            Map<String, AttributeValue> item = newItem(instrumentedThreadStats);
            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
            System.out.println("Result: " + putItemResult);

            // // Scan items for movies with a year attribute greater than 1985
            // HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
            // Condition condition = new Condition()
            //     .withComparisonOperator(ComparisonOperator.GT.toString())
            //     .withAttributeValueList(new AttributeValue().withN("1985"));
            // scanFilter.put("year", condition);
            // ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
            // ScanResult scanResult = dynamoDB.scan(scanRequest);
            // System.out.println("Result: " + scanResult);

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
    }

    public static int getMetric() {
        return 1;
    }

    public static void main(String[] args) throws Exception {
        String tableName = "test_table";
        String[] parameters = {"s=1", "un=1", "n1=1", "n2=1", "i=1"};
        InstrumentationThreadStatistics stats = new InstrumentationThreadStatistics(123, parameters);
        updateSudokuDynamoDB(tableName, stats);
    }

    private static synchronized Map<String, AttributeValue> newItem(InstrumentationThreadStatistics stats) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("parameters", new AttributeValue(stats.logParams())); // This is the unique key
        item.put("s", new AttributeValue(stats.getS()));
        item.put("un", new AttributeValue().withN((stats.getUn())));
        item.put("n1", new AttributeValue().withN((stats.getN1())));
        item.put("n2", new AttributeValue().withN((stats.getN2())));
        item.put("i", new AttributeValue(stats.getI()));
        item.put("lastRequestForParams", new AttributeValue(stats.getRequestDate()));
        item.put("metric_value", new AttributeValue().withN((stats.getMetric())));
        item.put("i_count", new AttributeValue().withN((stats.get_i_count())));
        item.put("b_count", new AttributeValue().withN((stats.get_b_count())));
        item.put("m_count", new AttributeValue().withN((stats.get_m_count())));
        item.put("dyn_bb_count", new AttributeValue().withN((stats.get_dyn_bb_count())));
        item.put("dyn_instr_count", new AttributeValue().withN(stats.get_dyn_instr_count()));
        item.put("dyn_method_count", new AttributeValue().withN((stats.get_dyn_method_count())));
        item.put("newcount", new AttributeValue().withN((stats.get_newcount())));
        item.put("newarraycount", new AttributeValue().withN((stats.get_newarraycount())));
        item.put("anewarraycount", new AttributeValue().withN((stats.get_anewarraycount())));
        item.put("multianewarraycount", new AttributeValue().withN((stats.get_multianewarraycount())));
        item.put("loadcount", new AttributeValue().withN((stats.get_loadcount())));
        item.put("storecount", new AttributeValue().withN((stats.get_storecount())));
        item.put("fieldloadcount", new AttributeValue().withN((stats.get_fieldloadcount())));
        item.put("fieldstorecount", new AttributeValue().withN((stats.get_fieldstorecount())));

        return item;
    }

}
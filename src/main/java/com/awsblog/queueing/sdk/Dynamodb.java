package com.awsblog.queueing.sdk;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.awsblog.queueing.Constants;
import com.awsblog.queueing.appdata.Assignment;
import com.awsblog.queueing.config.Configuration;
import com.awsblog.queueing.model.*;

import com.awsblog.queueing.utils.Utils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.amazonaws.retry.PredefinedRetryPolicies.DYNAMODB_DEFAULT_BACKOFF_STRATEGY;

public class Dynamodb implements IDbPriorityQueue<Dynamodb> {

    private AWSCredentials credentials;
    String logicalTableName;
    String awsRegion;
    String awsCredentialsProfileName;
    DynamoDBMapper dbMapper = null;
    AmazonDynamoDB dynamoDB = null;


    private static final RetryPolicy RETRY_POLICY = new RetryPolicy(null, DYNAMODB_DEFAULT_BACKOFF_STRATEGY, 10, false);


    public Dynamodb(Builder builder) {
        this.logicalTableName = builder.logicalTableName;

        this.awsRegion = builder.awsRegion;
        this.credentials = builder.credentials;
        this.awsCredentialsProfileName = builder.awsCredentialsProfileName;
    }

    @Override
    public Dynamodb initialize() {

        Locale.setDefault(Locale.ENGLISH);

        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");

        // If the aws credentials aren't given via cli then checks Environment variables
        if (Utils.checkIfNotNullAndNotEmptyString(accessKey) && Utils.checkIfNotNullAndNotEmptyString(secretKey)) {

            if (Utils.checkIfNullOrEmptyString(accessKey))
                accessKey = System.getenv("AWS_ACCESS_KEY_ID");

            if (Utils.checkIfNullOrEmptyString(secretKey))
                secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");

            this.credentials = new BasicAWSCredentials(accessKey, secretKey);
        } else if (Utils.checkIfNotNullAndNotEmptyString(this.awsCredentialsProfileName)) {

            this.credentials = new ProfileCredentialsProvider(this.awsCredentialsProfileName).getCredentials();
        }

        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        if (!Utils.checkIfNullObject(this.credentials))
            builder.withCredentials(new AWSStaticCredentialsProvider(this.credentials));
        if (!Utils.checkIfNullObject(this.awsRegion)) builder.withRegion(this.awsRegion);

        this.dynamoDB = builder
                .withClientConfiguration(new ClientConfiguration().withMaxConnections(100).withConnectionTimeout(30000).withRetryPolicy(RETRY_POLICY)).build();

        // get the configuration information
        Configuration config = Configuration.loadConfiguration();

        // searches this map for tableName
//		this.actualTableName = this.config.getTablesMap().get(this.logicalTableName).getTableName();
//		Utils.throwIfNullOrEmptyString(this.actualTableName, "Actual DynamoDB table name is not found!");

        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                //.withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(logicalTableName))
                .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING)
                .build();

        this.dbMapper = new DynamoDBMapper(dynamoDB, mapperConfig);

        return this;
    }

    @Override
    public Object get(String id) {
        return this.dbMapper.load(Assignment.class, id.trim());
    }

    @Override
    public void put(Object item) {
        if (item.getClass().getSimpleName().equals("Assignment")) {
            this.putImpl((Assignment) item);
        } else {
            System.out.println("Failed to insert Item into Dynamodb Object is not of type Assignment");
        }
    }

    public void delete(String id) {

        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be NULL!");

        this.dbMapper.delete(new Assignment(id));
    }

    @Override
    public ReturnResult remove(String id) {
        ReturnResult result = new ReturnResult(id);

        Assignment assignment = (Assignment) this.get(id);
        if (Utils.checkIfNullObject(assignment)) {

            result.setReturnValue(ReturnStatusEnum.FAILED_ID_NOT_FOUND);
            return result;
        }

        OffsetDateTime odt = OffsetDateTime.now(ZoneOffset.UTC);

        DynamoDB ddb = new DynamoDB(this.dynamoDB);
        Table table = ddb.getTable(this.logicalTableName);

        UpdateItemOutcome outcome = null;

        try {

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withPrimaryKey("id", id)
                    .withUpdateExpression("ADD #sys.#v :one "
                            + "REMOVE #sys.peek_utc_timestamp, queued, #DLQ "
                            + "SET #sys.queued = :zero ,"//#sys.queue_selected = :false, "
                            + "#sys.last_updated_timestamp = :lut, "
                            + "last_updated_timestamp = :lut, "
                            + "#sys.queue_remove_timestamp = :lut")
                    .withNameMap(new NameMap().with("#v", "version")
                            .with("#DLQ", "DLQ")
                            .with("#sys", "system_info"))
                    .withValueMap(
                            new ValueMap()
                                    .withInt(":one", 1)
                                    .withInt(":zero", 0)
                                    //.withBoolean(":false", false)
                                    .withInt(":v", assignment.getSystemInfo().getVersion())
                                    .withString(":lut", odt.toString()))
                    .withConditionExpression("#sys.#v = :v")
                    .withReturnValues(ReturnValue.ALL_NEW);

            outcome = table.updateItem(updateItemSpec);

        } catch (Exception e) {
            System.err.println("remove() - failed to update multiple attributes in " + this.logicalTableName);
            System.err.println(e.getMessage());

            result.setReturnValue(ReturnStatusEnum.FAILED_DYNAMO_ERROR);
            return result;
        }

        // Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
        // result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
        //  result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
        //result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));

        result.setReturnValue(ReturnStatusEnum.SUCCESS);
        return result;
    }

    @Override
    public ReturnResult restore(String id) {

        ReturnResult result = new ReturnResult(id);

        Assignment assignment = (Assignment) this.get(id);
        if (Utils.checkIfNullObject(assignment)) {

            result.setReturnValue(ReturnStatusEnum.FAILED_ID_NOT_FOUND);
            return result;
        }

        OffsetDateTime odt = OffsetDateTime.now(ZoneOffset.UTC);

        DynamoDB ddb = new DynamoDB(this.dynamoDB);
        Table table = ddb.getTable(this.logicalTableName);

        UpdateItemOutcome outcome = null;

        try {

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withPrimaryKey("id", id)
                    .withUpdateExpression("ADD #sys.#v :one "
                        //    + "REMOVE #DLQ "
                            + "SET #sys.queued = :one, queued = :one, "
                            //	+ "#sys.queue_selected = :false, "
                            + "last_updated_timestamp = :lut, "
                            + "#sys.last_updated_timestamp = :lut, "
                            + "#sys.queue_add_timestamp = :lut") //error occured due to extra space at the end
                            //+ "#sys.#st = :st")
                    .withNameMap(new NameMap()
                            .with("#v", "version")
                            //.with("#DLQ", "DLQ")
                            //.with("#st", "status")
                            .with("#sys", "system_info"))
                    .withValueMap(
                            new ValueMap().withInt(":one", 1)
                                    .withInt(":v", assignment.getSystemInfo().getVersion())
                                    //.withBoolean(":false", false)
                                    //.withString(":st", StatusEnum.READY_TO_SHIP.toString())
                                    .withString(":lut", odt.toString()))
                    .withConditionExpression("#sys.#v = :v")
                    .withReturnValues(ReturnValue.ALL_NEW);

            outcome = table.updateItem(updateItemSpec);
        }
        catch (Exception e) {
            System.err.println("restore() - failed to update multiple attributes in " + this.logicalTableName);
            System.err.println(e.getMessage());

            result.setReturnValue(ReturnStatusEnum.FAILED_DYNAMO_ERROR);
            return result;
        }

//        Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
//        result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
//        result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
//        result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));

        result.setReturnValue(ReturnStatusEnum.SUCCESS);
        return result;
    }

    @Override
    public PeekResult peek() {

        Map<String,AttributeValue> exclusiveStartKey = null;

        PeekResult result = new PeekResult();

        Map<String,AttributeValue> values = new HashMap<>();
        values.put(":one", new AttributeValue().withN("1"));

        String selectedID = null;

        int selectedVersion = 0;
        boolean recordForPeekIsFound = false;

        do {

            // this query grabs everything in sparse index, in other words all values with queued = 1
            QueryRequest queryRequest = new QueryRequest()
                    .withProjectionExpression("id, scheduled, system_info")
                    .withIndexName(Constants.QUEUEING_INDEX_NAME)
                    .withTableName(this.logicalTableName)
                    .withKeyConditionExpression("queued = :one") //  manages what will be in the queue
                    // This is unnecessary in our implementation because we don't need to denote a status on our items
                    //.withFilterExpression("attribute_not_exists(queue_selected)")   // we need to look for the stragglers
                    .withLimit(250)
                    .withScanIndexForward(true)
                    .withExpressionAttributeValues(values);

            queryRequest.withExclusiveStartKey(exclusiveStartKey);

            QueryResult queryResult = this.dynamoDB.query(queryRequest);
            exclusiveStartKey = queryResult.getLastEvaluatedKey();

            for(Map<String,AttributeValue> itemMap : queryResult.getItems()) {

                Map<String, AttributeValue> sysMap = itemMap.get("system_info").getM();

                selectedID = itemMap.get("id").getS();
                selectedVersion = Integer.parseInt(sysMap.get("version").getN());
                recordForPeekIsFound = true;

                // no need to go further
                if (recordForPeekIsFound) break;
            }

        } while (!recordForPeekIsFound && exclusiveStartKey != null);

        if (Utils.checkIfNullObject(selectedID)) {

            result.setReturnValue(ReturnStatusEnum.FAILED_EMPTY_QUEUE);
            return result;
        }

        // assign ID to 'result'
        result.setId(selectedID);

        // this is an simplest way to construct an App object
        Assignment assignment = (Assignment) this.get(selectedID);

        OffsetDateTime odt = OffsetDateTime.now(ZoneOffset.UTC);

        DynamoDB ddb = new DynamoDB(this.dynamoDB);
        Table table = ddb.getTable(this.logicalTableName);

        long tsUTC = System.currentTimeMillis();

        UpdateItemOutcome outcome = null;

        // This functionality is used to prevent us from interacting with same variable multiple times; However,
        // we won't necessarily need this functionality, because all we want to do is get top value and see it schedule

        try {

            // IMPORTANT
            // please note, we are not updating top-level attribute `last_updated_timestamp` in order to avoid re-indexing the order
            UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("id", assignment.getId())
                    .withUpdateExpression(
                            "ADD #sys.#v :one "
                                    + "SET queued = :one, #sys.queued = :one,"
                                    + " #sys.last_updated_timestamp = :lut, #sys.queue_peek_timestamp = :lut, "
                                    + "#sys.peek_utc_timestamp = :ts")
                    .withNameMap(new NameMap()
                            .with("#v", "version")
                            //.with("#st", "status")
                            .with("#sys", "system_info"))
                    .withValueMap(
                            new ValueMap()
                                    .withInt(":one", 1)
                                    .withInt(":v", selectedVersion)
                                    .withLong(":ts", tsUTC)
                                    .withString(":lut", odt.toString()))
                    .withConditionExpression("#sys.#v = :v")
                    .withReturnValues(ReturnValue.ALL_NEW);


            outcome = table.updateItem(updateItemSpec);
        }
        catch (Exception e) {
            System.err.println("peek() - failed to update multiple attributes in " + this.logicalTableName);
            System.err.println(e.getMessage());

            result.setReturnValue(ReturnStatusEnum.FAILED_DYNAMO_ERROR);
            return result;
        }

        // result.setId(outcome.getItem().getString("id"));

        // adding this to get the fresh data from DDB
        Assignment peekedAssignment = (Assignment) this.get(selectedID);
        result.setPeekedAssignmentObject(peekedAssignment);

        //Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
        //  result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
        // result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));

        // result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
        //result.setTimestampMillisUTC(((BigDecimal)sysMap.get("peek_utc_timestamp")).intValue());

        result.setReturnValue(ReturnStatusEnum.SUCCESS);
        return result;
    }

    @Override
    public EnqueueResult enqueue(String id) {
        EnqueueResult result = new EnqueueResult(id);

        if (Utils.checkIfNullOrEmptyString(id)) {

            System.out.printf("ID is not provided ... cannot proceed with the enqueue() operation!%n");
            result.setReturnValue(ReturnStatusEnum.FAILED_ID_NOT_PROVIDED);
            return result;
        }

//

        Assignment retrievedAssignment = (Assignment) this.get(id);

        if (Utils.checkIfNullObject(retrievedAssignment)) {

            System.out.printf("Assignment with ID [%s] cannot be found!%n", id);
            result.setReturnValue(ReturnStatusEnum.FAILED_ID_NOT_FOUND);
            return result;
        }

        int version = retrievedAssignment.getSystemInfo().getVersion();

        DynamoDB ddb = new DynamoDB(this.dynamoDB);
        Table table = ddb.getTable(this.logicalTableName);

        result.setVersion(version);
        result.setLastUpdatedTimestamp(retrievedAssignment.getSystemInfo().getLastUpdatedTimestamp());

        OffsetDateTime odt = OffsetDateTime.now(ZoneOffset.UTC);

        try {
            UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("id", id)
                    .withUpdateExpression(
                            "ADD #sys.#v :one "
                                    + "SET queued = :one, #sys.queued = :one,"  //#sys.queue_selected = :false, "
                                    + "last_updated_timestamp = :lut, #sys.last_updated_timestamp = :lut, "
                                    + "#sys.queue_added_timestamp = :lut") //#sys.#st = :st")
                    .withNameMap(new NameMap()
                            .with("#v", "version")
                            //.with("#st", "status")
                            .with("#sys", "system_info"))
                    .withValueMap(
                            new ValueMap()
                                    .withInt(":one", 1)
                                    //.withBoolean(":false", false)
                                    .withInt(":v", version)
                                    .withString(":lut", odt.toString()))
                    .withConditionExpression("#sys.#v = :v")
                    .withReturnValues(ReturnValue.ALL_NEW);

            UpdateItemOutcome outcome = table.updateItem(updateItemSpec);

            Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
            result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
            result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));
            // result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));

            Assignment assignment = (Assignment) this.get(id);
            result.setAssignment(assignment);
        }
        catch (Exception e) {
            System.err.println("enqueue() - failed to update multiple attributes in " + this.logicalTableName);
            System.err.println(e.getMessage());

            result.setReturnValue(ReturnStatusEnum.FAILED_DYNAMO_ERROR);
            return result;
        }

        result.setReturnValue(ReturnStatusEnum.SUCCESS);
        return result;
    }

    @Override
    public DequeueResult dequeue() {
        PeekResult peekResult = this.peek();

        DequeueResult dequeueResult = null;

        if (peekResult.isSuccessful()) {

            String ID = peekResult.getId();
            ReturnResult removeResult = this.remove(ID);

            dequeueResult = DequeueResult.fromReturnResult(removeResult);

            if (removeResult.isSuccessful()) {

                dequeueResult.setDequeuedAssignmentObject(peekResult.getPeekedAssignmentObject());
            }
        }
        else {

            dequeueResult = DequeueResult.fromReturnResult(peekResult);
        }

        return dequeueResult;
    }

    private void putImpl(Assignment assignment) {

        Utils.throwIfNullObject(assignment, "assignment object cannot be NULL!");

        int version = 0;

        // check if already present
        Assignment retrievedAssignment = this.dbMapper.load(Assignment.class, assignment.getId());
        if (!Utils.checkIfNullObject(retrievedAssignment)) {
                this.dbMapper.delete(retrievedAssignment);
            }

        OffsetDateTime odt = OffsetDateTime.now(ZoneOffset.UTC);

        SystemInfo system = new SystemInfo(assignment.getId());
        system.setInQueue(false); // we want all items placed in dynamodb to also be placed into queue
        // system.setSelectedFromQueue(false);
        //system.setStatus(assignment.getSystemInfo().getStatus());
        system.setCreationTimestamp(odt.toString());
        system.setLastUpdatedTimestamp(odt.toString());
        system.setVersion(version + 1);

        assignment.setSystemInfo(system);

        // store it in DynamoDB
        this.dbMapper.save(assignment);

        }

    @Override
    public QueueStats getQueueStats() {
        int totalQueueSize = 0;

        Map<String, AttributeValue> exclusiveStartKey = null;

        Map<String,String> names = new HashMap<>();
        names.put("#q", "queued");
        Map<String,AttributeValue> values = new HashMap<>();
        values.put(":one", new AttributeValue().withN("1"));

        int peekedRecords = 0;

        List<String> allQueueIDs = new ArrayList<>();
        List<String> processingIDs = new ArrayList<>();


        // Gets items: id and system_info; using the scheduled_index
        // that meet the condition of queued == 1 and then organizes them descending order
        do {

            QueryRequest queryRequest = new QueryRequest()
                    .withProjectionExpression("id, system_info")
                    .withIndexName(Constants.QUEUEING_INDEX_NAME)
                    .withTableName(this.logicalTableName)
                    .withExpressionAttributeNames(names)
                    .withKeyConditionExpression("#q = :one")
                    .withScanIndexForward(true)
                    .withLimit(250)
                    .withExpressionAttributeValues(values);

            // exclusive start key is just a structure containing the keys needed to resume the query and grab the next n items
            queryRequest.withExclusiveStartKey(exclusiveStartKey);

            QueryResult queryResult = this.dynamoDB.query(queryRequest);
            exclusiveStartKey = queryResult.getLastEvaluatedKey();

            for(Map<String,AttributeValue> itemMap : queryResult.getItems()) {
                ++totalQueueSize;
            }

        } while (exclusiveStartKey != null);

        QueueStats result = new QueueStats();
        //result.setTotalRecordsInProcessing(peekedRecords);
        result.setTotalRecordsInQueue(totalQueueSize);
        if (Utils.checkIfNotNullAndNotEmptyCollection(allQueueIDs)) result.setFirst100IDsInQueue(allQueueIDs);
        if (Utils.checkIfNotNullAndNotEmptyCollection(processingIDs)) result.setFirst100SelectedIDsInQueue(processingIDs);

        return result;
    }
    public static class Builder {


        protected AWSCredentials credentials = null;

        protected String logicalTableName = null;

        protected String awsRegion = null;
        protected String awsCredentialsProfileName = null;

        private Dynamodb client = null;

        /**
         * Default constructor
         */
        public Builder() {
            // ...
        }

        /**
         * Create a QueueSDK
         *
         * @return QueueSdkClient
         */
        public Dynamodb build() {
            if (Utils.checkIfNullObject(this.client)) {

                this.client = new Dynamodb(this);
                this.client.initialize();
            }

            return this.client;
        }
        /**
         * Specify AWS region
         * If not used, default value is 'us-east-1'.
         *
         * @param region Proper AWS Region string
         * @return Builder
         */
        public Builder withRegion(String region) {

            this.awsRegion = region;
            return this;
        }

        /**
         * Specify local credential profile
         *
         * @param profile This is the name of the local AWS Credential profile
         * @return Builder
         */
        public Builder withCredentialsProfileName(String profile) {

            this.awsCredentialsProfileName = profile;
            return this;
        }

        public Builder withLogicalTableName(String logicalTableName) {

            this.logicalTableName = logicalTableName;
            return this;
        }
    }
}

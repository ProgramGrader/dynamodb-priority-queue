package com.awsblog.queueing.sdk

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.retry.PredefinedRetryPolicies
import com.amazonaws.retry.RetryPolicy
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.ReturnValue
import com.awsblog.queueing.Constants
import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.config.Configuration
import com.awsblog.queueing.model.*
import com.awsblog.queueing.utils.Utils
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


class Dynamodb(builder: Builder) : Database {
    private var credentials: AWSCredentials?
    var logicalTableName: String?
    var awsRegion: String?
    var awsCredentialsProfileName: String?
    var dbMapper: DynamoDBMapper? = null
    var dynamoDB: AmazonDynamoDB? = null

    init {
        logicalTableName = builder.logicalTableName
        awsRegion = builder.awsRegion
        credentials = builder.credentials
        awsCredentialsProfileName = builder.awsCredentialsProfileName
    }

    override fun initialize() :Database {
        Locale.setDefault(Locale.ENGLISH)
        var accessKey = System.getenv("AWS_ACCESS_KEY_ID")
        var secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")

        // If the aws credentials aren't given via cli then checks Environment variables
        if (Utils.checkIfNotNullAndNotEmptyString(accessKey) && Utils.checkIfNotNullAndNotEmptyString(secretKey)) {
            if (Utils.checkIfNullOrEmptyString(accessKey)) accessKey = System.getenv("AWS_ACCESS_KEY_ID")
            if (Utils.checkIfNullOrEmptyString(secretKey)) secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")
            credentials = BasicAWSCredentials(accessKey, secretKey)
        } else if (Utils.checkIfNotNullAndNotEmptyString(awsCredentialsProfileName)) {
            credentials = ProfileCredentialsProvider(awsCredentialsProfileName).credentials
        }
        val builder = AmazonDynamoDBClientBuilder.standard()
        if (!Utils.checkIfNullObject(credentials)) builder.withCredentials(
            AWSStaticCredentialsProvider(
                credentials
            )
        )
        if (!Utils.checkIfNullObject(awsRegion)) builder.withRegion(awsRegion)
        dynamoDB = builder
            .withClientConfiguration(
                ClientConfiguration().withMaxConnections(100).withConnectionTimeout(30000).withRetryPolicy(
                    RETRY_POLICY
                )
            ).build()

        // get the configuration information
        val config: Configuration? = Configuration.Companion.loadConfiguration()

        // searches this map for tableName
//		this.actualTableName = this.config.getTablesMap().get(this.logicalTableName).getTableName();
//		Utils.throwIfNullOrEmptyString(this.actualTableName, "Actual DynamoDB table name is not found!");
        val mapperConfig = DynamoDBMapperConfig.builder()
            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
            .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT) //.withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
            .withTableNameOverride(TableNameOverride(logicalTableName))
            .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING)
            .build()
        dbMapper = DynamoDBMapper(dynamoDB, mapperConfig)
        return this
    }

    override fun get(id: String?): Any {
        return dbMapper!!.load(Assignment::class.java, id!!.trim { it <= ' ' })
    }

    override fun put(item: Any) {
        if (item.javaClass.simpleName == "Assignment") {
            putImpl(item as Assignment)
        } else {
            println("Failed to insert Item into Dynamodb Object is not of type Assignment")
        }
    }

    override fun delete(id: String) {
        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be NULL!")
        dbMapper!!.delete(Assignment(id))
    }

    override fun remove(id: String?): ReturnResult {
        val result = ReturnResult(id)
        val assignment = this[id] as Assignment
        if (Utils.checkIfNullObject(assignment)) {
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(logicalTableName)
        var outcome: UpdateItemOutcome? = null
        try {
            val updateItemSpec = UpdateItemSpec()
                .withPrimaryKey("id", id)
                .withUpdateExpression(
                    "ADD #sys.#v :one "
                            + "REMOVE #sys.peek_utc_timestamp, queued, #DLQ "
                            + "SET #sys.queued = :zero ," //#sys.queue_selected = :false, "
                            + "#sys.last_updated_timestamp = :lut, "
                            + "last_updated_timestamp = :lut, "
                            + "#sys.queue_remove_timestamp = :lut"
                )
                .withNameMap(
                    NameMap().with("#v", "version")
                        .with("#DLQ", "DLQ")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    assignment.systemInfo?.let {
                        ValueMap()
                            .withInt(":one", 1)
                            .withInt(":zero", 0) //.withBoolean(":false", false)
                            .withInt(":v", it.version)
                            .withString(":lut", odt.toString())
                    }
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)
            outcome = table.updateItem(updateItemSpec)
        } catch (e: Exception) {
            System.err.println("remove() - failed to update multiple attributes in " + logicalTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }

        // Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
        // result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
        //  result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
        //result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));
        result.returnValue = ReturnStatusEnum.SUCCESS
        return result
    }

    override fun restore(id: String?): ReturnResult {
        val result = ReturnResult(id)
        val assignment = this[id] as Assignment
        if (Utils.checkIfNullObject(assignment)) {
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(logicalTableName)
        var outcome: UpdateItemOutcome? = null
        try {
            val updateItemSpec = UpdateItemSpec()
                .withPrimaryKey("id", id)
                .withUpdateExpression(
                    "ADD #sys.#v :one " //    + "REMOVE #DLQ "
                            + "SET #sys.queued = :one, queued = :one, " //	+ "#sys.queue_selected = :false, "
                            + "last_updated_timestamp = :lut, "
                            + "#sys.last_updated_timestamp = :lut, "
                            + "#sys.queue_add_timestamp = :lut"
                ) //error occured due to extra space at the end
                //+ "#sys.#st = :st")
                .withNameMap(
                    NameMap()
                        .with("#v", "version") //.with("#DLQ", "DLQ")
                        //.with("#st", "status")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    assignment.systemInfo?.let {
                        ValueMap().withInt(":one", 1)
                            .withInt(":v", it.version) //.withBoolean(":false", false)
                            //.withString(":st", StatusEnum.READY_TO_SHIP.toString())
                            .withString(":lut", odt.toString())
                    }
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)
            outcome = table.updateItem(updateItemSpec)
        } catch (e: Exception) {
            System.err.println("restore() - failed to update multiple attributes in " + logicalTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }

//        Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
//        result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
//        result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
//        result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));
        result.returnValue = ReturnStatusEnum.SUCCESS
        return result
    }

    override fun peek(): PeekResult {
        var exclusiveStartKey: Map<String?, AttributeValue?>? = null
        val result = PeekResult()
        val values: MutableMap<String, AttributeValue> = HashMap()
        values[":one"] = AttributeValue().withN("1")
        var selectedID: String? = null
        var selectedVersion = 0
        var recordForPeekIsFound = false
        do {

            // this query grabs everything in sparse index, in other words all values with queued = 1
            val queryRequest = QueryRequest()
                .withProjectionExpression("id, scheduled, system_info")
                .withIndexName(Constants.QUEUEING_INDEX_NAME)
                .withTableName(logicalTableName)
                .withKeyConditionExpression("queued = :one") //  manages what will be in the queue
                // This is unnecessary in our implementation because we don't need to denote a status on our items
                //.withFilterExpression("attribute_not_exists(queue_selected)")   // we need to look for the stragglers
                .withLimit(250)
                .withScanIndexForward(true)
                .withExpressionAttributeValues(values)
            queryRequest.withExclusiveStartKey(exclusiveStartKey)
            val queryResult = dynamoDB!!.query(queryRequest)
            exclusiveStartKey = queryResult.lastEvaluatedKey
            for (itemMap in queryResult.items) {
                val sysMap = itemMap["system_info"]!!
                    .m
                selectedID = itemMap["id"]!!.s
                selectedVersion = sysMap["version"]!!.n.toInt()
                recordForPeekIsFound = true

                // no need to go further
                if (recordForPeekIsFound) break
            }
        } while (!recordForPeekIsFound && exclusiveStartKey != null)
        if (Utils.checkIfNullObject(selectedID)) {
            result.returnValue = ReturnStatusEnum.FAILED_EMPTY_QUEUE
            return result
        }

        // assign ID to 'result'
        result.id = selectedID

        // this is an simplest way to construct an App object
        val assignment = this[selectedID] as Assignment
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(logicalTableName)
        val tsUTC = System.currentTimeMillis()
        var outcome: UpdateItemOutcome? = null

        // This functionality is used to prevent us from interacting with same variable multiple times; However,
        // we won't necessarily need this functionality, because all we want to do is get top value and see it schedule
        try {

            // IMPORTANT
            // please note, we are not updating top-level attribute `last_updated_timestamp` in order to avoid re-indexing the order
            val updateItemSpec = UpdateItemSpec().withPrimaryKey("id", assignment.id)
                .withUpdateExpression(
                    "ADD #sys.#v :one "
                            + "SET queued = :one, #sys.queued = :one,"
                            + " #sys.last_updated_timestamp = :lut, #sys.queue_peek_timestamp = :lut, "
                            + "#sys.peek_utc_timestamp = :ts"
                )
                .withNameMap(
                    NameMap()
                        .with("#v", "version") //.with("#st", "status")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    ValueMap()
                        .withInt(":one", 1)
                        .withInt(":v", selectedVersion)
                        .withLong(":ts", tsUTC)
                        .withString(":lut", odt.toString())
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)
            outcome = table.updateItem(updateItemSpec)
        } catch (e: Exception) {
            System.err.println("peek() - failed to update multiple attributes in " + logicalTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }

        // result.setId(outcome.getItem().getString("id"));

        // adding this to get the fresh data from DDB
        val peekedAssignment = this[selectedID] as Assignment
        result.peekedAssignmentObject = peekedAssignment

        //Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
        //  result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
        // result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));

        // result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
        //result.setTimestampMillisUTC(((BigDecimal)sysMap.get("peek_utc_timestamp")).intValue());
        result.returnValue = ReturnStatusEnum.SUCCESS
        return result
    }

    override fun enqueue(id: String?): EnqueueResult {
        val result = EnqueueResult(id)
        if (Utils.checkIfNullOrEmptyString(id)) {
            System.out.printf("ID is not provided ... cannot proceed with the enqueue() operation!%n")
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_PROVIDED
            return result
        }

//
        val retrievedAssignment = this[id] as Assignment
        if (Utils.checkIfNullObject(retrievedAssignment)) {
            System.out.printf("Assignment with ID [%s] cannot be found!%n", id)
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val version = retrievedAssignment.systemInfo?.version
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(logicalTableName)
        if (version != null) {
            result.version = version
        }
        result.lastUpdatedTimestamp = retrievedAssignment.systemInfo?.lastUpdatedTimestamp
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        try {
            val updateItemSpec = UpdateItemSpec().withPrimaryKey("id", id)
                .withUpdateExpression(
                    "ADD #sys.#v :one "
                            + "SET queued = :one, #sys.queued = :one," //#sys.queue_selected = :false, "
                            + "last_updated_timestamp = :lut, #sys.last_updated_timestamp = :lut, "
                            + "#sys.queue_added_timestamp = :lut"
                ) //#sys.#st = :st")
                .withNameMap(
                    NameMap()
                        .with("#v", "version") //.with("#st", "status")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    version?.let {
                        ValueMap()
                            .withInt(":one", 1) //.withBoolean(":false", false)
                            .withInt(":v", it)
                            .withString(":lut", odt.toString())
                    }
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)
            val outcome = table.updateItem(updateItemSpec)
            val sysMap = outcome.item.getRawMap("system_info")
            result.version = (sysMap["version"] as BigDecimal?)!!.toInt()
            result.lastUpdatedTimestamp = sysMap["last_updated_timestamp"] as String?
            // result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
            val assignment = this[id] as Assignment
            result.assignment = assignment
        } catch (e: Exception) {
            System.err.println("enqueue() - failed to update multiple attributes in " + logicalTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }
        result.returnValue = ReturnStatusEnum.SUCCESS
        return result
    }

    override fun dequeue(): DequeueResult {
        val peekResult = peek()
        var dequeueResult: DequeueResult? = null
        if (peekResult.isSuccessful) {
            val ID = peekResult.id
            val removeResult = this.remove(ID)
            dequeueResult = DequeueResult.Companion.fromReturnResult(removeResult)
            if (removeResult.isSuccessful) {
                dequeueResult.dequeuedAssignmentObject = peekResult.peekedAssignmentObject
            }
        } else {
            dequeueResult = DequeueResult.Companion.fromReturnResult(peekResult)
        }
        return dequeueResult
    }

    override fun getQueueStats(): QueueStats {
        var totalQueueSize = 0

        var exclusiveStartKey: Map<String?, AttributeValue?>? = null

        val names: MutableMap<String, String> = HashMap()
        names["#q"] = "queued"
        val values: MutableMap<String, AttributeValue> = HashMap()
        values[":one"] = AttributeValue().withN("1")

        val peekedRecords = 0

        val allQueueIDs: List<String> = ArrayList()
        val processingIDs: List<String> = ArrayList()


        // Gets items: id and system_info; using the scheduled_index
        // that meet the condition of queued == 1 and then organizes them descending order


        // Gets items: id and system_info; using the scheduled_index
        // that meet the condition of queued == 1 and then organizes them descending order
        do {
            val queryRequest = QueryRequest()
                .withProjectionExpression("id, system_info")
                .withIndexName(Constants.QUEUEING_INDEX_NAME)
                .withTableName(logicalTableName)
                .withExpressionAttributeNames(names)
                .withKeyConditionExpression("#q = :one")
                .withScanIndexForward(true)
                .withLimit(250)
                .withExpressionAttributeValues(values)

            // exclusive start key is just a structure containing the keys needed to resume the query and grab the next n items
            queryRequest.withExclusiveStartKey(exclusiveStartKey)
            val queryResult = dynamoDB!!.query(queryRequest)
            exclusiveStartKey = queryResult.lastEvaluatedKey
            for (itemMap in queryResult.items) {
                ++totalQueueSize
            }
        } while (exclusiveStartKey != null)

        val result = QueueStats()
        //result.setTotalRecordsInProcessing(peekedRecords);
        //result.setTotalRecordsInProcessing(peekedRecords);
        result.totalRecordsInQueue = totalQueueSize
        if (Utils.checkIfNotNullAndNotEmptyCollection(allQueueIDs)) result.first100IDsInQueue = allQueueIDs
        if (Utils.checkIfNotNullAndNotEmptyCollection(processingIDs)) result.first100SelectedIDsInQueue = processingIDs

        return result
    }

    private fun putImpl(assignment: Assignment) {
        Utils.throwIfNullObject(assignment, "assignment object cannot be NULL!")
        val version = 0

        // check if already present
        val retrievedAssignment = dbMapper!!.load(Assignment::class.java, assignment.id)
        if (!Utils.checkIfNullObject(retrievedAssignment)) {
            dbMapper!!.delete(retrievedAssignment)
        }
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val system = SystemInfo(assignment.id)
        system.isInQueue = false // we want all items placed in dynamodb to also be placed into queue
        // system.setSelectedFromQueue(false);
        //system.setStatus(assignment.getSystemInfo().getStatus());
        system.creationTimestamp = odt.toString()
        system.lastUpdatedTimestamp = odt.toString()
        system.version = version + 1
        assignment.systemInfo = system

        // store it in DynamoDB
        dbMapper!!.save(assignment)
    }


    class Builder
    /**
     * Default constructor
     */
    {
        var credentials: AWSCredentials? = null
        var logicalTableName: String? = null
        var awsRegion: String? = null
        var awsCredentialsProfileName: String? = null
        private var client: Dynamodb? = null

        /**
         * Create a QueueSDK
         *
         * @return QueueSdkClient
         */
        fun build(): Dynamodb? {
            if (Utils.checkIfNullObject(client)) {
                client = Dynamodb(this)
                client!!.initialize()
            }
            return client
        }

        /**
         * Specify AWS region
         * If not used, default value is 'us-east-1'.
         *
         * @param region Proper AWS Region string
         * @return Builder
         */
        fun withRegion(region: String?): Builder {
            awsRegion = region
            return this
        }

        /**
         * Specify local credential profile
         *
         * @param profile This is the name of the local AWS Credential profile
         * @return Builder
         */
        fun withCredentialsProfileName(profile: String?): Builder {
            awsCredentialsProfileName = profile
            return this
        }

        fun withLogicalTableName(logicalTableName: String?): Builder {
            this.logicalTableName = logicalTableName
            return this
        }
    }

    companion object {
        private val RETRY_POLICY =
            RetryPolicy(null, PredefinedRetryPolicies.DYNAMODB_DEFAULT_BACKOFF_STRATEGY, 10, false)
    }
}
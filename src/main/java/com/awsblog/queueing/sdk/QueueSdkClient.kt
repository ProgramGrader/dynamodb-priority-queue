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
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.ReturnValue
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.awsblog.queueing.Constants
import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.config.ConfigField
import com.awsblog.queueing.config.Configuration
import com.awsblog.queueing.model.*
import com.awsblog.queueing.utils.Utils
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Collectors

/**
 * Queue SDK client
 *
 * @author zorani
 * @version 1.00
 */
class QueueSdkClient private constructor(builder: Builder) {
    /**
     * Part of the fluid API chaining. Once all parameters are set with .with(...) methods, build() method needs to be called.
     * No other client call can be used before build() call is executed.
     *
     * @return QueueSdkClient
     */
    private fun initialize(): QueueSdkClient {
        Locale.setDefault(Locale.ENGLISH)
        var accessKey = System.getenv("AWS_ACCESS_KEY_ID")
        var secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")

        // If the aws credentials aren't given via cli then checks Environment variables
        if (Utils.checkIfNotNullAndNotEmptyString(accessKey) && Utils.checkIfNotNullAndNotEmptyString(secretKey)) {
            if (Utils.checkIfNullOrEmptyString(accessKey)) accessKey = System.getenv("AWS_ACCESS_KEY_ID")
            if (Utils.checkIfNullOrEmptyString(secretKey)) secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")
            aWSCredentials = BasicAWSCredentials(accessKey, secretKey)
        } else if (Utils.checkIfNotNullAndNotEmptyString(awsCredentialsProfileName)) {
            aWSCredentials = ProfileCredentialsProvider(awsCredentialsProfileName).credentials
        }
        val builder = AmazonDynamoDBClientBuilder.standard()
        if (!Utils.checkIfNullObject(aWSCredentials)) builder.withCredentials(
            AWSStaticCredentialsProvider(
                aWSCredentials
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
        //config = Configuration.loadConfiguration()

        // searches this map for tableName
//		this.actualTableName = this.config.getTablesMap().get(this.logicalTableName).getTableName();
//		Utils.throwIfNullOrEmptyString(this.actualTableName, "Actual DynamoDB table name is not found!");
        actualTableName = logicalTableName
        val s3builder = AmazonS3ClientBuilder.standard()
        if (!Utils.checkIfNullObject(aWSCredentials)) s3builder.withCredentials(
            AWSStaticCredentialsProvider(
                aWSCredentials
            )
        )
        if (!Utils.checkIfNullObject(awsRegion)) s3builder.withRegion(awsRegion)
        s3 = s3builder.build()
        val snsBuilder = AmazonSNSClientBuilder.standard()
        if (!Utils.checkIfNullObject(aWSCredentials)) snsBuilder.withCredentials(
            AWSStaticCredentialsProvider(
                aWSCredentials
            )
        )
        if (!Utils.checkIfNullObject(awsRegion)) snsBuilder.withRegion(awsRegion)
        sns = snsBuilder.build()
        val mapperConfig = DynamoDBMapperConfig.builder()
            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
            .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT) //.withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
            .withTableNameOverride(TableNameOverride(actualTableName))
            .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING)
            .build()
        dbMapper = DynamoDBMapper(dynamoDB, mapperConfig)
        return this
    }// exclusive start key is just a structure containing the keys needed to resume the query and grab the next n items
    //result.setTotalRecordsInProcessing(peekedRecords);
// Gets items: id and system_info; using the scheduled_index
    // that meet the condition of queued == 1 and then organizes them descending order
    /**
     * Get the queue depth statistics
     * A good queue stats would be to show the amount of items that have been dequeued? No
     * Currently we have processed, items in queue
     */
    val queueStats: QueueStats
        get() {
            var totalQueueSize = 0
            var exclusiveStartKey: Map<String?, AttributeValue?>? = null
            val names: MutableMap<String, String> = HashMap()
            names["#q"] = "queued"
            val values: MutableMap<String, AttributeValue> = HashMap()
            values[":one"] = AttributeValue().withN("1")
            val peekedRecords = 0
            val allQueueIDs: List<String?> = ArrayList()
            val processingIDs: List<String?> = ArrayList()


            // Gets items: id and system_info; using the scheduled_index
            // that meet the condition of queued == 1 and then organizes them descending order
            do {
                val queryRequest = QueryRequest()
                    .withProjectionExpression("id, system_info")
                    .withIndexName(Constants.QUEUEING_INDEX_NAME)
                    .withTableName(actualTableName)
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
            result.totalRecordsInQueue = totalQueueSize
            if (Utils.checkIfNotNullAndNotEmptyCollection(allQueueIDs)) result.first100IDsInQueue = allQueueIDs
            if (Utils.checkIfNotNullAndNotEmptyCollection(processingIDs)) result.first100SelectedIDsInQueue =
                processingIDs
            return result
        }

    /**
     * Retrieve DLQ information
     *
     */
    val dLQStats: DLQResult
        get() {
            val startTime = System.currentTimeMillis()
            var totalDLQSize = 0
            var exclusiveStartKey: Map<String?, AttributeValue?>? = null
            val names: MutableMap<String, String> = HashMap()
            names["#DLQ"] = "DLQ"
            val values: MutableMap<String, AttributeValue> = HashMap()
            values[":one"] = AttributeValue().withN("1")
            val listBANs: MutableList<String> = ArrayList()
            do {
                val queryRequest = QueryRequest()
                    .withProjectionExpression("id, DLQ, system_info")
                    .withIndexName(Constants.DLQ_QUEUEING_INDEX_NAME)
                    .withTableName(actualTableName)
                    .withExpressionAttributeNames(names)
                    .withKeyConditionExpression("#DLQ = :one")
                    .withLimit(250)
                    .withExpressionAttributeValues(values)
                queryRequest.withExclusiveStartKey(exclusiveStartKey)
                val queryResult = dynamoDB!!.query(queryRequest)
                exclusiveStartKey = queryResult.lastEvaluatedKey
                for (itemMap in queryResult.items) {
                    ++totalDLQSize
                    if (listBANs.size < 100) listBANs.add(itemMap["id"]!!.s)
                }
            } while (exclusiveStartKey != null)
            val result = DLQResult()
            result.first100IDsInQueue = listBANs
            result.setTotalRecordsInQueue(totalDLQSize)
            return result
        }

    /**
     * Get the Assignment object/record from DynamoDB
     *
     * @param id
     * @return
     */
    operator fun get(id: String?): Assignment? {
        if (Utils.checkIfNullOrEmptyString(id)) {
            System.out.printf("ID is not provided ... cannot retrieve the assignment record!%n")
            return null
        }
        return dbMapper!!.load(Assignment::class.java, id!!.trim { it <= ' ' })
    }

    /**
     * Put the new object into DynamoDB - replaces values, if there is already data with the same primary key
     */
    fun put(assignment: Assignment) {
        putImpl(assignment, false)
    }

    /**
     * Put & Merge object in the DynamoDB
     *
     * @param assignment
     */
    fun upsert(assignment: Assignment) {
        putImpl(assignment, true)
    }

    private fun putImpl(assignment: Assignment, useUpsert: Boolean) {
        Utils.throwIfNullObject(assignment, "assignment object cannot be NULL!")
        var version = 0

        // check if already present
        val retrievedAssignment = dbMapper!!.load(Assignment::class.java, assignment.id)
        if (!Utils.checkIfNullObject(retrievedAssignment)) {
            if (useUpsert) {
                version = retrievedAssignment.systemInfo?.version!!
            } else {
                dbMapper!!.delete(
                    retrievedAssignment
                )
            }
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
    /**
     * Method for changing the status of the record
     * This call should not be used unless there are operational issues and there are live issues that needs to be resolved.
     *
     */
    //	public ReturnResult updateStatus(String id, StatusEnum newStatus) {
    //
    //		ReturnResult result = new ReturnResult(id);
    //
    //		if (Utils.checkIfNullOrEmptyString(id)) {
    //
    //			System.out.printf("ERROR: ID is not provided ... cannot retrieve the record!%n");
    //			result.setReturnValue(ReturnStatusEnum.FAILED_ID_NOT_FOUND);
    //			return result;
    //		}
    //
    //		Map<String,AttributeValue> key = new HashMap<>();
    //		key.put("id", new AttributeValue().withS(id));
    //
    //		DynamoDB ddb = new DynamoDB(this.dynamoDB);
    //		Table table = ddb.getTable(this.actualTableName);
    //
    //		Assignment assignment = this.get(id);
    //
    //		if (Utils.checkIfNullObject(assignment)) {
    //
    //			System.out.printf("ERROR: Customer with ID [%s] cannot be found!%n", id);
    //			result.setReturnValue(ReturnStatusEnum.FAILED_ID_NOT_FOUND);
    //			return result;
    //		}
    //
    //		//StatusEnum prevStatus = shipment.getSystemInfo().getStatus();
    //		int version = assignment.getSystemInfo().getVersion();
    //
    //		//result.setStatus(newStatus);
    //
    ////		if (prevStatus == newStatus) {
    ////            result.setVersion(version);
    ////            result.setLastUpdatedTimestamp(shipment.getLastUpdatedTimestamp());
    ////			result.setReturnValue(ReturnStatusEnum.SUCCESS);
    ////			return result;
    ////		}
    //
    //		OffsetDateTime odt = OffsetDateTime.now(ZoneOffset.UTC);
    //
    //        try {
    //
    //            UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("id", id)
    //                .withUpdateExpression("ADD #sys.#v :inc" +
    //						//" SET #sys.#st = :st," +
    //						" #sys.last_updated_timestamp = :lut, last_updated_timestamp = :lut")
    //                .withNameMap(new NameMap()
    //                		.with("#v", "version")
    //                		//.with("#st", "status")
    //                		.with("#sys", "system_info"))
    //                .withValueMap(
    //                    new ValueMap()
    //                    	.withInt(":inc", 1)
    //                    	.withInt(":v", version)
    //                    	.withString(":lut", odt.toString()))
    //                    	//.withString(":st", newStatus.toString()))
    //                .withConditionExpression("#sys.#v = :v")
    //                .withReturnValues(ReturnValue.ALL_NEW);
    //
    //            UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
    //
    //            Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
    //            //result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
    //            result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
    //            result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));
    //        }
    //        catch (Exception e) {
    //            System.err.println("updateFullyConstructedFlag() - failed to update multiple attributes in " + this.actualTableName);
    //            System.err.println(e.getMessage());
    //
    //            result.setReturnValue(ReturnStatusEnum.FAILED_DYNAMO_ERROR);
    //    		return result;
    //        }
    //
    //        result.setReturnValue(ReturnStatusEnum.SUCCESS);
    //		return result;
    //	}
    //
    /**
     * enqueue() the record into the queue
     * Only provides an illusion of movement, we are using a sparse index to define the priority queues values
     * this is controlled by the queued attribute which is = 1 whenever an item is in queue
     */
    fun enqueue(id: String?): EnqueueResult {
        val result = EnqueueResult(id)
        if (Utils.checkIfNullOrEmptyString(id)) {
            System.out.printf("ID is not provided ... cannot proceed with the enqueue() operation!%n")
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_PROVIDED
            return result
        }

//
        val retrievedAssignment = this[id]
        if (Utils.checkIfNullObject(retrievedAssignment)) {
            System.out.printf("Assignment with ID [%s] cannot be found!%n", id)
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val version = retrievedAssignment!!.systemInfo?.version
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(actualTableName)
        result.version = version!!
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
                    ValueMap()
                        .withInt(":one", 1) //.withBoolean(":false", false)
                        .withInt(":v", version)
                        .withString(":lut", odt.toString())
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)

            val outcome = table.updateItem(updateItemSpec)
            val sysMap = outcome.item.getRawMap("system_info")
            result.version = (sysMap["version"] as BigDecimal?)!!.toInt()
            result.lastUpdatedTimestamp = sysMap["last_updated_timestamp"] as String?
            // result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
            val assignment = this[id]
            result.assignment = assignment
        } catch (e: Exception) {
            System.err.println("enqueue() - failed to update multiple attributes in " + actualTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }
        result.returnValue = ReturnStatusEnum.SUCCESS
        return result
    }

    /**
     * Peek the record from the queue
     * Peek's update query request is failing and preventing dequeue to work properly
     * Once this is fixed, we can create another method called pop that removes the value with
     * the highest priority from queue and from dynamodb
     *
     * @return
     */
    fun peek(): PeekResult {
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
                .withTableName(actualTableName)
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
        val assignment = this[selectedID]
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(actualTableName)
        val tsUTC = System.currentTimeMillis()
        var outcome: UpdateItemOutcome? = null

        // This functionality is used to prevent us from interacting with same variable multiple times; However,
        // we won't necessarily need this functionality, because all we want to do is get top value and see it schedule
        try {

            // IMPORTANT
            // please note, we are not updating top-level attribute `last_updated_timestamp` in order to avoid re-indexing the order
            val updateItemSpec = UpdateItemSpec().withPrimaryKey("id", assignment!!.id)
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
            System.err.println("peek() - failed to update multiple attributes in " + actualTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }

        // result.setId(outcome.getItem().getString("id"));

        // adding this to get the fresh data from DDB
        val peekedAssignment = this[selectedID]
        result.peekedAssignmentObject = peekedAssignment

        //Map<String, Object> sysMap = outcome.getItem().getRawMap("system_info");
        //  result.setVersion(((BigDecimal)sysMap.get("version")).intValue());
        // result.setLastUpdatedTimestamp((String)sysMap.get("last_updated_timestamp"));

        // result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
        //result.setTimestampMillisUTC(((BigDecimal)sysMap.get("peek_utc_timestamp")).intValue());
        result.returnValue = ReturnStatusEnum.SUCCESS
        return result
    }

    /**
     * dequeue() logic
     * It peeks the record from the queue, it locks it for the short time and removes from the queue
     *
     * @return
     */
    fun dequeue(): DequeueResult? {
        val peekResult = peek()
        var dequeueResult: DequeueResult? = null
        if (peekResult.isSuccessful) {
            val ID = peekResult.id
            val removeResult = this.remove(ID)
            dequeueResult = DequeueResult.fromReturnResult(removeResult)
            if (removeResult.isSuccessful) {
                dequeueResult.dequeuedAssignmentObject = peekResult.peekedAssignmentObject
            }
        } else {
            dequeueResult = DequeueResult.fromReturnResult(peekResult)
        }
        return dequeueResult
    }

    /**
     * Acknowledge that the account is processed and that can be removed from the queue
     * queued = 0 (and REMOVE), queue_selected = false
     *
     * this needs to delete the item from table not remove it from queue
     * @param id
     * @return
     */
    fun remove(id: String?): ReturnResult {
        val result = ReturnResult(id)
        val assignment = this[id]
        if (Utils.checkIfNullObject(assignment)) {
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(actualTableName)
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
                    assignment!!.systemInfo?.let {
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
            System.err.println("remove() - failed to update multiple attributes in " + actualTableName)
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

    /**
     * Put back the record in the queue
     * Only works if we don't remove item
     * You want to check if item exists in table, but has not been queued if it does restore it
     */
    fun restore(id: String?): ReturnResult {
        val result = ReturnResult(id)
        val assignment = this[id]
        if (Utils.checkIfNullObject(assignment)) {
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(actualTableName)
        var outcome: UpdateItemOutcome? = null
        try {
            val updateItemSpec = UpdateItemSpec()
                .withPrimaryKey("id", id)
                .withUpdateExpression(
                    "ADD #sys.#v :one "
                            + "REMOVE #DLQ "
                            + "SET #sys.queued = :one, queued = :one, " //	+ "#sys.queue_selected = :false, "
                            + "last_updated_timestamp = :lut, "
                            + "#sys.last_updated_timestamp = :lut, "
                            + "#sys.queue_add_timestamp = :lut, "
                            + "#sys.#st = :st"
                )
                .withNameMap(
                    NameMap()
                        .with("#v", "version")
                        .with("#DLQ", "DLQ")
                        .with("#st", "status")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    assignment!!.systemInfo?.let {
                        ValueMap().withInt(":one", 1)
                            .withInt(":v", it.version)
                            .withBoolean(":false", false) //.withString(":st", StatusEnum.READY_TO_SHIP.toString())
                            .withString(":lut", odt.toString())
                    }
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)
            outcome = table.updateItem(updateItemSpec)
        } catch (e: Exception) {
            System.err.println("restore() - failed to update multiple attributes in " + actualTableName)
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

    /**
     * Send the problematic record to DLQ
     *
     * @param id
     * @return ReturnResult
     */
    fun sendToDLQ(id: String?): ReturnResult {
        val result = ReturnResult(id)
        val assignment = this[id]
        if (Utils.checkIfNullObject(assignment)) {
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(actualTableName)
        var outcome: UpdateItemOutcome? = null
        try {
            val updateItemSpec = UpdateItemSpec()
                .withPrimaryKey("id", id)
                .withUpdateExpression(
                    "ADD #sys.#v :one "
                            + "REMOVE queued "
                            + "SET #DLQ = :one, #sys.queued = :zero, " // + "#sys.queue_selected = :false, "
                            + "last_updated_timestamp = :lut, "
                            + "#sys.last_updated_timestamp = :lut, "
                            + "#sys.dlq_add_timestamp = :lut, #sys.#st = :st"
                )
                .withNameMap(
                    NameMap()
                        .with("#v", "version")
                        .with("#DLQ", "DLQ")
                        .with("#st", "status")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    assignment!!.systemInfo?.let {
                        ValueMap().withInt(":one", 1)
                            .withInt(":v", it.version)
                            .withInt(":zero", 0)
                            .withBoolean(":false", false) // .withString(":st", StatusEnum.IN_DLQ.toString())
                            .withString(":lut", odt.toString())
                    }
                )
                .withConditionExpression("#sys.#v = :v and #sys.queued = :one")
                .withReturnValues(ReturnValue.ALL_NEW)
            outcome = table.updateItem(updateItemSpec)
        } catch (e: Exception) {
            System.err.println("restore() - failed to update multiple attributes in " + actualTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }
        val sysMap = outcome.item.getRawMap("system_info")
        result.version = (sysMap["version"] as BigDecimal?)!!.toInt()
        //result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
        result.lastUpdatedTimestamp = sysMap["last_updated_timestamp"] as String?

        //result.setReturnValue(ReturnStatusEnum.SUCCESS);
        return result
    }

    /**
     * Changes the last_updated_timestamp and increments 'version' by 1
     *
     * Not sure how to use this... if we have any coroutines maybe we should consider using this
     * @param id
     * @return
     */
    fun touch(id: String?): ReturnResult {
        val result = ReturnResult(id)
        val assignment = this[id]
        if (Utils.checkIfNullObject(assignment)) {
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(actualTableName)
        var outcome: UpdateItemOutcome? = null
        try {
            val updateItemSpec = UpdateItemSpec()
                .withPrimaryKey("id", id)
                .withUpdateExpression(
                    "ADD #sys.#v :one "
                            + "SET last_updated_timestamp = :lut, "
                            + "#sys.last_updated_timestamp = :lut"
                )
                .withNameMap(
                    NameMap()
                        .with("#v", "version")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    assignment!!.systemInfo?.let {
                        ValueMap().withInt(":one", 1)
                            .withInt(":v", it.version)
                            .withString(":lut", odt.toString())
                    }
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)
            outcome = table.updateItem(updateItemSpec)
        } catch (e: Exception) {
            System.err.println("restore() - failed to update multiple attributes in " + actualTableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }
        val sysMap = outcome.item.getRawMap("system_info")
        result.version = (sysMap["version"] as BigDecimal?)!!.toInt()
        // result.setStatus(StatusEnum.valueOf((String)sysMap.get("status")));
        result.lastUpdatedTimestamp = sysMap["last_updated_timestamp"] as String?

        //result.setReturnValue(ReturnStatusEnum.SUCCESS);
        return result
    }

    /**
     * Get the first 'size' items form the Assignment table
     *
     * @return
     */
    fun listIDs(size: Int): MutableList<String?>? {
        val scanExpression = DynamoDBScanExpression()
        scanExpression.withLimit(size)
        scanExpression.withProjectionExpression("id, scheduled, system_info")

        //List<String> listOfIDs = new ArrayList<>();
        val result = dbMapper!!.scanPage(Assignment::class.java, scanExpression)
        return result.results.stream()
            .map { obj: Assignment -> obj.id }
            .limit(size.toLong())
            .collect(Collectors.toList())
    }
    /**
     * Get the first 'size' items form the Shipment table
     *
     * @return
     */
    //	public List<String> listExtendedIDs(int size) {
    //
    //		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    //		scanExpression.withLimit(size);
    //		scanExpression.withProjectionExpression("id, system_info");
    //
    //		//List<String> listOfIDs = new ArrayList<>();
    //
    //		ScanResultPage<Shipment> result = this.dbMapper.scanPage(Shipment.class, scanExpression);
    //
    ////		List<String> listOfIDs = result.getResults().stream()
    ////										.map(s -> s.getId() + " - status: " //+ s.getSystemInfo().getStatus().toString())
    ////										.limit(size)
    ////										.collect(Collectors.toList());
    //
    //		return listOfIDs;
    //	}
    /**
     * Delete the assignment record from DynamoDB by Assignment ID
     *
     * @param id
     */
    fun delete(id: String?) {
        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be NULL!")
        dbMapper!!.delete(id?.let { Assignment(it) })
    }

    /**
     * This method should be called at the end of the SDK usage
     */
    fun shutdown() {
        if (dynamoDB != null) dynamoDB!!.shutdown()
        if (sns != null) sns!!.shutdown()
        if (s3 != null) s3!!.shutdown()
        aWSCredentials = null
        dynamoDB = null
        sns = null
        s3 = null
    }

    /**
     * @return the s3
     */
    // ------------- fields
    var s3: AmazonS3? = null
        private set

    /**
     * @return the SNS
     */
    var sns: AmazonSNS? = null
        private set

    /**
     * @return the dynamoDB
     */
    var dynamoDB: AmazonDynamoDB? = null
        private set
    private val key: ConfigField? = null

    /**
     * Get the AWSCredentials object reference
     *
     * @return AWSCredentials
     */
    var aWSCredentials: AWSCredentials? = null
        private set

    /**
     * @return the actualTableName
     */
    var actualTableName: String? = null
        private set
    private var logicalTableName: String? = null

    /**
     * @return the config
     */
    var config: Configuration? = null
        private set

    /**
     * Get the AWS Region
     *
     * @return the awsRegion
     */
    //private String configFileName = null;
    //private String configContent = null;
    var awsRegion: String? = null

    /**
     * Get the AWS Credential's Profile name
     *
     * @return the awsCredentialsProfileName
     */
    var awsCredentialsProfileName: String? = null
    private var dbMapper: DynamoDBMapper? = null

    /**
     * Private constructor using Builder
     *
     * @param builder Builder object reference
     */
    init {
        logicalTableName = builder.logicalTableName
        awsRegion = builder.awsRegion
        aWSCredentials = builder.credentials
        awsCredentialsProfileName = builder.awsCredentialsProfileName
    }

    /**
     * Inner builder class
     *
     * @author zorani
     */
    class Builder
    /**
     * Default constructor
     */
    {
        private var configFileName: String? = null
        private var configContent: String? = null
        var credentials: AWSCredentials? = null
        var logicalTableName: String? = null
        var awsRegion: String? = null
        var awsCredentialsProfileName: String? = null
        private var client: QueueSdkClient? = null

        /**
         * Create a QueueSDK
         *
         * @return QueueSdkClient
         */
        fun build(): QueueSdkClient? {

            //if (Utils.checkIfNullObject(this.logicalTableName)) this.logicalTableName = Constants.DEFAULT_SHIPMENT_TABLE_NAME;
            // only if QueueSdkClient is not formed yet
            if (Utils.checkIfNullObject(client)) {
                client = QueueSdkClient(this)
                client!!.initialize()
            }
            return client
        }

        /**
         * Set the configuration filename
         * This can be used if running on the host/desktop/Fargate. For Lambda, rely on the configuration stored as a JAR resource file
         *
         * @param fileName Local filename where configuration json file is located
         * @return Builder
         */
        fun withConfigurationFileName(fileName: String?): Builder {
            configFileName = fileName
            return this
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

        /**
         * Specify local credential profile
         * Shortcut named method. This is the same method as withCredentialsProfileName()
         *
         * @param profile This is the name of the local AWS Credential profile
         * @return Builder
         */
        fun withProfile(profile: String?): Builder {
            awsCredentialsProfileName = profile
            return this
        }

        /**
         * Specify a logical table name
         * The builder will find the actual DynamoDB table name by provided logical name.
         *
         * @param logicalTableName
         * @return
         */
        fun withLogicalTableName(logicalTableName: String?): Builder {
            this.logicalTableName = logicalTableName
            return this
        }

        /**
         * Provide the AWS Credentials
         *
         * @param awsCredentials User's provided AWSCredential
         * @return Builder
         */
        fun withCredentials(awsCredentials: AWSCredentials?): Builder {
            credentials = awsCredentials
            return this
        }

        /**
         * Set the configuration JSON content for the Client to load it at the startup time.
         * This is used if the configuration data sits in S3, DynamoDB or other not-local file places
         *
         * @param configurationJsonContent This should represent a JSON configuration content
         * @return Builder
         */
        fun withConfigurationContent(configurationJsonContent: String?): Builder {
            configContent = configurationJsonContent
            return this
        }
    } //end Builder

    companion object {
        private val RETRY_POLICY =
            RetryPolicy(null, PredefinedRetryPolicies.DYNAMODB_DEFAULT_BACKOFF_STRATEGY, 10, false)
    }
} // end QueueSdkClient

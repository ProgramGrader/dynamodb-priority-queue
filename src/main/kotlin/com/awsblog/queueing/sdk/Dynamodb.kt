package com.awsblog.queueing.sdk

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.*
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder
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
import com.awsblog.queueing.appdata.PriorityQueueElement
import com.awsblog.queueing.model.*
import com.awsblog.queueing.utils.Utils
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


class Dynamodb(builder: Builder) : Database {
    private var credentials: AWSCredentials?
    var tableName: String?
    var awsRegion: String?
    var awsCredentialsProfileName: String?
    var dbMapper: DynamoDBMapper? = null
    var dynamoDB: AmazonDynamoDB? = null

    init {
        tableName = builder.tableName
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

        val mapperConfig = DynamoDBMapperConfig.builder()
            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
            .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT) //.withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
            .withTableNameOverride(TableNameOverride(tableName))
            .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING)
            .build()
        dbMapper = DynamoDBMapper(dynamoDB, mapperConfig)
        return this
    }


    // Function made for testing sdk locally
    fun initialize(endpoint: AwsClientBuilder.EndpointConfiguration): Dynamodb {
        Locale.setDefault(Locale.ENGLISH)
        val builder = AmazonDynamoDBClientBuilder.standard()
        val credentialsProvide: AWSCredentialsProvider = DefaultAWSCredentialsProviderChain()
        builder.credentials = credentialsProvide

        // initializes instance at endpoint.
        builder.setEndpointConfiguration(endpoint)
        dynamoDB = builder.build()


        dbMapper = DynamoDBMapper(dynamoDB) // , mapperConfig
        return this
    }

    override fun get(id: String?) : PriorityQueueElement?{
       return dbMapper!!.load(PriorityQueueElement::class.java, id?.trim { it <= ' ' })
    }

    override fun put(item: PriorityQueueElement) {
        Utils.throwIfNullObject(item, " object cannot be NULL!")
        val accessed = 0

        // check if already present
        val retrievedItem = dbMapper!!.load(PriorityQueueElement::class.java, item.id)
        if (!Utils.checkIfNullObject(retrievedItem)) {
            dbMapper!!.delete(retrievedItem)
        }

        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val system = SystemInfo(item.id)
        system.creationTimestamp = odt.toString()
        system.lastUpdatedTimestamp = odt.toString()
        system.accessed = accessed + 1
        item.systemInfo = system

        // store it in DynamoDB
        dbMapper!!.save(item)
    }

    override fun delete(id: String) {
        Utils.throwIfNullOrEmptyString(id, "DatabaseItem ID cannot be NULL!")
        dbMapper!!.delete(PriorityQueueElement(id))
    }

    // currently unnecessary but may be useful down the road
//    override fun remove(id: String?): ReturnResult {
//        val result = ReturnResult(id)
//        val item = this[id]
//        if (Utils.checkIfNullObject(item)) {
//            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
//            return result
//        }
//        val odt = OffsetDateTime.now(ZoneOffset.UTC)
//        val ddb = DynamoDB(dynamoDB)
//        val table = ddb.getTable(tableName)
//        var outcome: UpdateItemOutcome? = null
//        try {
//            val updateItemSpec = UpdateItemSpec()
//                .withPrimaryKey("id", id)
//                .withUpdateExpression(
//                    "ADD #sys.#v :one "
//                            + "REMOVE #sys.peek_utc_timestamp, queued, #DLQ "
//                            + "SET #sys.queued = :zero ,"
//                            + "#sys.last_updated_timestamp = :lut, "
//                            + "last_updated_timestamp = :lut, "
//                            + "#sys.queue_remove_timestamp = :lut"
//                )
//                .withNameMap(
//                    NameMap().with("#v", "accessed")
//                        .with("#DLQ", "DLQ")
//                        .with("#sys", "system_info")
//                )
//                .withValueMap(
//                    item?.systemInfo?.let {
//                        ValueMap()
//                            .withInt(":one", 1)
//                            .withInt(":zero", 0)
//                            .withInt(":v", it.accessed)
//                            .withString(":lut", odt.toString())
//                    }
//                )
//                .withConditionExpression("#sys.#v = :v")
//                .withReturnValues(ReturnValue.ALL_NEW)
//            outcome = table.updateItem(updateItemSpec)
//        } catch (e: Exception) {
//            System.err.println("remove() - failed to update multiple attributes in " + tableName)
//            System.err.println(e.message)
//            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
//            return result
//        }
//        result.returnValue = ReturnStatusEnum.SUCCESS
//        return result
//    }

    // currently unnecessary but may be useful down the road
//    override fun restore(id: String?): ReturnResult {
//        val result = ReturnResult(id)
//        val item = this[id]
//        if (Utils.checkIfNullObject(item)) {
//            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
//            return result
//        }
//        val odt = OffsetDateTime.now(ZoneOffset.UTC)
//        val ddb = DynamoDB(dynamoDB)
//        val table = ddb.getTable(tableName)
//        var outcome: UpdateItemOutcome? = null
//        try {
//            val updateItemSpec = UpdateItemSpec()
//                .withPrimaryKey("id", id)
//                .withUpdateExpression(
//                    "ADD #sys.#v :one "
//                            + "SET #sys.queued = :one, queued = :one, "
//                            + "last_updated_timestamp = :lut, "
//                            + "#sys.last_updated_timestamp = :lut, "
//                            + "#sys.queue_add_timestamp = :lut"
//                ) //errors occur if there's an extra space at the end of expression
//
//                .withNameMap(
//                    NameMap()
//                        .with("#v", "accessed")
//
//                        .with("#sys", "system_info")
//                )
//                .withValueMap(
//                    item?.systemInfo?.let {
//                        ValueMap().withInt(":one", 1)
//                            .withInt(":v", it.accessed)
//                            .withString(":lut", odt.toString())
//                    }
//                )
//                .withConditionExpression("#sys.#v = :v")
//                .withReturnValues(ReturnValue.ALL_NEW)
//            outcome = table.updateItem(updateItemSpec)
//        } catch (e: Exception) {
//            System.err.println("restore() - failed to update multiple attributes in " + tableName)
//            System.err.println(e.message)
//            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
//            return result
//        }
//
//        result.returnValue = ReturnStatusEnum.SUCCESS
//        return result
//    }

    override fun retrieve(id: String?): ReturnResult {
        var exclusiveStartKey: Map<String?, AttributeValue?>? = null
        val result = ReturnResult()
        val values: MutableMap<String, AttributeValue> = HashMap()
        values[":one"] = AttributeValue().withN("1")
        values[":id"] = AttributeValue().withS(id)
        var selectedID: String? = null

        // this query grabs everything in sparse index, in other words all values with queued = 1
        val queryRequest = QueryRequest()
            .withProjectionExpression("id, schedule, system_info")
            .withIndexName(Constants.QUEUEING_INDEX_NAME)
            .withTableName(tableName)
            .withKeyConditionExpression("queued = :one")
            .withFilterExpression("id = :id")
            .withLimit(250)
            .withScanIndexForward(true)
            .withExpressionAttributeValues(values)
        queryRequest.withExclusiveStartKey(exclusiveStartKey)
        val queryResult = dynamoDB!!.query(queryRequest)
        exclusiveStartKey = queryResult.lastEvaluatedKey

        selectedID = queryResult.items[0]["id"]!!.s

        if( (selectedID != id && exclusiveStartKey != null)){
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }

        result.id = selectedID

        // this a simple way to construct an App object
        result.resultObject = this[selectedID]
        result.returnValue = ReturnStatusEnum.SUCCESS

        return result
    }


    override fun peek(n : Int): List<PriorityQueueElement>{
        var exclusiveStartKey: Map<String?, AttributeValue?>? = null
        val result = emptyList<PriorityQueueElement>().toMutableList()
        val values: MutableMap<String, AttributeValue> = HashMap()
        values[":one"] = AttributeValue().withN("1")
        var selectedID: String? = null
        val selectIDLIst = emptyList<String?>().toMutableList()
        var selectedaccessed = 0
        var recordsForPeekIsFound = false
        do {

            // this query grabs everything in sparse index, in other words all values with queued = 1
            val queryRequest = QueryRequest()
                .withProjectionExpression("id, schedule, system_info")
                .withIndexName(Constants.QUEUEING_INDEX_NAME)
                .withTableName(tableName)
                .withKeyConditionExpression("queued = :one")
                .withLimit(250)
                .withScanIndexForward(true)
                .withExpressionAttributeValues(values)
            queryRequest.withExclusiveStartKey(exclusiveStartKey)
            val queryResult = dynamoDB!!.query(queryRequest)
            exclusiveStartKey = queryResult.lastEvaluatedKey

            if(n > queryResult.items.size){
                System.err.println("peek() - number of items in queue are " + queryResult.items.size +
                        " number of items to peek is exceeding this value ("+ n +")"+tableName)
                return result
            }

            // Assuming that his for loop gets the first value in the queryResult: which is the top
            var i = 0
            for (itemMap in queryResult.items) {
                val sysMap = itemMap["system_info"]!!.m
                selectedID = itemMap["id"]!!.s
                selectedaccessed = sysMap["accessed"]!!.n.toInt()

                i++
                selectIDLIst.add(selectedID)
                this[selectedID].let {
                    if (it != null) {
                        result.add(it)
                    }
                }
                if(i==n){
                    recordsForPeekIsFound = true
                    break

                }
                // no need to go further

            }
        } while (!recordsForPeekIsFound && exclusiveStartKey != null)
        if (Utils.checkIfNullObject(selectedID)) {
            return result
        }

        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(tableName)
        val tsUTC = System.currentTimeMillis()
        var outcome: UpdateItemOutcome? = null

        try {
            // This query is just adding +1 to the accessed to denote that the item was touched in the system

            for( id in selectIDLIst) {
                val updateItemSpec = UpdateItemSpec().withPrimaryKey("id", id)
                    .withUpdateExpression(
                        "ADD #sys.#v :one "
                                + "SET queued = :one, #sys.queued = :one,"
                                + " #sys.last_updated_timestamp = :lut, #sys.queue_peek_timestamp = :lut, "
                                + "#sys.peek_utc_timestamp = :ts"
                    )
                    .withNameMap(
                        NameMap()
                            .with("#v", "accessed") //.with("#st", "status")
                            .with("#sys", "system_info")
                    )
                    .withValueMap(
                        ValueMap()
                            .withInt(":one", 1)
                            .withInt(":v", selectedaccessed)
                            .withLong(":ts", tsUTC)
                            .withString(":lut", odt.toString())
                    )
                    .withConditionExpression("#sys.#v = :v")
                    .withReturnValues(ReturnValue.ALL_NEW)
                outcome = table.updateItem(updateItemSpec)
            }
        } catch (e: Exception) {
            System.err.println("peek() - failed to update multiple attributes in " + tableName)
            System.err.println(e.message)
            return result
        }

        return result
    }


    override fun enqueue(item: PriorityQueueElement?): ReturnResult {


        val result = ReturnResult(item?.id)
        val it = this[item?.id]
        if (Utils.checkIfNullObject(it)) {
            System.out.printf("Item with ID [%s] does not exist in database inserting it", item?.id)
            if (item != null) {
                put(item)
            }
        }

        val retrievedItem = this[item?.id]
        if (Utils.checkIfNullObject(retrievedItem)) {
            System.out.printf("Item with ID [%s] cannot be found!%n", item?.id)
            result.returnValue = ReturnStatusEnum.FAILED_ID_NOT_FOUND
            return result
        }
        val accessed = retrievedItem?.systemInfo?.accessed
        val ddb = DynamoDB(dynamoDB)
        val table = ddb.getTable(tableName)
        if (accessed != null) {
            result.accessed = accessed
        }
        result.lastUpdatedTimestamp = retrievedItem?.systemInfo?.lastUpdatedTimestamp
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        try {
            val updateItemSpec = UpdateItemSpec().withPrimaryKey("id", item?.id)
                .withUpdateExpression(
                    "ADD #sys.#v :one "
                            + "SET queued = :one, #sys.queued = :one,"
                            + "#sys.last_updated_timestamp = :lut, "
                            + "#sys.queue_added_timestamp = :lut"
                )
                .withNameMap(
                    NameMap()
                        .with("#v", "accessed")
                        .with("#sys", "system_info")
                )
                .withValueMap(
                    accessed?.let {
                        ValueMap()
                            .withInt(":one", 1)
                            .withInt(":v", it)
                            .withString(":lut", odt.toString())
                    }
                )
                .withConditionExpression("#sys.#v = :v")
                .withReturnValues(ReturnValue.ALL_NEW)
            val outcome = table.updateItem(updateItemSpec)
            val sysMap = outcome.item.getRawMap("system_info")
            result.accessed = (sysMap["accessed"] as BigDecimal?)!!.toInt()
            result.lastUpdatedTimestamp = sysMap["last_updated_timestamp"] as String?
            val updatedItem = this[item?.id]
            result.resultObject = updatedItem
        } catch (e: Exception) {
            System.err.println("enqueue() - failed to update multiple attributes in " + tableName)
            System.err.println(e.message)
            result.returnValue = ReturnStatusEnum.FAILED_DYNAMO_ERROR
            return result
        }
        result.returnValue = ReturnStatusEnum.SUCCESS
        return result
    }

   override fun dequeue(n:Int) :  List<PriorityQueueElement>{

       val dequeuedItems = peek(n)
       if (dequeuedItems.isNotEmpty()) {
           for (item in dequeuedItems) {
               val ID = item.id
               ID?.let { this.delete(it) }
              if( this[ID] != null){
                  System.out.printf("Item with ID [%s] does not exist in database inserting it", item.id)
              }
           }
       }else if (n!=0){
           System.err.println("Queue is empty")
       }
        return dequeuedItems
    }

    override fun getQueueStats(): QueueStats {
        var totalQueueSize = 0

        var exclusiveStartKey: Map<String?, AttributeValue?>? = null

        val names: MutableMap<String, String> = HashMap()
        names["#q"] = "queued"
        val values: MutableMap<String, AttributeValue> = HashMap()
        values[":one"] = AttributeValue().withN("1")


        val allQueueIDs: List<String> = ArrayList()
        val processingIDs: List<String> = ArrayList()

        do {
            val queryRequest = QueryRequest()
                .withProjectionExpression("id, system_info")
                .withIndexName(Constants.QUEUEING_INDEX_NAME)
                .withTableName(tableName)
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

        result.totalRecordsInQueue = totalQueueSize
        if (Utils.checkIfNotNullAndNotEmptyCollection(allQueueIDs)) result.first100IDsInQueue = allQueueIDs
        if (Utils.checkIfNotNullAndNotEmptyCollection(processingIDs)) result.first100SelectedIDsInQueue = processingIDs

        return result
    }


    class Builder
    /**
     * Default constructor
     */
    {
        var credentials: AWSCredentials? = null
        var tableName: String? = null
        var awsRegion: String? = null
        var awsCredentialsProfileName: String? = null
        private var client: Dynamodb? = null

        /**
         * Create a DynamoDB Client
         *
         * @return DynamoDB
         */
        fun build(): Dynamodb? {
            if (Utils.checkIfNullObject(client)) {
                client = Dynamodb(this)
                client!!.initialize()
            }
            return client
        }
        fun build(endpoint: AwsClientBuilder.EndpointConfiguration ): Dynamodb? {
            if (Utils.checkIfNullObject(client)) {
                client = Dynamodb(this)
                client!!.initialize(endpoint)
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

        fun withTableName(tableName: String?): Builder {
            this.tableName = tableName
            return this
        }
    }

    companion object {
        private val RETRY_POLICY =
            RetryPolicy(null, PredefinedRetryPolicies.DYNAMODB_DEFAULT_BACKOFF_STRATEGY, 10, false)
    }
}
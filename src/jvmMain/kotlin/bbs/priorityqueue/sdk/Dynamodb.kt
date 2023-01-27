package bbs.priorityqueue.sdk

import bbs.priorityqueue.Constants
import bbs.priorityqueue.appdata.PriorityQueueElement
import bbs.priorityqueue.model.QueueStats
import bbs.priorityqueue.model.ReturnResult
import bbs.priorityqueue.model.ReturnStatusEnum
import bbs.priorityqueue.model.SystemInfo
import bbs.priorityqueue.utils.Utils
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.*
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier


class Dynamodb(builder: Builder) : Database {
    private var credentialProvider: ProfileCredentialsProvider?

    var tableName: String?
    var awsRegion: Region?
    var awsCredentialsProfileName: String?
    var dbMapper: DynamoDbTable<PriorityQueueElement>? = null
    var dynamoDB: DynamoDbClient? = null
    var dynamoDBEnhanced: DynamoDbEnhancedClient? = null

    init {
        tableName = builder.tableName
        awsRegion = builder.awsRegion
        awsCredentialsProfileName = builder.awsCredentialsProfileName
        credentialProvider = ProfileCredentialsProvider.create(awsCredentialsProfileName)
    }

    override fun initialize() : Database {
        Locale.setDefault(Locale.ENGLISH)

        dynamoDB =DynamoDbClient.builder()
            .credentialsProvider(credentialProvider)
            .region(awsRegion)
            .httpClient(software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder().build())
            .build()

        dynamoDBEnhanced = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDB)
            .build()


        //Tables must be static to support graalvm
        val systemInfoSchema = StaticTableSchema
            .builder(SystemInfo::class.java)
            .newItemSupplier(::SystemInfo)
            .addAttribute(String::class.java){
                it.name("id").getter(SystemInfo::id::get).setter(SystemInfo::id::set)
            }
            .addAttribute(String::class.java){
                it.name("lastUpdatedTimestamp").getter(SystemInfo::lastUpdatedTimestamp::get).setter(SystemInfo::lastUpdatedTimestamp::set)
            }
            .addAttribute(String::class.java){
                it.name("creationTimestamp").getter(SystemInfo::creationTimestamp::get).setter(SystemInfo::creationTimestamp::set)
            }
            .addAttribute(Int::class.java){
                it.name("accessed").getter(SystemInfo::accessed::get).setter(SystemInfo::accessed::set)
            }
            .build()

        val tableSchema = StaticTableSchema
            .builder(PriorityQueueElement::class.java)
            .newItemSupplier(::PriorityQueueElement)
            .addAttribute(String::class.java){
                it.name("id").getter(PriorityQueueElement::id::get).setter(PriorityQueueElement::id::set)
                    .tags(primaryPartitionKey())
            }
            .addAttribute(String::class.java){
                it.name("schedule").getter(PriorityQueueElement::schedule::get).setter(PriorityQueueElement::schedule::set)
                    .tags(secondarySortKey("scheduled-index"))
            }
            .addAttribute(String::class.java){
                it.name("data").getter(PriorityQueueElement::data::get).setter(PriorityQueueElement::data::set)
            }
            .addAttribute(EnhancedType.documentOf(SystemInfo::class.java, systemInfoSchema))
            {
                it.name("system_info").getter(PriorityQueueElement::systemInfo::get).setter(PriorityQueueElement::systemInfo::set)
            }
            .build()

        dbMapper = dynamoDBEnhanced?.table(tableName, tableSchema)
        return this
    }


    // Function made for testing sdk locally
    fun initialize(endpoint: URI): Dynamodb {
        Locale.setDefault(Locale.ENGLISH)

        // initializes instance at endpoint.
        dynamoDB = DynamoDbClient.builder()
            .credentialsProvider(credentialProvider)
            .region(awsRegion)
            .endpointOverride(endpoint)
            .build()

        dynamoDBEnhanced = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDB)
            .build()

        dbMapper =dynamoDBEnhanced?.table(tableName, TableSchema.fromBean(PriorityQueueElement::class.java)) // , mapperConfig
        return this
    }

    override fun get(id: String?) : PriorityQueueElement? {

        val key:Key = Key.builder().partitionValue(id).build()
        return dbMapper?.getItem { requestBuilder: GetItemEnhancedRequest.Builder -> requestBuilder.key(key) }

    }

    override fun put(item: PriorityQueueElement) {
        Utils.throwIfNullObject(item, " object cannot be NULL!")
        val accessed = 0

        // check if already present
        val key:Key = Key.builder().partitionValue(item.id).build()
        val retrievedItem = dbMapper!!.getItem { requestBuilder: GetItemEnhancedRequest.Builder -> requestBuilder.key(key) }
        if (!Utils.checkIfNullObject(retrievedItem)) {
            dbMapper!!.deleteItem(retrievedItem)
        }

        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val system = SystemInfo(item.id)
        system.creationTimestamp = odt.toString()
        system.lastUpdatedTimestamp = odt.toString()
        system.accessed = accessed + 1
        item.systemInfo = system

        // store it in DynamoDB
        dbMapper!!.putItem(item)
    }

    override fun delete(id: String) {
        Utils.throwIfNullOrEmptyString(id, "DatabaseItem ID cannot be NULL!")
        dbMapper!!.deleteItem(PriorityQueueElement(id))
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
        values[":one"] = AttributeValue.fromN("1")
        values[":id"] = AttributeValue.fromS(id)
        var selectedID: String? = null

        // this query grabs everything in sparse index, in other words all values with queued = 1
        val queryRequest = QueryRequest.builder()
            .projectionExpression("id, schedule, system_info")
            .indexName(Constants.QUEUEING_INDEX_NAME)
            .tableName(tableName)
            .keyConditionExpression("queued = :one")
            .filterExpression("id = :id")
            .limit(250)
            .scanIndexForward(true)
            .expressionAttributeValues(values)
            .exclusiveStartKey(exclusiveStartKey)
            .tableName(tableName)
            .build()

        val queryResult = dynamoDB?.query(queryRequest)
        exclusiveStartKey = queryResult?.lastEvaluatedKey()

        selectedID = queryResult?.items()?.get(0)?.get("id")!!.s()

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
        values[":one"] = AttributeValue.fromN("1")
        var selectedID: String? = null
        val selectIDLIst = emptyList<String?>().toMutableList()
        var selectedaccessed = 0
        var recordsForPeekIsFound = false
        do {

            // this query grabs everything in sparse index, in other words all values with queued = 1
            val queryRequest = QueryRequest.builder()
                .projectionExpression("id, schedule, system_info")
                .indexName(Constants.QUEUEING_INDEX_NAME)
                .tableName(tableName)
                .keyConditionExpression("queued = :one")
                .limit(250)
                .scanIndexForward(true)
                .expressionAttributeValues(values)
                .exclusiveStartKey(exclusiveStartKey)
                .tableName(tableName)
                .build()
            val queryResult = dynamoDB!!.query(queryRequest)
            exclusiveStartKey = queryResult.lastEvaluatedKey()

            if(n > queryResult.items().size){
                System.err.println("peek() - number of items in queue are " + queryResult.items().size +
                        " number of items to peek is exceeding this value ("+ n +")"+tableName)
                return result
            }

            // this for loop gets the first value in the queryResult: which is the top
            var i = 0
            for (itemMap in queryResult.items()) {
                val sysMap = itemMap["system_info"]!!.m()
                selectedID = itemMap["id"]!!.s()
                selectedaccessed = sysMap["accessed"]!!.n().toInt()

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
//        val ddb = Duj(dynamoDB)
      //  val table = dbMapper?.
        val tsUTC = System.currentTimeMillis()
        var outcome: UpdateItemResponse? = null

        try {

            val attributeNameMap :MutableMap<String, String> = mutableMapOf<String, String>()
                attributeNameMap.set("#v", "accessed")
                attributeNameMap.set("#sys", "system_info")

            val attributeValueMap :MutableMap<String, AttributeValue> = mutableMapOf<String, AttributeValue>()
                attributeValueMap.set(":one", AttributeValue.fromN("1"))
                attributeValueMap.set(":v", AttributeValue.fromN(selectedaccessed.toString()))
                attributeValueMap.set(":ts", AttributeValue.fromN(tsUTC.toString()) )
                attributeValueMap.set(":lut",AttributeValue.fromS(odt.toString()))

            for (id in selectIDLIst) {
                val keyMap :MutableMap<String, AttributeValue> = mutableMapOf<String, AttributeValue>()
                keyMap.set("id", AttributeValue.fromS(id))

                // This query is just adding +1 to the accessed to denote that the item was touched in the system\

                val updateItemSpec = UpdateItemRequest.builder()
                    .key(keyMap)
                    .updateExpression(
                        "ADD #sys.#v :one "
                                + "SET queued = :one, #sys.queued = :one,"
                                + " #sys.last_updated_timestamp = :lut, #sys.queue_peek_timestamp = :lut, "
                                + "#sys.peek_utc_timestamp = :ts"
                    )
                    .expressionAttributeNames(attributeNameMap)
                    .expressionAttributeValues(attributeValueMap)
                    .conditionExpression("#sys.#v = :v")
                    .returnValues(ReturnValue.ALL_NEW)
                    .tableName(tableName)
                    .build()

                outcome = dynamoDB!!.updateItem(updateItemSpec)

            }
        }catch (e: Exception) {
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
//        val ddb = DynamoDB(dynamoDB)
       // val table = ddb.getTable(tableName)
        if (accessed != null) {
            result.accessed = accessed
        }
        result.lastUpdatedTimestamp = retrievedItem?.systemInfo?.lastUpdatedTimestamp
        val odt = OffsetDateTime.now(ZoneOffset.UTC)

        val attributeNameMap :MutableMap<String, String> = mutableMapOf<String, String>()
            attributeNameMap.set("#v", "accessed")
            attributeNameMap.set("#sys", "system_info")

        val attributeValueMap :MutableMap<String, AttributeValue> = mutableMapOf<String, AttributeValue>()
            attributeValueMap.set(":one", AttributeValue.fromN("1"))
            attributeValueMap.set(":v", AttributeValue.fromN(accessed.toString()))
            attributeValueMap.set(":lut",AttributeValue.fromS(odt.toString()))
        try {
            val keyMap :MutableMap<String, AttributeValue> = mutableMapOf<String, AttributeValue>()
            keyMap["id"] = AttributeValue.fromS(item?.id)

            val updateItemSpec = UpdateItemRequest.builder()
                .key(keyMap)
                .updateExpression(
                    "ADD #sys.#v :one "
                            + "SET queued = :one, #sys.queued = :one,"
                            + "#sys.last_updated_timestamp = :lut, "
                            + "#sys.queue_added_timestamp = :lut"
                )
                .expressionAttributeNames(attributeNameMap)
                .expressionAttributeValues(attributeValueMap)
                .conditionExpression("#sys.#v = :v")
                .returnValues(ReturnValue.ALL_NEW)
                .tableName(tableName)
                .build()

            val outcome = dynamoDB!!.updateItem(updateItemSpec)

            //val sysMap = outcome.getValueForField("system_info", SystemInfo::class.java)
            val updatedItem = this[item?.id]
            result.accessed= updatedItem?.systemInfo?.accessed!!
            result.lastUpdatedTimestamp= updatedItem?.systemInfo?.lastUpdatedTimestamp!!
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
        values[":one"] = AttributeValue.fromN("1")    //withN("1")


        val allQueueIDs: List<String> = ArrayList()
        val processingIDs: List<String> = ArrayList()

        do {
            val queryRequest = QueryRequest.builder()
                .projectionExpression("id, system_info")
                .indexName(Constants.QUEUEING_INDEX_NAME)
                .tableName(tableName)
                .expressionAttributeNames(names)
                .keyConditionExpression("#q = :one")
                .scanIndexForward(true)
                .limit(250)
                .expressionAttributeValues(values)
                .exclusiveStartKey(exclusiveStartKey)
                .tableName(tableName)
                .build()

            // exclusive start key is just a structure containing the keys needed to resume the query and grab the next n items
            val queryResult = dynamoDB!!.query(queryRequest)
            exclusiveStartKey = queryResult.lastEvaluatedKey()
            for (itemMap in queryResult.items()) {
                ++totalQueueSize
            }
        } while (exclusiveStartKey != null && queryResult.lastEvaluatedKey().isNotEmpty())

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
        var tableName: String? = null
        var awsRegion: Region? = null
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
        fun build(endpoint:URI ): Dynamodb? {
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
        fun withRegion(region: Region?): Builder {
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

//    companion object {
//        private val RETRY_POLICY =
//            RetryPolicy(null, PredefinedRetryPolicies.DYNAMODB_DEFAULT_BACKOFF_STRATEGY, 10, false)
//    }
}
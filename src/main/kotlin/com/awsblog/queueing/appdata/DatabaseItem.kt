package com.awsblog.queueing.appdata

import com.amazonaws.services.dynamodbv2.datamodeling.*
import com.awsblog.queueing.model.SystemInfo
import com.awsblog.queueing.utils.Utils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty


@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBTable(tableName = "assignment_schedule")
class DatabaseItem {

    constructor(id: String?) {
        this.id = id
    }

    //Todo convert schedule to date time
    constructor(id: String, schedule: String?) {
        Utils.throwIfNullOrEmptyString(id, "ID cannot be null!")
        this.id = id.trim { it <= ' ' }
        systemInfo = SystemInfo(this.id)
        this.schedule = schedule
    }


    /**
     * Hash key for the priority queue
     */
    @get:DynamoDBAttribute(attributeName = "id")
    @get:DynamoDBHashKey(attributeName = "id")
    @JsonProperty("id")
    var id: String? = null

    /**
     * item to store GSI range key values
     */
    @get:DynamoDBAttribute(attributeName = "scheduled")
    @JsonProperty("schedule")
    var schedule: String? = null

    @get:DynamoDBAttribute(attributeName = "last_updated_timestamp")
    @get:JsonIgnore
    var lastUpdatedTimestamp: String?
        /**
         * @return the lastUpdatedTimestamp
         */
        get() = systemInfo?.lastUpdatedTimestamp
        /**
         * @param lastUpdatedTimestamp the lastUpdatedTimestamp to set
         */
        set(lastUpdatedTimestamp) {
            systemInfo?.lastUpdatedTimestamp = lastUpdatedTimestamp
        }

    @JsonProperty("version")//accessed
    var version = 0

    @get:DynamoDBAttribute(attributeName = "system_info")
    @JsonProperty("system_info")
    var systemInfo: SystemInfo? = null


    @JsonProperty("data")
    private var data: DatabaseItemData? = null

    @DynamoDBAttribute(attributeName = "data")
    fun getData(): Any? {
        return data?.item
    }

//    fun bsetData(data: DatabaseItemData) {
//         this.data?.item = data.item
//    }

    fun setData(data: DynamoDBTypeConverter<Any,Any>) {
        this.data = data.unconvert(this.data) as DatabaseItemData?
    }
    // assignmentData
}

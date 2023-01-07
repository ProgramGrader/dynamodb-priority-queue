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

    constructor()

    constructor(id: String?) {
        this.id = id
    }

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

    @JsonProperty("version")
    var version = 0

    @get:DynamoDBAttribute(attributeName = "system_info")
    @JsonProperty("system_info")
    var systemInfo: SystemInfo? = null


    @JsonProperty("data")
    private var data: DatabaseItemData? = null

    @DynamoDBTypeConverted(converter = AssignmentItemConverter::class)
    @DynamoDBAttribute(attributeName = "data")
    fun getData(): DatabaseItemData? {
        return data
    }

    fun setData(data: DatabaseItemData) {
        this.data = data
    }

    class AssignmentItemConverter : DynamoDBTypeConverter<String, AssignmentItem> {
        override fun convert(`object`: AssignmentItem?): String? {
            val item: AssignmentItem? = `object`
            var assignment: String? = null
            try {
                if (item != null) {
                    assignment = String().format(" %s,%s", item.id, item.dueDate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return assignment
        }

        override fun unconvert(s: String?): AssignmentItem {
            val item = AssignmentItem()
            try {
                if (!s.isNullOrEmpty()) run {
                    val data = s.split(",")
                    item.id = data[0].trim()
                    item.dueDate = data[1].trim()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return item
        }

// {"data_1":{"S":"2023-01-14"},
// "items":{"L":
// [{"M":{"id":{"S":"Hello_World"},"dueDate":{"S":"2017-01-22"}}}]} ,
// "id":{"S":"A-101"}
// }
    }
}
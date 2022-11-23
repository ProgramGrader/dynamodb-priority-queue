package com.awsblog.queueing.appdata

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.awsblog.queueing.model.SystemInfo
import com.awsblog.queueing.utils.Utils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBTable(tableName = "assignment_schedule")
class Assignment {
    /**
     * @return the id
     */
    // ---------------------- fields
    @get:DynamoDBAttribute(attributeName = "id")
    @get:DynamoDBHashKey(attributeName = "id")
    @JsonProperty("id")
    var id: String? = null
        private set

    @JsonProperty("schedule")
    private var schedule: String? = null
    /**
     * @return the systemInfo
     */
    /**
     * @param systemInfo the systemInfo to set
     */
    @get:DynamoDBAttribute(attributeName = "system_info")
    @JsonProperty("system_info")
    var systemInfo: SystemInfo? = null
    // ----------------------
    /**
     * C-tor
     */
    constructor() {
        systemInfo = SystemInfo()
    }

    /**
     * C-tor
     *
     * @param id, schedule
     */
    constructor(id: String) {
        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be null!")
        this.id = id.trim { it <= ' ' }
    }

    constructor(id: String, schedule: String?) {
        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be null!")
        this.id = id.trim { it <= ' ' }
        systemInfo = SystemInfo(this.id)
        this.schedule = schedule
    }

    /**
     * @param id the id to set
     */
    fun setId(id: String) {
        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be null!")
        this.id = id.trim { it <= ' ' }
        systemInfo!!.id = this.id
    }

    fun setSchedule(schedule: String?) {
        Utils.throwIfNullOrEmptyString(schedule, "Assignment Schedule cannot be null!")
        this.schedule = schedule
    }
    /**
     * Mark the object as a partially constructed
     */
    //    public void markAsPartiallyConstructed() {
    //
    //        this.systemInfo.setStatus(StatusEnum.UNDER_CONSTRUCTION);
    //    }
    /**
     * @return the isQueued
     */ //@DynamoDBAttribute(attributeName = "queued")
    //@DynamoDBTyped(DynamoDBAttributeType.N)
    @get:DynamoDBIgnore
    @get:JsonIgnore
    val isQueued: Boolean
        get() = systemInfo!!.isInQueue
    /**
     * @return the lastUpdatedTimestamp
     */
    /**
     * @param lastUpdatedTimestamp the lastUpdatedTimestamp to set
     */
    @get:DynamoDBAttribute(attributeName = "last_updated_timestamp")
    @get:JsonIgnore
    var lastUpdatedTimestamp: String?
        get() = systemInfo!!.lastUpdatedTimestamp
        set(lastUpdatedTimestamp) {
            systemInfo!!.lastUpdatedTimestamp = lastUpdatedTimestamp
        }

    /**
     * Reset the data inside the Assignment's system info object
     */
    fun resetSystemInfo() {
        systemInfo = SystemInfo(id)
    }

    /**
     * @return get Schedule
     */
    @DynamoDBAttribute(attributeName = "scheduled")
    fun getSchedule(): String? {
        return schedule
    }

    /**
     * @param data the data to set
     */
    fun setData(data: String?) {
        schedule = data
    }
}
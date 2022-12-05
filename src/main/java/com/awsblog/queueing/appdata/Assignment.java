package com.awsblog.queueing.appdata;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.awsblog.queueing.model.SystemInfo;
import com.awsblog.queueing.utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBTable(tableName="assignment_schedule")
public class Assignment {

    // ---------------------- fields

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("schedule")
    private String schedule = null;

    @JsonProperty("system_info")
    private SystemInfo systemInfo = null;

    // ----------------------

    /**
     * C-tor
     */
    public Assignment() {

        this.systemInfo = new SystemInfo();

    }

    /**
     * C-tor
     *
     * @param id, schedule
     */

    public Assignment(String id) {
        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be null!");
        this.id = id.trim();
    }

    public Assignment(String id, String schedule) {

        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be null!");

        this.id = id.trim();

        this.systemInfo = new SystemInfo(this.id);
        this.schedule = schedule;
    }

    /**
     * @return the id
     */
    @DynamoDBHashKey(attributeName = "id")
    @DynamoDBAttribute(attributeName="id")
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {

        Utils.throwIfNullOrEmptyString(id, "Assignment ID cannot be null!");

        this.id = id.trim();
        this.systemInfo.setId(this.id);
    }


    public void setSchedule(String schedule) {

        Utils.throwIfNullOrEmptyString(schedule, "Assignment Schedule cannot be null!");

        this.schedule = schedule;
    }

    /**
     * Mark the object as a partially constructed
     */
//    public void markAsPartiallyConstructed() {
//
//        this.systemInfo.setStatus(StatusEnum.UNDER_CONSTRUCTION);
//    }

    /**
     * @param systemInfo the systemInfo to set
     */
    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    /**
     * @return the systemInfo
     */
    @DynamoDBAttribute(attributeName = "system_info")
    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    /**
     * @return the isQueued
     */
    @JsonIgnore
    @DynamoDBIgnore
    //@DynamoDBAttribute(attributeName = "queued")
    //@DynamoDBTyped(DynamoDBAttributeType.N)
    public boolean isQueued() {
        return this.systemInfo.isInQueue();
    }

    /**
     * @return the lastUpdatedTimestamp
     */
    @JsonIgnore
    @DynamoDBAttribute(attributeName = "last_updated_timestamp")
    public String getLastUpdatedTimestamp() {
        return this.systemInfo.getLastUpdatedTimestamp();
    }
    /**
     * @param lastUpdatedTimestamp the lastUpdatedTimestamp to set
     */
    public void setLastUpdatedTimestamp(String lastUpdatedTimestamp) {
        this.systemInfo.setLastUpdatedTimestamp(lastUpdatedTimestamp);
    }

    /**
     * Reset the data inside the Assignment's system info object
     */
    public void resetSystemInfo() {

        this.systemInfo = new SystemInfo(this.id);
    }

    /**
     * @return get Schedule
     */
    @DynamoDBAttribute(attributeName = "scheduled")
    public String getSchedule() {
        return schedule;
    }

    /**
     * @param data the data to set
     */
    public void setData(String data) {
        this.schedule = data;
    }


}

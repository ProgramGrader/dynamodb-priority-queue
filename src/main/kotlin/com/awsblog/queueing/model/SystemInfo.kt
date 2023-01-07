package com.awsblog.queueing.model

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Model all system info for queuing
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBDocument
class SystemInfo {
    /**
     * Default C-tor
     */
    constructor() {
        val odt = OffsetDateTime.now(ZoneOffset.UTC)

        // creation should be overwritten with the real value from DDB
        creationTimestamp = odt.toString()
        lastUpdatedTimestamp = odt.toString()
    }

    /**
     * C-tor
     *
     * @param id
     */
    constructor(id: String?) {
        this.id = id
        val odt = OffsetDateTime.now(ZoneOffset.UTC)

        // creation should be overwritten with the real value from DDB
        creationTimestamp = odt.toString()
        lastUpdatedTimestamp = odt.toString()
    }

    /**
     * @return the ID
     */
    /**
     * @param id the id to set
     */
    // ------------------------ fields
    @get:DynamoDBAttribute(attributeName = "id")
    @get:JsonIgnore
    @JsonProperty("id")
    var id: String? = null
    /**
     * @return the creationTimestamp
     */
    /**
     * @param creationTimestamp the creationTimestamp to set
     */
    @get:DynamoDBAttribute(attributeName = "creation_timestamp")
    @JsonProperty("creation_timestamp")
    var creationTimestamp: String? = null
    /**
     * @return the lastUpdatedTimestamp
     */
    /**
     * @param lastUpdatedTimestamp the lastUpdatedTimestamp to set
     */
    @get:DynamoDBAttribute(attributeName = "last_updated_timestamp")
    @JsonProperty("last_updated_timestamp")
    var lastUpdatedTimestamp: String? = null
    /**
     * @return the version
     */
    /**
     * @param version the version to set
     */

    @get:DynamoDBVersionAttribute
    @get:DynamoDBAttribute(attributeName = "version")
    @JsonProperty("version")
    var version = 1
    /**
     * @return the inQueue
     */
    /**
     * @param inQueue the inQueue to set
     */
    @get:DynamoDBAttribute(attributeName = "queued")
    @JsonProperty("queued")
    var isInQueue = false
    /**
     * @return the addToQueueTimestamp
     */
    /**
     * @param addToQueueTimestamp the addToQueueTimestamp to set
     */

    @get:DynamoDBAttribute(attributeName = "queue_add_timestamp")
    @JsonProperty("queue_add_timestamp")
    var addToQueueTimestamp: String? = null
    /**
     * @return the addToDlqTimestamp
     */
    /**
     * @param addToDlqTimestamp the addToDlqTimestamp to set
     */
    @get:DynamoDBAttribute(attributeName = "dlq_add_timestamp")
    @JsonProperty("dlq_add_timestamp")
    var addToDlqTimestamp: String? = null
    /**
     * @return the peekFromQueueTimestamp
     */
    /**
     * @param peekFromQueueTimestamp the peekFromQueueTimestamp to set
     */
    @get:DynamoDBAttribute(attributeName = "queue_peek_timestamp")
    @JsonProperty("queue_peek_timestamp")
    var peekFromQueueTimestamp: String? = null
    /**
     * @return the removeFromQueueTimestamp
     */
    /**
     * @param removeFromQueueTimestamp the removeFromQueueTimestamp to set
     */
    @get:DynamoDBAttribute(attributeName = "queue_remove_timestamp")
    @JsonProperty("queue_remove_timestamp")
    var removeFromQueueTimestamp: String? = null
} // end SystemInfo

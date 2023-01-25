package bbs.priorityqueue.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Model all system info for queuing
 *
 * @author zorani
 */

@DynamoDbBean
@JsonInclude(JsonInclude.Include.NON_NULL)
//@DynamoDBDocument
class SystemInfo { // end SystemInfo
    /**
     * Default C-tor
     */
    constructor() {
        val odt = OffsetDateTime.now(ZoneOffset.UTC)

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

        creationTimestamp = odt.toString()
        lastUpdatedTimestamp = odt.toString()
    }

    /**
     * The unique ID
     */
    // ------------------------ fields
    @get:DynamoDbAttribute("id")
    @get:JsonIgnore
    @JsonProperty("id")
    var id: String? = null
    /**
     * time of creation
     */
    @get:DynamoDbAttribute("creation_timestamp")
    @JsonProperty("creation_timestamp")
    var creationTimestamp: String? = null


    /**
     * last time element was updated
     */
    @get:DynamoDbAttribute("last_updated_timestamp")
    @JsonProperty("last_updated_timestamp")
    var lastUpdatedTimestamp: String? = null


    /**
     * keeps track of the number of times an element was touched
     */
    @JsonProperty("touched")
    var accessed = 0

}

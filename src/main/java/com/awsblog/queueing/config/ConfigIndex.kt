package com.awsblog.queueing.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Model DynamoDB indexes
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConfigIndex
/**
 * C-tor
 */
{
    /**
     * @return the name
     */
    /**
     * @param name the name to set
     */
    // ----------- fields
    @JsonProperty("index_name")
    var name: String? = null
    /**
     * @return the hashKey
     */
    /**
     * @param hashKey the hashKey to set
     */
    @JsonProperty("hash_key")
    var hashKey: ConfigField? = null
    /**
     * @return the sortKey
     */
    /**
     * @param sortKey the sortKey to set
     */
    @JsonProperty("sort_key")
    var sortKey: ConfigField? = null
    /**
     * @return the gsi
     */
    /**
     * @param gsi the gsi to set
     */
    @JsonProperty("GSI")
    var isGsi = false
    /**
     * @return the readCapacity
     */
    /**
     * @param readCapacity the readCapacity to set
     */
    @JsonProperty("read_capacity")
    var readCapacity = 0
    /**
     * @return the writeCapacity
     */
    /**
     * @param writeCapacity the writeCapacity to set
     */
    @JsonProperty("write_capacity")
    var writeCapacity = 0
} // end ConfigIndex

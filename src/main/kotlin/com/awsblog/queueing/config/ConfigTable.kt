package com.awsblog.queueing.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DynamoDB table configuration parameters
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConfigTable
/**
 * C-tor
 */
{
    /**
     * @return the logicalName
     */
    /**
     * @param logicalName the logicalName to set
     */
    // ----- fields
    @JsonProperty("logical_name")
    var logicalName: String? = null
    /**
     * @return the tableName
     */
    /**
     * @param tableName the tableName to set
     */
    @JsonProperty("table_name")
    var tableName: String? = null
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
    /**
     * @return the partitionKey
     */
    /**
     * @param partitionKey the partitionKey to set
     */
    @JsonProperty("partition_key")
    var partitionKey: ConfigField? = null
    /**
     * @return the sortKey
     */
    /**
     * @param sortKey the sortKey to set
     */
    @JsonProperty("sort_key")
    var sortKey: ConfigField? = null
    /**
     * @return the indexes
     */
    /**
     * @param indexes the indexes to set
     */
    @JsonProperty("indexes")
    var indexes: List<ConfigIndex>? = null
} // end ConfigTable

package com.awsblog.queueing.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Modeling table fields
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConfigField
/**
 * C-tor
 */
{
    /**
     * @return the attributeName
     */
    /**
     * @param attributeName the attributeName to set
     */
    // ---- fields
    @JsonProperty("attribute_name")
    var attributeName: String? = null
    /**
     * @return the attributeType
     */
    /**
     * @param attributeType the attributeType to set
     */
    @JsonProperty("attribute_type")
    var attributeType: String? = null
} // end ConfigField

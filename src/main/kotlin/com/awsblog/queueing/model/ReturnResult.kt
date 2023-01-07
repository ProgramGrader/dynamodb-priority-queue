package com.awsblog.queueing.model

import com.awsblog.queueing.appdata.DatabaseItem
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Return value base object
 *
 * @author zorani
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class ReturnResult {
    constructor()

    /**
     * C-tor
     *
     * @param id
     */
    constructor(id: String?) {
        this.id = id
    }

    val isSuccessful: Boolean
        get() = returnValue == ReturnStatusEnum.SUCCESS

    // ---------------- fields
    @JsonProperty("id")
    var id: String? = null

    @JsonProperty("return_value")
    var returnValue = ReturnStatusEnum.NONE

    @JsonProperty("last_updated_timestamp")
    var lastUpdatedTimestamp: String? = null

    @JsonProperty("version")
    var version = 0

    @JsonIgnore
    var resultObject : DatabaseItem? = null
} // end ReturnResult

/**
 * This Result object is necessary
 */
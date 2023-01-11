package bbs.priorityqueue.model

import bbs.priorityqueue.appdata.PriorityQueueElement
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

    @JsonProperty("accessed")
    var accessed = 0

    @JsonIgnore
    var resultObject : PriorityQueueElement? = null
} // end ReturnResult

package com.awsblog.queueing.model

import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.utils.Utils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Result for the peek() API call
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class DequeueResult : ReturnResult {
    /**
     * Default empty c-tor
     */
    constructor() : super() {}

    /**
     * C-tor
     *
     * @param id
     */
    constructor(id: String?) : super(id) {}

    /**
     * Get the peeked Assignment id
     *
     * @return
     */
    @get:JsonProperty("dequeue_id")
    val peekedAssignmentId: String?
        get() = if (Utils.checkIfNotNullObject(dequeuedAssignmentObject)) dequeuedAssignmentObject!!.id else "NOT FOUND"
    /**
     * @return the dequeuedAssignmentObject
     */
    /**
     * @param dequeuedAssignmentObject the dequeuedAssignmentObject to set
     */
    // ---------------- fields
    @JsonIgnore
    var dequeuedAssignmentObject: Assignment? = null

    companion object {
        /**
         * Get the values from ReturnResult object
         *
         * @param result
         * @return
         */
        fun fromReturnResult(result: ReturnResult): DequeueResult {
            Utils.throwIfNullObject(result, "Resulting object is NULL!")
            val dequeueResult = DequeueResult(result.id)
            dequeueResult.lastUpdatedTimestamp = result.lastUpdatedTimestamp
            //dequeueResult.setStatus(result.getStatus());
            dequeueResult.version = result.version
            dequeueResult.returnValue = result.returnValue
            return dequeueResult
        }
    }
} // end DequeueResult

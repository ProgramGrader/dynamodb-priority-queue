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
 *
 *
 * Need to update this to reflect returning schedules
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class PeekResult : ReturnResult {
    /**
     * Default empty c-tor
     */
    constructor() : super()

    /**
     * C-tor
     */
    constructor(id: String?) : super(id)

    @get:JsonProperty("peeked_id")
    val peekedAssignmentId: String?
        /**
         * Get the peeked shipment id
         *
         * @return
         */
        get() = if (Utils.checkIfNotNullObject(peekedAssignmentObject)) peekedAssignmentObject?.id else "NOT FOUND"

    @get:JsonProperty("peeked_schedule")
    val peekedAssignmentSchedule: String?
        get() = if (Utils.checkIfNotNullObject(peekedAssignmentObject)) peekedAssignmentObject!!.getSchedule() else "NOT FOUND"
    /**
     * @return the timestampMillisUTC
     */
    /**
     * @param timestampMillisUTC the timestampMillisUTC to set
     */
    // ---------------- fields
    @JsonProperty("timestamp_milliseconds_utc")
    var timestampMillisUTC = 0L
    /**
     * @return the peekedShipmentObject
     */
    /**
     * @param peekedAssignmentObject the peekedShipmentObject to set
     */
    @JsonIgnore
    var peekedAssignmentObject: Assignment? = null
} // end PeekResult

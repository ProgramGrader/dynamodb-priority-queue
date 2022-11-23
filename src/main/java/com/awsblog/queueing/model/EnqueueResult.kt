package com.awsblog.queueing.model

import com.awsblog.queueing.appdata.Assignment
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Result for the enqueue() API call
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class EnqueueResult : ReturnResult {
    /**
     * Default empty c-tor
     */
    constructor() : super() {}

    /**
     * C-tor
     */
    constructor(id: String?) : super(id) {}
    /**
     * @return the shipment
     */
    /**
     * @param assignment the shipment to set
     */
    // ---------------- fields
    @JsonIgnore
    var assignment: Assignment? = null
} // end EnqueueResult

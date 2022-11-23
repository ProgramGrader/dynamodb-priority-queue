package com.awsblog.queueing.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

/**
 * Retrieve DLQ depth statistics
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class DLQResult
/**
 * C-tor
 *
 * @param BAN
 */
{
    /**
     * @param totalRecordsInDLQ the totalRecordsInDLQ to set
     */
    fun setTotalRecordsInQueue(totalRecordsInQueue: Int) {
        totalRecordsInDLQ = totalRecordsInQueue
    }

    /**
     * toString
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(String.format("Queue statistics:%n"))
        sb.append(String.format(" >> Total records in DLQ: %d%n", totalRecordsInDLQ))
        if (!first100IDsInQueue!!.isEmpty()) {
            sb.append("   >>> BANs in DLQ: { ")
            var count = 0
            for (ban in first100IDsInQueue!!) {
                if (count++ > 0) sb.append(", ")
                sb.append(ban)
            }
            sb.append(String.format(" }%n"))
        }
        return sb.toString()
    }
    /**
     * @return the first100IDsInQueue
     */
    /**
     * @param first100iDsInQueue the first100IDsInQueue to set
     */
    // ---------------- fields
    @JsonProperty("first_100_IDs_in_queue")
    var first100IDsInQueue: List<String>? = null

    /**
     * @return the totalRecordsInDLQ
     */
    @JsonProperty("total_records_in_DLQ")
    var totalRecordsInDLQ = 0
        private set

    companion object {
        /**
         * Create a RFM_QueueDepthResult object from JSON
         */
        @Throws(IOException::class)
        fun createObjectFromJson(jsonInString: String?): DLQResult {
            val mapper = ObjectMapper()
            return mapper.readValue(jsonInString, DLQResult::class.java)
        }
    }
} // end DLQResult

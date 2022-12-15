package com.awsblog.queueing.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

/**
 * Retrieve various queue depth statistics
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class QueueStats
/**
 * C-tor
 *
 */
{
    /**
     * toString
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(String.format("Queue statistics:%n"))
        sb.append(String.format(" >> Total records in the queue: %d%n", totalRecordsInQueue))
        if (!first100IDsInQueue!!.isEmpty()) {
            sb.append("   >>> IDs in the queue: { ")
            var count = 0
            for (id in first100IDsInQueue!!) {
                if (count++ > 0) sb.append(", ")
                sb.append(id)
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
     * @return the first100SelectedIDsInQueue
     */
    /**
     * @param first100SelectedIDsInQueue the first100SelectedIDsInQueue to set
     */
    @JsonProperty("first_100_selected_IDs_in_queue")
    var first100SelectedIDsInQueue: List<String>? = null
    /**
     * @return the totalRecordsInQueue
     */
    /**
     * @param totalRecordsInQueue the totalRecordsInQueue to set
     */
    @JsonProperty("total_records_in_queue")
    var totalRecordsInQueue = 0

    companion object {
        /**
         * Create a RFM_QueueDepthResult object from JSON
         */
        @Throws(IOException::class)
        fun createObjectFromJson(jsonInString: String?): QueueStats {
            val mapper = ObjectMapper()
            return mapper.readValue(jsonInString, QueueStats::class.java)
        }
    }
} // end QueueStats

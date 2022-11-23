package com.awsblog.queueing.test

import com.awsblog.queueing.sdk.QueueSdkClient
import com.awsblog.queueing.utils.Utils

/**
 * Simple test app for peeking the record from the queue
 *
 * @author zorani
 */
object PeekSDK {
    /**
     * @param args
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val client = QueueSdkClient.Builder().withCredentialsProfileName("default")
            .withRegion("us-east-2")
            .withLogicalTableName("assignment_schedule")
            .build()
        var queueStats = client!!.queueStats
        println(Utils.toJSON(queueStats))
        val peek = client.peek()
        println(Utils.toJSON(peek.peekedAssignmentId))
        queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
    }
} // end TestSDK

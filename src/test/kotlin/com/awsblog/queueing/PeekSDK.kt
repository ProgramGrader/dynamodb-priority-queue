package com.awsblog.queueing

import com.awsblog.queueing.model.PeekResult
import com.awsblog.queueing.model.QueueStats
import com.awsblog.queueing.sdk.Database
import com.awsblog.queueing.sdk.Dynamodb
import com.awsblog.queueing.sdk.IPriorityQueue
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
        val client: Database? = Dynamodb.Builder().withCredentialsProfileName("default")
            .withRegion("us-east-2")
            .withLogicalTableName("assignment_schedule")
            .build()
        var queueStats: QueueStats? = client?.getQueueStats()
        println(Utils.toJSON(queueStats))
        val peek: PeekResult? = client?.peek()
        println(Utils.toJSON(peek?.peekedAssignmentId))
        queueStats = client?.getQueueStats()
        println(Utils.toJSON(queueStats))
    }
} // end TestSDK

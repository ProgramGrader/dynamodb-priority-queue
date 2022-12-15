package com.awsblog.queueing

import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.model.DequeueResult
import com.awsblog.queueing.model.PeekResult
import com.awsblog.queueing.model.QueueStats
import com.awsblog.queueing.sdk.Database
import com.awsblog.queueing.sdk.Dynamodb
import com.awsblog.queueing.sdk.IPriorityQueue
import com.awsblog.queueing.utils.Utils

/**
 * Testing dequeuing - peek and remove
 *
 * @author zorani
 */
object ProcessDequeuedRecordSDK {
    /**
     * Test main
     *
     * @param args
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val client: Database? = Dynamodb.Builder().withCredentialsProfileName("default")
            .withRegion("us-east-2")
            .withLogicalTableName("assignment_schedule")
            .build()
        var queueStats : QueueStats? = client?.getQueueStats()

        println(Utils.toJSON(queueStats))
        val peek: PeekResult? = client?.peek()
        println(Utils.toJSON(peek?.peekedAssignmentId))
        val assignment: Assignment? = peek?.peekedAssignmentObject

        if (Utils.checkIfNullObject(assignment)) {
            println("Nothing to peek() from the queue!")
            System.exit(1)
        }

        val result: DequeueResult? = client?.dequeue()
        println(result?.dequeuedAssignmentObject?.getSchedule())
        queueStats = client?.getQueueStats()
        println(Utils.toJSON(queueStats))
    }
} // end ProcessDequeuedRecordSDK

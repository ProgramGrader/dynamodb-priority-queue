package com.awsblog.queueing

import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.model.EnqueueResult
import com.awsblog.queueing.model.PeekResult
import com.awsblog.queueing.model.QueueStats
import com.awsblog.queueing.sdk.Database
import com.awsblog.queueing.sdk.Dynamodb
import com.awsblog.queueing.utils.Utils

/**
 * Test SDK
 *
 * @author zorani
 */
object TestSDK {
    /**
     * @param args
     * Something is wrong with our partition key
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val ID = "A-101"
        val date = "11/17/2022"
        val assignment = Assignment()
        assignment.setId(ID)
        assignment.setSchedule(date)
        val client: Database? = Dynamodb.Builder().withCredentialsProfileName("default")
            .withRegion("us-east-2")
            .withLogicalTableName("assignment_schedule")
            .build()
        client?.put(assignment)
        var queueStats: QueueStats? = client?.getQueueStats()
        println(Utils.toJSON(queueStats))
        val retrievedAssignment: Assignment = client?.get("A-101") as Assignment
        println(Utils.toJSON(retrievedAssignment))
        val result: EnqueueResult = client.enqueue("A-101")
        println(Utils.toJSON(result))
        //System.out.println(result.getReturnValue());
        //System.out.println(Utils.toJSON(client.getQueueStats()));
        val enqueuedAssignment: Assignment? = result.assignment
        println(Utils.toJSON(enqueuedAssignment))
        queueStats = client.getQueueStats()
        println(Utils.toJSON(queueStats))
        val peek: PeekResult = client.peek()
        println(Utils.toJSON(peek.peekedAssignmentId))
        println(Utils.toJSON(peek.peekedAssignmentSchedule))
    }
} // end TestSDK

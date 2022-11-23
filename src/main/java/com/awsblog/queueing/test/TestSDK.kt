package com.awsblog.queueing.test

import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.sdk.QueueSdkClient
import com.awsblog.queueing.utils.Utils

/**
 * Test SDK
 *
 * @author zorani
 */
object TestSDK {
    /**
     * @param args
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val ID = "A-101"
        val date = "11/17/2022"
        val assignment = Assignment()
        assignment.setId(ID)
        assignment.setSchedule(ID)
        val client = QueueSdkClient.Builder()
            .withCredentialsProfileName("default")
            .withLogicalTableName("assignment_schedule")
            .withRegion("us-east-2").build()
        client!!.put(assignment)
        var queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
        val retrievedAssignment = client["A-101"]
        println(Utils.toJSON(retrievedAssignment))
        val result = client.enqueue("A-101")
        println(Utils.toJSON(result))
        //System.out.println(result.getReturnValue());
        //System.out.println(Utils.toJSON(client.getQueueStats()));
        val enqueuedAssignment = result.assignment
        println(Utils.toJSON(enqueuedAssignment))
        queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
        val peek = client.peek()
        println(Utils.toJSON(peek.peekedAssignmentId))
        println(Utils.toJSON(peek.peekedAssignmentSchedule))
    }
} // end TestSDK

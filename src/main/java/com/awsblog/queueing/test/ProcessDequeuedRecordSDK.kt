package com.awsblog.queueing.test

import com.awsblog.queueing.sdk.QueueSdkClient
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
        val client = QueueSdkClient.Builder()
            .withCredentialsProfileName("default")
            .withRegion("us-east-2")
            .withLogicalTableName("assignment_schedule")
            .build()!!

        var queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
        val peek = client.peek()
        println(Utils.toJSON(peek.peekedAssignmentId))
        val assignment = peek.peekedAssignmentObject
        if (Utils.checkIfNullObject(assignment)) {
            println("Nothing to peek() from the queue!")
            System.exit(1)
        }
        val ID = assignment?.id

//		ReturnResult result = client.remove(ID);
//		if (!result.isSuccessful()) {
//			System.out.println("remove() on ID [" + ID + "] has failed!");
//			System.exit(1);
//		}
        val result = client.dequeue()
        println(result!!.dequeuedAssignmentObject?.getSchedule())
        queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
    }
} // end ProcessDequeuedRecordSDK

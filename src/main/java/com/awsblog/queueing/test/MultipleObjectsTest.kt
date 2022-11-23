package com.awsblog.queueing.test

import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.sdk.QueueSdkClient
import com.awsblog.queueing.utils.Utils
import java.time.LocalDate

/**
 * Create multiple Shipment objects and store it in the database (remove previous copy, if exists)
 *
 * @author zorani
 */
object MultipleObjectsTest {
    /**
     * The main
     *
     * @param args
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // must add the LogicalTableName to the builder for this to work properly
        // FYI: previous logic required the tableName to be defined in an external resource configuration.json file
        // this logic has been updated so that we can just pass the table name instead
        val client = QueueSdkClient.Builder()
            .withCredentialsProfileName("default")
            .withRegion("us-east-2")
            .withLogicalTableName("assignment_schedule")
            .build()
        val id1 = "A-101"
        val date1 = LocalDate.now().plusDays(10)
        val assignment1 = Assignment(id1, date1.toString())
        val id2 = "A-202"
        val date2 = LocalDate.now().plusDays(2)
        val assignment2 = Assignment(id2, date2.toString())
        val id3 = "A-303"
        val date3 = LocalDate.now().plusDays(3)
        val assignment3 = Assignment(id3, date3.toString())
        client!!.put(assignment1)
        client.put(assignment2)
        client.put(assignment3)
        var queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
        //
        val a1 = client[id1]
        println("Successfully inserted " + Utils.toJSON(id1) + " into Dynamodb")
        //
        val a2 = client[id2]
        println("Successfully inserted " + Utils.toJSON(id2) + " into Dynamodb")
        //
        val a3 = client[id3]
        println("Successfully inserted " + Utils.toJSON(id3) + " into Dynamodb")

        // ----------------------------------------------------
        println("====================> Start enqueue process <=====================")
        var er = client.enqueue(id1)
        println(Utils.toJSON(er))
        queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
        er = client.enqueue(id2)
        println(Utils.toJSON(er))
        queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
        er = client.enqueue(id3)
        println(Utils.toJSON(er))
        queueStats = client.queueStats
        println(Utils.toJSON(queueStats))

        // ----------------------------------------------------
        println("====================> PEEK <=====================")
        val peek = client.peek()
        println(Utils.toJSON(peek.peekedAssignmentObject))
        queueStats = client.queueStats
        println(Utils.toJSON(queueStats))
    }
} // end MultipleObjectsTest

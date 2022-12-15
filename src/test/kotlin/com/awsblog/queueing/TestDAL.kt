package com.awsblog.queueing

import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.sdk.Database
import com.awsblog.queueing.sdk.Dynamodb
import com.awsblog.queueing.sdk.IPriorityQueue
import com.awsblog.queueing.utils.Utils
import io.quarkus.test.junit.QuarkusTest
import org.junit.Test
import java.time.LocalDate


// TODO 1.) Create proper tests for this sdk using local stacks -
//  (create helpful documentation to guide developers on how to use local_stacks)
// TODO 2.) Implement the database abstraction layer that extends IDbPriorityQueue (dynamodb extends database) - DONE ask for Review
// TODO 3.) Publish package to github allowing us to use this repo as a package from anywhere,
//  this entails adding a github workflow that does this for us.
// TODO 4.) TODO Complete lambda 1 and lambda 2
@QuarkusTest
class TestDAL {
    @Test
    fun main() {
        val client: Database? = Dynamodb.Builder().withCredentialsProfileName("default")
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

        val queueStats = client.getQueueStats()
        println(Utils.toJSON(queueStats))
        //
        val a1 = client[id1] as Assignment
        println("Successfully inserted " + Utils.toJSON(id1) + " into Dynamodb")
        //
        val a2 = client[id2] as Assignment
        println("Successfully inserted " + Utils.toJSON(id2) + " into Dynamodb")
        //
        val a3 = client[id3] as Assignment
        println("Successfully inserted " + Utils.toJSON(id3) + " into Dynamodb")
        var peek = client.peek()
        println("Empty Queue: " + Utils.toJSON(peek.peekedAssignmentObject))
        client.enqueue(a1.id)
        peek = client.peek()
        println(Utils.toJSON(peek.peekedAssignmentObject))
        val result = client.dequeue()
        println("The schedule of the dequeued Value: " + result.dequeuedAssignmentObject?.getSchedule())
        peek = client.peek()
        println("Peeking Queue: " + Utils.toJSON(peek.peekedAssignmentObject))
        client.restore(result.id)
        peek = client.peek()
        println(
            """
    restored dequeued value to queue
    ${Utils.toJSON(peek.peekedAssignmentObject)}
    """.trimIndent()
        )
        a1.id?.let { client.delete(it) };
        a2.id?.let { client.delete(it) };
        a3.id?.let { client.delete(it) };
    }
}
package com.awsblog.queueing.test

import com.awsblog.queueing.appdata.Assignment
import com.awsblog.queueing.sdk.QueueSdkClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Create a new object and store it in the database (remove previous copy, if exists)
 *
 * @author EdWrld
 */
class TestPutObject {
    /**
     * TestPutObject
     * TODO we don't have any of these tests to actually call our in-production database
     * We can use local_stacks to test this
     * TODO create helpful documentation to guide developers on how to use local_stacks
     */
    @Test
    fun Test_Put_Object() {
        val client = QueueSdkClient.Builder()
            .withCredentialsProfileName("default")
            .withRegion("us-east-2")
            .withLogicalTableName("assignment_schedule")
            .build()
        val AssignmentID = "Assignment-1-Ed5000"
        val scheduled = OffsetDateTime.now(ZoneOffset.UTC).toString()
        var assignment: Assignment? = Assignment(AssignmentID, scheduled)
        client!!.put(assignment!!)
        assignment = client[AssignmentID]
        Assertions.assertEquals(AssignmentID, assignment!!.id)
        client.delete(AssignmentID)
    }
} // end PutObject

package com.awsblog.queueing

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
     *
     */
//    @Test
//    fun Test_Put_Object() {
//        val client: IDbPriorityQueue<Dynamodb>? = Dynamodb.Builder().withCredentialsProfileName("default")
//            .withRegion("us-east-2")
//            .withLogicalTableName("assignment_schedule")
//            .build()
//        val AssignmentID = "Assignment-1-Ed5000"
//        val scheduled: String = OffsetDateTime.now(ZoneOffset.UTC).toString()
//        var assignment = Assignment(AssignmentID, scheduled)
//        client?.put(assignment)
//        assignment = client?.get(AssignmentID) as Assignment
//        Assertions.assertEquals(AssignmentID, assignment.id)
//        client.delete(AssignmentID)
//    }
} // end PutObject

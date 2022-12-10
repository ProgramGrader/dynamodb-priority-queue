package com.awsblog.queueing.test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;


import com.awsblog.queueing.appdata.Assignment;

import com.awsblog.queueing.sdk.Dynamodb;
import com.awsblog.queueing.sdk.IDbPriorityQueue;
import com.awsblog.queueing.sdk.QueueSdkClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Create a new object and store it in the database (remove previous copy, if exists)
 * 
 * @author EdWrld
 *
 */
public class TestPutObject {

	/**
	 * TestPutObject
	 * TODO we don't have any of these tests to actually call our in-production database
	 * We can use local_stacks to test this
	 * TODO create helpful documentation to guide developers on how to use local_stacks
	 */
	@Test
	void Test_Put_Object() {

		IDbPriorityQueue<Dynamodb> client = new Dynamodb.Builder().withCredentialsProfileName("default")
				.withRegion("us-east-2")
				.withLogicalTableName("assignment_schedule")
				.build();

		String AssignmentID = "Assignment-1-Ed5000";
		String scheduled =  OffsetDateTime.now(ZoneOffset.UTC).toString();
		Assignment assignment = new Assignment(AssignmentID, scheduled);
		client.put(assignment);
		assignment = (Assignment) client.get(AssignmentID);

		assertEquals(AssignmentID,assignment.getId());

		client.delete(AssignmentID);
	}

} // end PutObject
package com.awsblog.queueing.test;

import com.awsblog.queueing.appdata.Assignment;
import com.awsblog.queueing.model.EnqueueResult;
import com.awsblog.queueing.model.PeekResult;
import com.awsblog.queueing.model.QueueStats;
import com.awsblog.queueing.sdk.QueueSdkClient;
import com.awsblog.queueing.utils.Utils;

/**
 * Test SDK
 * 
 * @author zorani
 *
 */
public class TestSDK {

	/**
	 * @param args
	 * Something is wrong with our partition key
	 */
	public static void main(String[] args) {

		String ID = "A-101";
		String date = "11/17/2022";

		Assignment assignment = new Assignment();
		assignment.setId(ID);
		assignment.setSchedule(date);
		
		QueueSdkClient client = new QueueSdkClient.Builder()
									.withCredentialsProfileName("default")
									.withLogicalTableName("assignment_schedule")
									.withRegion("us-east-2").build();
		client.put(assignment);
		
		QueueStats queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));

		Assignment retrievedAssignment = client.get("A-101");
		System.out.println(Utils.toJSON(retrievedAssignment));
		
		EnqueueResult result = client.enqueue("A-101");
		System.out.println(Utils.toJSON(result));
		//System.out.println(result.getReturnValue());
		//System.out.println(Utils.toJSON(client.getQueueStats()));

	 Assignment enqueuedAssignment = result.getAssignment();
		System.out.println(Utils.toJSON(enqueuedAssignment));

		queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));
		
		PeekResult peek = client.peek();
		System.out.println(Utils.toJSON(peek.getPeekedAssignmentId()));
		System.out.println(Utils.toJSON(peek.getPeekedAssignmentSchedule()));
	}

} // end TestSDK
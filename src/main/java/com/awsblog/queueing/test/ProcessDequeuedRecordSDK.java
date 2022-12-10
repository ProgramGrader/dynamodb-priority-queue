package com.awsblog.queueing.test;

import com.awsblog.queueing.appdata.Assignment;
import com.awsblog.queueing.sdk.Dynamodb;
import com.awsblog.queueing.sdk.IDbPriorityQueue;
import com.awsblog.queueing.model.DequeueResult;
import com.awsblog.queueing.model.PeekResult;
import com.awsblog.queueing.model.QueueStats;
import com.awsblog.queueing.sdk.QueueSdkClient;
import com.awsblog.queueing.utils.Utils;

/**
 * Testing dequeuing - peek and remove
 * 
 * @author zorani
 *
 */
public class ProcessDequeuedRecordSDK {

	/**
	 * Test main 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		IDbPriorityQueue<Dynamodb> client = new Dynamodb.Builder().withCredentialsProfileName("default")
				.withRegion("us-east-2")
				.withLogicalTableName("assignment_schedule")
				.build();

		QueueStats queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));

		PeekResult peek = client.peek();
		System.out.println(Utils.toJSON(peek.getPeekedAssignmentId()));

		Assignment assignment = peek.getPeekedAssignmentObject();
		if (Utils.checkIfNullObject(assignment)) {

			System.out.println("Nothing to peek() from the queue!");
			System.exit(1);
		}

		String ID = assignment.getId();

//		ReturnResult result = client.remove(ID);
//		if (!result.isSuccessful()) {
//			System.out.println("remove() on ID [" + ID + "] has failed!");
//			System.exit(1);
//		}

		DequeueResult result = client.dequeue();
		System.out.println(result.getDequeuedAssignmentObject().getSchedule());
		queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));
	}

}// end ProcessDequeuedRecordSDK
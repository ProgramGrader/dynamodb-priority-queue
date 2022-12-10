package com.awsblog.queueing.test;

import com.awsblog.queueing.sdk.Dynamodb;
import com.awsblog.queueing.sdk.IDbPriorityQueue;
import com.awsblog.queueing.model.PeekResult;
import com.awsblog.queueing.model.QueueStats;
import com.awsblog.queueing.sdk.QueueSdkClient;
import com.awsblog.queueing.utils.Utils;

/**
 * Simple test app for peeking the record from the queue
 * 
 * @author zorani
 *
 */
public class PeekSDK {

	/**
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
		
		queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));		
	}

} // end TestSDK
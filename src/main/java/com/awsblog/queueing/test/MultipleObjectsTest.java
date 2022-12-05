package com.awsblog.queueing.test;

import java.time.LocalDate;

import com.awsblog.queueing.appdata.Assignment;
import com.awsblog.queueing.model.EnqueueResult;
import com.awsblog.queueing.model.PeekResult;
import com.awsblog.queueing.model.QueueStats;
import com.awsblog.queueing.sdk.QueueSdkClient;
import com.awsblog.queueing.utils.Utils;

/**
 * Create multiple Shipment objects and store it in the database (remove previous copy, if exists)
 * 
 * @author zorani
 *
 */
public class MultipleObjectsTest {

	/**
	 * The main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// must add the LogicalTableName to the builder for this to work properly
		// FYI: previous logic required the tableName to be defined in an external resource configuration.json file
		// this logic has been updated so that we can just pass the table name instead

		QueueSdkClient client = new QueueSdkClient.Builder()
				.withCredentialsProfileName("default")
				.withRegion("us-east-2")
				.withLogicalTableName("assignment_schedule")
				.build();
		
		String id1 = "A-101";
		LocalDate date1 = LocalDate.now().plusDays(10);

		Assignment assignment1 = new Assignment(id1, date1.toString());

		String id2 = "A-202";
		LocalDate date2 = LocalDate.now().plusDays(2);


		Assignment assignment2 = new Assignment(id2, date2.toString());

		String id3 = "A-303";
		LocalDate date3 = LocalDate.now().plusDays(3);


		Assignment assignment3 = new Assignment(id3, date3.toString());

		client.put(assignment1);
		client.put(assignment2);
		client.put(assignment3);
		QueueStats queueStats = client.getQueueStats();

		System.out.println(Utils.toJSON(queueStats));
//
		Assignment a1 = client.get(id1);
		System.out.println("Successfully inserted "+ Utils.toJSON(id1)+ " into Dynamodb");
//
		Assignment a2 = client.get(id2);
		System.out.println("Successfully inserted " + Utils.toJSON(id2) + " into Dynamodb");
//
		Assignment a3 = client.get(id3);
		System.out.println("Successfully inserted " + Utils.toJSON(id3) + " into Dynamodb");

		// ----------------------------------------------------

		System.out.println("====================> Start enqueue process <=====================");

		EnqueueResult er = client.enqueue(id1);
		System.out.println(Utils.toJSON(er));
		
		queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));		

		er = client.enqueue(id2);
		System.out.println(Utils.toJSON(er));

		queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));		

		er = client.enqueue(id3);
		System.out.println(Utils.toJSON(er));

		queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));		

		// ----------------------------------------------------

		System.out.println("====================> PEEK <=====================");
		
		PeekResult peek = client.peek();
		System.out.println(Utils.toJSON(peek.getPeekedAssignmentObject()));
		
		queueStats = client.getQueueStats();
		System.out.println(Utils.toJSON(queueStats));
	}

} // end MultipleObjectsTest
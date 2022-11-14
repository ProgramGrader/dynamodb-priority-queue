package com.awsblog.queueing.test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import com.awsblog.queueing.appdata.Assignment;
import com.awsblog.queueing.appdata.ShipmentItem;
import com.awsblog.queueing.appdata.Shipment;
import com.awsblog.queueing.appdata.ShipmentData;
import com.awsblog.queueing.model.QueueStats;
import com.awsblog.queueing.sdk.QueueSdkClient;
import com.awsblog.queueing.utils.Utils;

/**
 * Create a new object and store it in the database (remove previous copy, if exists)
 * 
 * @author EdWrld
 *
 */
public class PutObject {

	/**
	 * The main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

//		String ID = "A-101";
//		ShipmentData data = new ShipmentData(ID);
//		data.setData1("Data 1"); data.setData2("Data 2"); data.setData3("Data 3");
//		data.setItems(Arrays.asList(new ShipmentItem("Item-1", true), new ShipmentItem("Item-2", false)));
//
//		Shipment shipment = new Shipment(ID);
//		//shipment.setId(ID);
//		shipment.setData(data);
//
//		QueueSdkClient client = new QueueSdkClient.Builder()
//									.withCredentialsProfileName("default")
//									.withRegion("us-east-2")
//									.build();
//		client.put(shipment);
//
//		QueueStats queueStats = client.getQueueStats();
//		System.out.println(Utils.toJSON(queueStats));
//
//		Shipment retrievedShipment = client.get("A-101");
//		System.out.println(Utils.toJSON(retrievedShipment));
//
		QueueSdkClient client = new QueueSdkClient.Builder()
									.withCredentialsProfileName("default")
									.withRegion("us-east-2")
									.withLogicalTableName("assignment_schedule")
									.build();

		String AssignmentID = "Assignment-1-Ed5000";
		String scheduled =  OffsetDateTime.now(ZoneOffset.UTC).toString();
		Assignment assignment = new Assignment(AssignmentID, scheduled);
		client.put(assignment);

	}

} // end PutObject
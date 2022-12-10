package com.awsblog.queueing.test;
import com.awsblog.queueing.appdata.Assignment;

import com.awsblog.queueing.sdk.Dynamodb;
import com.awsblog.queueing.sdk.IDbPriorityQueue;
import com.awsblog.queueing.model.DequeueResult;
import com.awsblog.queueing.model.PeekResult;
import com.awsblog.queueing.model.QueueStats;
import com.awsblog.queueing.utils.Utils;

import java.time.LocalDate;

public class TestDAL {

    public static void main(String[] args) {
        IDbPriorityQueue<Dynamodb> client = new Dynamodb.Builder().withCredentialsProfileName("default")
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
        Assignment a1 = (Assignment) client.get(id1);
        System.out.println("Successfully inserted "+ Utils.toJSON(id1)+ " into Dynamodb");
//
        Assignment a2 = (Assignment) client.get(id2);
        System.out.println("Successfully inserted " + Utils.toJSON(id2) + " into Dynamodb");
//
        Assignment a3 = (Assignment) client.get(id3);
        System.out.println("Successfully inserted " + Utils.toJSON(id3) + " into Dynamodb");

        PeekResult peek = client.peek();
        System.out.println("Empty Queue: "+Utils.toJSON(peek.getPeekedAssignmentObject()));

        client.enqueue(a1.getId());
        peek = client.peek();
        System.out.println(Utils.toJSON(peek.getPeekedAssignmentObject()));

        DequeueResult result = client.dequeue();
        System.out.println("The schedule of the dequeued Value: "+ result.getDequeuedAssignmentObject().getSchedule());
        peek = client.peek();
        System.out.println("Peeking Queue: "+ Utils.toJSON(peek.getPeekedAssignmentObject()));

        client.restore(result.getId());
        peek = client.peek();
        System.out.println("restored dequeued value to queue\n" + Utils.toJSON(peek.getPeekedAssignmentObject()));
//        client.delete(a1.getId());
//        client.delete(a2.getId());
//        client.delete(a3.getId());
    }
}

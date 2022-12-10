package com.awsblog.queueing.sdk;

import com.awsblog.queueing.model.*;

public interface IDbPriorityQueue<Database> {

    Object get(String id);
    Database initialize();
    void put(Object item);
    ReturnResult remove(String id);
    ReturnResult restore(String id);
    void delete(String id);
    PeekResult peek();
    EnqueueResult enqueue(String id);
    DequeueResult dequeue();
    QueueStats getQueueStats();



}

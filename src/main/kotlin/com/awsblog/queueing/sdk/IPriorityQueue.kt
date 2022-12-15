package com.awsblog.queueing.sdk

import com.awsblog.queueing.model.*

// TODO change:
interface IPriorityQueue {

    // Removes queue given Assignment id
    fun remove(id: String?): ReturnResult

    // Restores Assignment to queue given Assignment id
    fun restore(id: String?): ReturnResult

    // Peeks the top of the queue
    fun peek(): PeekResult

    // Inserts Assigment to top of queue given Assignment id
    fun enqueue(id: String?): EnqueueResult

    // Removes Assignment from top of queue given Assignment id
    fun dequeue(): DequeueResult

    // Creates a object containing Queue content
    fun getQueueStats() :QueueStats

}
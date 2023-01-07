package com.awsblog.queueing.sdk
import com.awsblog.queueing.appdata.DatabaseItem
import com.awsblog.queueing.model.*



interface IPriorityQueue {

    /**
     * Removes item from queue given item id, item is deleted from database
     * @param id
     * @return base object containing values
     *
     */
    fun remove(id: String?): ReturnResult

    /**
     * Restores item to queue given item id
     * @param id
     * @return base object containing values
     */
    fun restore(id: String?): ReturnResult


    /**
     * gets item from queue given item id
     * @param id
     * @return base object containing values
     */
    fun retrieve(id: String?): ReturnResult

    /**
     * Peeks the top of the queue
     * @return Object containing result for the peek() API call
     *
     */
    fun peek(n : Int): List<DatabaseItem>

    /**
     *  Inserts item to top of queue given item id, If item already exists in table inserts
     *  into queue else inserts into database then queue
     *  @param id
     *  @return Object containing result for the enqueue() API call
     */
    fun enqueue(item: DatabaseItem?): ReturnResult

    /**
     * Deletes top n items from queue and deletes it from the database
     * @return List of top n items in priority queue DatabaseItems
     */
    fun dequeue(n:Int) :  List<DatabaseItem>

    /**
     * Creates a object containing Queue content
     * @return Object with various queue depth statistics
     */
    fun getQueueStats() :QueueStats

}
package bbs.priorityqueue.sdk

import bbs.priorityqueue.appdata.PriorityQueueElement

interface Database : IPriorityQueue {

    /**
     * Gets item from table in Database
     * @param id Identifier for item to get from table
     * @return Item if found, else null
     */
    operator fun get(id: String?) : PriorityQueueElement?

    /**
     * Places item into table in Database, if item exists deletes and inserts the new fresh version
     * @param id Identifier for item to get from table
     */
    fun put(item: PriorityQueueElement)

    /**
     * Removes item from table in Database
     * @param id Identifier for item to get from table
     */
    fun delete(id: String)

    /**
     * Initializes Database
     */
    fun initialize() : Database

}
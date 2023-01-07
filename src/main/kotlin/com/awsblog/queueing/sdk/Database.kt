package com.awsblog.queueing.sdk

import com.awsblog.queueing.appdata.DatabaseItem

interface Database : IPriorityQueue{

    /**
     * Gets item from table in Database
     * @param id Identifier for item to get from table
     * @return Item if found, else null
     */
    operator fun get(id: String?) : DatabaseItem?

    /**
     * Places item into table in Database, if item exists deletes and inserts the new fresh version
     * @param id Identifier for item to get from table
     */
    fun put(item: DatabaseItem)

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
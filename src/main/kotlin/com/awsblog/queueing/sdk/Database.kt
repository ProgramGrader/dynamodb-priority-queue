package com.awsblog.queueing.sdk

interface Database : IPriorityQueue{
    operator fun get(id: String?): Any
    fun put(item: Any)
    fun delete(id: String)
    fun initialize() : Database

}
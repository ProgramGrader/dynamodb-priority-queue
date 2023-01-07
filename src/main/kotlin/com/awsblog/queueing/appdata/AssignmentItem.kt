package com.awsblog.queueing.appdata


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter

import com.awsblog.queueing.utils.Utils
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * This object is created to represent the items in the assignment_schedule table
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBDocument
class AssignmentItem(){



    constructor(id: String, dueDate: String?) : this() {
        Utils.throwIfNullOrEmptyString(id, "ID cannot be null!")
        this.id = id
        if (dueDate != null) {
            this.dueDate = dueDate
        }
    }

    // ---------------------- fields

    @JsonProperty("id")
    var id :String? = null

    @JsonProperty("dueDate")
    var dueDate: String? = null
}

// could implement this in Database Item for data attribute dev would then need to
// extend it and reimplement convert and unconvert


//class AssignmentItemConverter :DynamoDBTypeConverter<String, AssignmentItem>{
//    override fun convert(`object`: AssignmentItem?): String? {
//        val item : AssignmentItem? = `object`
//        var assignment : String? = null
//        try {
//            if(item != null) {
//                assignment = String().format("due_date: %s, id: %s", item.dueDate, item.id)
//            }
//        }
//        catch (e : Exception){
//            e.printStackTrace()
//        }
//        return  assignment
//    }
//
//    override fun unconvert(s: String?): AssignmentItem? {
//        var item : AssignmentItem? = AssignmentItem()
//        try {
//            if(!s.isNullOrEmpty()){
//
//            }
//        }
//        catch (e : Exception){
//            e.printStackTrace()
//        }
//        return item
//    }
//
//}


/**
 * Not seeing a way to make this generic it seems like objects that are to be inserted into Dynamodb need to
 * define the attributes and indexes that are to be inserted therefore there is no getting around
 * the developer creating an object in Appdata
 */
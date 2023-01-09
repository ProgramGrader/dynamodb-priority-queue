package com.awsblog.queueing.appdata

import com.amazonaws.services.dynamodbv2.datamodeling.*
import com.awsblog.queueing.utils.Utils
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBDocument
 class DatabaseItemData(){

    @DynamoDBAttribute(attributeName="item")
    @JsonProperty("item")

    var item: Any? = null


    /**
     * constructor uses Dynamodb type convertor object to convert any data inputted into items
     * into a databaseItemData
     */
//    constructor(c: DynamoDBTypeConverter<Any, Any>) : this() {
//        this.item = c.convert(this.item)
//    }
}
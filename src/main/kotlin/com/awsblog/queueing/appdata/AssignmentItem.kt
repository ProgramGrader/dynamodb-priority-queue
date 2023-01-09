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
class AssignmentItem() : DynamoDBTypeConverter<DatabaseItemData, AssignmentItem> {
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
    override fun convert(`object`: AssignmentItem?): DatabaseItemData {
        TODO("Not yet implemented")
    }

    override fun unconvert(`object`: DatabaseItemData?): AssignmentItem {
        TODO("Not yet implemented")
    }
}

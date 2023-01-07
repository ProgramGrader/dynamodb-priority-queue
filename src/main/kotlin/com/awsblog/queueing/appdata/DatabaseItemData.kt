package com.awsblog.queueing.appdata

import com.amazonaws.services.dynamodbv2.datamodeling.*
import com.awsblog.queueing.utils.Utils
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBDocument
 class DatabaseItemData() : Serializable {


    //@DynamoDBTypeConverted("")
    @DynamoDBAttribute(attributeName="items")
    @JsonProperty("items")
    // instead of it being item maybe this should just accept Any in general
    // supporting multiple items in a list is cool but not very useful for us
    var items: Any? = null


//    fun getItems() : List<Any>?{
//        return this.items
//    }
//
//    fun setItems(items:List<Any>?) {
//         this.items = items
//    }+

    class AssignmentItemConverter : DynamoDBTypeConverter<String, AssignmentItem> {
        override fun convert(`object`: AssignmentItem?): String? {
            val item: AssignmentItem? = `object`
            var assignment: String? = null
            try {
                if (item != null) {
                    assignment = String().format(" %s,%s", item.id, item.dueDate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return assignment
        }

        override fun unconvert(s: String?): AssignmentItem {
            val item = AssignmentItem()
            try {
                if (!s.isNullOrEmpty()) run {
                    val data = s.split(",")
                    item.id = data[0].trim()
                    item.dueDate = data[1].trim()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return item
        }
    }
}
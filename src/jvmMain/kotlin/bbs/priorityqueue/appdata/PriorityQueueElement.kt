package bbs.priorityqueue.appdata

import com.amazonaws.services.dynamodbv2.datamodeling.*
import bbs.priorityqueue.model.SystemInfo
import bbs.priorityqueue.utils.Utils
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.util.logging.Logger

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBTable(tableName = "priority_queue_table")
class PriorityQueueElement {
    companion object {
        val LOG = Logger.getLogger(PriorityQueueElement::class.java.name)
    }
    constructor()
    constructor(id: String?) {
        this.id = id
    }

    constructor(id: String, date: LocalDate?) {
        Utils.throwIfNullOrEmptyString(id, "ID cannot be null!")
        this.id = id.trim { it <= ' ' }
        systemInfo = SystemInfo(this.id)
        this.schedule = date.toString()
    }


    /**
     * Hash key for the priority queue
     */
    @get:DynamoDBAttribute(attributeName = "id")
    @get:DynamoDBHashKey(attributeName = "id")
    @JsonProperty("id")
    var id: String? = null

    /**
     * item to store GSI range key values
     */

    @JsonProperty("schedule")
    private var schedule: String? = null

    @DynamoDBAttribute(attributeName = "schedule")
    fun getSchedule(): String?{
        return this.schedule
    }

    fun setSchedule(date: String?) {
        this.schedule = date
    }


    @get:DynamoDBAttribute(attributeName = "system_info")
    @JsonProperty("system_info")
    var systemInfo: SystemInfo? = null


    @get:DynamoDBAttribute(attributeName = "data")
    @JsonProperty("data")
    var data: String? = null

    fun convertDateToIso(){

        var isIso = true

        try {
            java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(this.schedule);
        }catch (e: java.time.DateTimeException){
            isIso = false
        }

        if(!isIso){
            this.schedule=DateTime().withDate(LocalDate.parse(this.schedule)).toDateTimeISO().toString()
        }else{
            LOG.info( "Date is already of iso_format")
        }

    }


}

package bbs.priorityqueue.appdata

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*
import bbs.priorityqueue.model.SystemInfo
import bbs.priorityqueue.utils.Utils
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime
import org.joda.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDbBean
class PriorityQueueElement() {

    init {

    }

    constructor(id: String?) : this() {
        this.id = id
    }

    constructor(id: String, date: LocalDate?) : this() {
        Utils.throwIfNullOrEmptyString(id, "ID cannot be null!")
        this.id = id.trim { it <= ' ' }
        systemInfo = SystemInfo(this.id)
        this.schedule = date.toString()
    }


    /**
     * Hash key for the priority queue
     */
    @get:DynamoDbAttribute("id")
    @get:DynamoDbPartitionKey()
    @JsonProperty("id")
    var id: String? = null

    /**
     * item to store GSI range key values
     */

    @JsonProperty("schedule")
    private var schedule: String? = null

    @DynamoDbAttribute("schedule")
    fun getSchedule(): String?{
        return this.schedule
    }

    fun setSchedule(date: String?) {
        this.schedule = date
    }


    @get:DynamoDbAttribute("system_info")
    @JsonProperty("system_info")
    var systemInfo: SystemInfo? = null


    @get:DynamoDbAttribute("data")
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
        }
    }

}

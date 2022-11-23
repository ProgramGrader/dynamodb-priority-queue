package com.awsblog.queueing.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Return value base object
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class ReturnResult {
    constructor() {}

    /**
     * C-tor
     *
     * @param id
     */
    constructor(id: String?) {
        this.id = id
    }

    /**
     * Was it successful?
     *
     * @return
     */
    val isSuccessful: Boolean
        get() = returnValue == ReturnStatusEnum.SUCCESS

    /**
     * Get the error message
     * @return
     */
    val errorMessage: String
        get() = returnValue.errorMessage
    //	public String getSchedule() {return schedule; }
    //	public void setSchedule(String schedule) {this.schedule = schedule;}
    /**
     * @return the status
     */
    //	public StatusEnum getStatus() {
    //		return status;
    //	}
    /**
     * @param status the status to set
     */
    //	public void setStatus(StatusEnum status) {
    //		this.status = status;
    //	}
    /**
     * @return the Id
     */
    /**
     * @param id the id to set
     */
    // ---------------- fields
    @JsonProperty("id")
    var id: String? = null

    /**
     * @return the status
     */
    @JsonProperty("return_value")
    var returnValue = ReturnStatusEnum.NONE
    /**
     * @return the lastUpdatedTimestamp
     */
    /**
     * @param lastUpdatedTimestamp the lastUpdatedTimestamp to set
     */
    //	@JsonProperty("status")
    //	private StatusEnum status = StatusEnum.NONE;
    @JsonProperty("last_updated_timestamp")
    var lastUpdatedTimestamp: String? = null

    @JsonProperty("schedule")
    private val schedule: String? = null
    /**
     * @return the version
     */
    /**
     * @param version the version to set
     */
    @JsonProperty("version")
    var version = 0
} // end ReturnResult

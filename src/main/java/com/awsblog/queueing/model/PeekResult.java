package com.awsblog.queueing.model;

import com.awsblog.queueing.appdata.Assignment;
import com.awsblog.queueing.utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Result for the peek() API call
 * 
 * @author zorani
 *
 *
 * Need to update this to reflect returning schedules
 */
@JsonInclude(Include.NON_NULL)
public class PeekResult extends ReturnResult {

	/**
	 * Default empty c-tor
	 */
	public PeekResult() {
		
		super();
	}

	/**
	 * C-tor
	 */
	public PeekResult(String id) {
		
		super(id);
	}
	
	/**
	 * @return the peekedShipmentObject
	 */
	public Assignment getPeekedAssignmentObject() {
		return peekedAssignmentObject;
	}

	/**
	 * @param peekedAssignmentObject the peekedShipmentObject to set
	 */
	public void setPeekedAssignmentObject(Assignment peekedAssignmentObject) {
		this.peekedAssignmentObject = peekedAssignmentObject;
	}	
	
	/**
	 * @return the timestampMillisUTC
	 */
	public long getTimestampMillisUTC() {
		return timestampMillisUTC;
	}

	/**
	 * @param timestampMillisUTC the timestampMillisUTC to set
	 */
	public void setTimestampMillisUTC(long timestampMillisUTC) {
		this.timestampMillisUTC = timestampMillisUTC;
	}

	/**
	 * Get the peeked shipment id
	 * 
	 * @return
	 */
	@JsonProperty("peeked_id")
	public String getPeekedAssignmentId() {
		
		if (Utils.checkIfNotNullObject(this.peekedAssignmentObject))
			return this.peekedAssignmentObject.getId();
		
		return "NOT FOUND";
	}

	@JsonProperty("peeked_schedule")
	public String getPeekedAssignmentSchedule(){
		if (Utils.checkIfNotNullObject(this.peekedAssignmentObject))
			return this.getPeekedAssignmentObject().getSchedule();

		return "NOT FOUND";
	}

	// ---------------- fields

	@JsonProperty("timestamp_milliseconds_utc")
	private long timestampMillisUTC = 0L;
	
	@JsonIgnore
	private Assignment peekedAssignmentObject = null;
	
} // end PeekResult
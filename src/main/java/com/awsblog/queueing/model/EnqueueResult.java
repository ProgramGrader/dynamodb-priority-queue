package com.awsblog.queueing.model;

import com.awsblog.queueing.appdata.Assignment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Result for the enqueue() API call
 * 
 * @author zorani
 *
 */
@JsonInclude(Include.NON_NULL)
public class EnqueueResult extends ReturnResult {

	/**
	 * Default empty c-tor
	 */
	public EnqueueResult() {
		
		super();
	}

	/**
	 * C-tor
	 */
	public EnqueueResult(String id) {
		
		super(id);
	}
	
	/**
	 * @return the shipment
	 */
	public Assignment getAssignment() {
		return assignment;
	}

	/**
	 * @param assignment the shipment to set
	 */
	public void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}

	// ---------------- fields

	@JsonIgnore
	private Assignment assignment = null;
	
} // end EnqueueResult
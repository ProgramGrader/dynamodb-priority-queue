package com.awsblog.queueing;

/**
 * Keep the project constants
 * 
 * @author zorani
 *
 */
public class Constants {


	private Constants() {}
	
	// ------- constants
	
	public static final String AWS_REGION_DEFAULT = "us-east-2";
	public static final String AWS_PROFILE_DEFAULT= "default";
	public static final String CONFIGURATION_FILE_NAME = "configuration.json";
	public static final String QUEUEING_INDEX_NAME = "scheduled-index";
	public static final String DLQ_QUEUEING_INDEX_NAME = "dlq-last_updated_timestamp-index";
	
	public static final int VISIBILITY_TIMEOUT_IN_MINUTES = 1;
	
} // end Constants
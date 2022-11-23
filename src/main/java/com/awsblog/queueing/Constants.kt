package com.awsblog.queueing

/**
 * Keep the project constants
 *
 * @author zorani
 */
object Constants {
    // ------- constants
    const val AWS_REGION_DEFAULT = "us-east-2"
    const val AWS_PROFILE_DEFAULT = "default"
    const val CONFIGURATION_FILE_NAME = "configuration.json"
    const val QUEUEING_INDEX_NAME = "scheduled-index"
    const val DLQ_QUEUEING_INDEX_NAME = "dlq-last_updated_timestamp-index"
    const val VISIBILITY_TIMEOUT_IN_MINUTES = 1
} // end Constants

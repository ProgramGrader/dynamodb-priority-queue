package com.awsblog.queueing.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Queue operations return status
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
enum class ReturnStatusEnum {
    NONE, SUCCESS, FAILED_ID_NOT_PROVIDED, FAILED_ID_NOT_FOUND, FAILED_RECORD_NOT_CONSTRUCTED, FAILED_ON_CONDITION, FAILED_EMPTY_QUEUE, FAILED_ILLEGAL_STATE, FAILED_DYNAMO_ERROR;

    val errorMessage: String
        /**
         * Get the error message
         *
         * @return
         */
        get() {
            if (this == SUCCESS) return "No error" else if (this == FAILED_ID_NOT_PROVIDED) return "ID was not provided!" else if (this == FAILED_ID_NOT_FOUND) return "Provided Shipment ID was not found in the Dynamo DB!" else if (this == FAILED_RECORD_NOT_CONSTRUCTED) return "Shipment record not yet fully constructed .. cannot execute API!" else if (this == FAILED_ON_CONDITION) return "Condition on the 'version' attribute has failed!" else if (this == FAILED_EMPTY_QUEUE) return "Cannot proceed, queue is empty!" else if (this == FAILED_ILLEGAL_STATE) return "Illegal state, cannot proceed!" else if (this == FAILED_DYNAMO_ERROR) return "Unspecified DynamoDB error is encountered!"
            return "API was not called!"
        }
} // end ReturnStatusEnum

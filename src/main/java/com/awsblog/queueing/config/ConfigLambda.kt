package com.awsblog.queueing.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Lambda configuration
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConfigLambda
/**
 * C-tor
 */
{
    /**
     * @return the logicalName
     */
    /**
     * @param logicalName the logicalName to set
     */
    // ------------ fields
    @JsonProperty("logical_name")
    var logicalName: String? = null
    /**
     * @return the className
     */
    /**
     * @param className the className to set
     */
    @JsonProperty("lambda_class")
    var className: String? = null
    /**
     * @return the lambdaDeploymentName
     */
    /**
     * @param lambdaDeploymentName the lambdaDeploymentName to set
     */
    @JsonProperty("lambda_name")
    var lambdaDeploymentName: String? = null
    /**
     * @return the runtime
     */
    /**
     * @param runtime the runtime to set
     */
    @JsonProperty("runtime")
    var runtime: String? = null
    /**
     * @return the handler
     */
    /**
     * @param handler the handler to set
     */
    @JsonProperty("handler")
    var handler: String? = null
    /**
     * @return the localJarPath
     */
    /**
     * @param localJarPath the localJarPath to set
     */
    @JsonProperty("local_jar")
    var localJarPath: String? = null
    /**
     * @return the description
     */
    /**
     * @param description the description to set
     */
    @JsonProperty("description")
    var description: String? = null
    /**
     * @return the memoryInMegabytes
     */
    /**
     * @param memoryInMegabytes the memoryInMegabytes to set
     */
    @JsonProperty("memory_megabytes")
    var memoryInMegabytes = 0
    /**
     * @return the timeoutInSeconds
     */
    /**
     * @param timeoutInSeconds the timeoutInSeconds to set
     */
    @JsonProperty("timeout_seconds")
    var timeoutInSeconds = 0
} // end ConfigLambda

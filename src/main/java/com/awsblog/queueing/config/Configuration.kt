package com.awsblog.queueing.config

import com.awsblog.queueing.utils.Utils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.util.stream.Collectors

/**
 * Parent Configuration POJO
 *
 * @author zorani
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Configuration {
    /**
     * Load the Configuration from JSON definition
     *
     * @return
     */
    //	public static Configuration loadConfiguration() {
    //		
    //		String jsonPayload = FileUtils.getFileFromResourcesAsString(Constants.CONFIGURATION_FILE_NAME);
    //		
    //		Utils.throwIfNullOrEmptyString(jsonPayload, "configuration.json is not found!");
    //		
    //		return Configuration.fromJSON(jsonPayload);
    //	}
    /**
     * Get the logical lambda names
     *
     * @return
     */
    val logicalLambdaNames: List<String?>
        get() = lambdas!!.stream()
            .map { x: ConfigLambda -> x.logicalName }
            .collect(Collectors.toList())

    /**
     * Get the deployment lambda names
     *
     * @return
     */
    val deploymentLambdaNames: List<String?>
        get() = lambdas!!.stream()
            .map { x: ConfigLambda -> x.lambdaDeploymentName }
            .collect(Collectors.toList())

    /**
     * Retrieve the DynamoDB configuration info by provided domain object name
     *
     * @param domainObjectName
     * @return ConfigTableInfo
     */
    fun getConfigTableInfo(logicalTableName: String): ConfigTable? {
        Utils.throwIfNullOrEmptyString(logicalTableName, "Missing domain object name!")
        return tablesMap!![logicalTableName.trim { it <= ' ' }]
    }

    /**
     * Get configuration lambda info
     * Lambda's full Java class name has to be provided.
     *
     * @param lambdaClassName
     * @return ConfigLambdaInfo
     */
    fun getConfigLambdaInfoByLogicalName(lambdaLogicalName: String): ConfigLambda? {
        Utils.throwIfNullOrEmptyString(lambdaLogicalName, "Missing Lambda logical name!")
        return lambdasMap!![lambdaLogicalName.trim { it <= ' ' }]
    }

    /**
     * Get the Lambda config info by the deployment name
     *
     * @param lambdaName
     * @return
     */
    fun getConfigLambdaInfoByDeploymentName(lambdaDeploymentName: String?): ConfigLambda? {
        Utils.throwIfNullOrEmptyString(lambdaDeploymentName, "Missing Lambda deployment name!")
        for (cli in lambdas!!) {
            if (cli.lambdaDeploymentName.equals(lambdaDeploymentName, ignoreCase = true)) return cli
        }
        return null
    }
    /**
     * @return the version
     */
    /**
     * @param version the version to set
     */
    // ------------ fields
    @JsonProperty("version")
    var version: String? = null
    /**
     * @return the lastUpdatedDate
     */
    /**
     * @param lastUpdatedDate the lastUpdatedDate to set
     */
    @JsonProperty("last_updated_date")
    var lastUpdatedDate: String? = null
    /**
     * @return the tables
     */
    /**
     * @param tables the tables to set
     */
    @JsonProperty("tables")
    var tables: List<ConfigTable>? = null

    /**
     * @return the tablesMap
     */
    @JsonIgnore
    var tablesMap: MutableMap<String?, ConfigTable>? = null

    /**
     * @return the lambdasMap
     */
    @JsonIgnore
    var lambdasMap: MutableMap<String?, ConfigLambda>? = null
    /**
     * @return the s3CodeBucket
     */
    /**
     * @param s3CodeBucket the s3CodeBucket to set
     */
    @JsonProperty("s3_code_bucket")
    var s3CodeBucket: String? = null
    /**
     * @return the lambdas
     */
    /**
     * @param lambdas the lambdas to set
     */
    @JsonProperty("lambdas")
    var lambdas: List<ConfigLambda>? = null

    /**
     * C-tor
     */
    init {
        tables = ArrayList()
        tablesMap = HashMap()
        lambdasMap = HashMap()
        lambdas = ArrayList()
    }

    companion object {
        /**
         * Create a Configuration object from JSON
         *
         * @param jsonPayload
         * @return
         */
        fun fromJSON(jsonPayload: String?): Configuration? {
            val mapper = ObjectMapper()

            //JSON string to Java Object
            var configurationObject: Configuration? = null
            try {
                configurationObject = mapper.readValue(jsonPayload, Configuration::class.java)
                for (tbl in configurationObject.tables!!) {
                    configurationObject.tablesMap!![tbl.logicalName] = tbl
                }
                for (lamnda in configurationObject.lambdas!!) {
                    configurationObject.lambdasMap!![lamnda.logicalName] = lamnda
                }
            } catch (e: JsonParseException) {
                e.printStackTrace()
            } catch (e: JsonMappingException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return configurationObject
        }
    }
} // end Configuration

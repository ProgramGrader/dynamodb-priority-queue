package com.awsblog.queueing.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.UnsupportedEncodingException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Bunch of utility methods
 *
 * @author zorani
 */
object Utils {
    /**
     * Convert an Object to JSON (Object should be properly annotated)
     */
    fun toJSON(obj: Any?): String? {
        if (checkIfNullObject(obj)) return null
        var jsonInString: String? = null
        try {
            val mapper = ObjectMapper()
            jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
            throwIfConditionIsTrue(true, e.message)
        }
        return jsonInString
    }

    /**
     * Compare the values
     *
     * @param val1
     * @param val2
     * @param percentAllowed
     * @return
     */
    fun compareValues(val1: Double, val2: Double, percentAllowed: Double): Boolean {
        if (val1 == val2) return true
        if (val1 < 0.000001 && val2 < 0.000001) return true
        val diff = val1 - val2
        val percent = diff / Math.max(val1, val2) * 100
        return percent <= percentAllowed
    }

    /**
     * Check if NULL or EMPTY string
     *
     * @param strToValidate
     * @return
     */
    fun nullOrEmpty(strToValidate: String?): Boolean {
        return strToValidate.isNullOrEmpty()
    }

    /**
     * Check if the string is Null or Empty
     *
     * @param strToValidate
     * @return
     */
    fun checkIfNullOrEmptyString(strToValidate: String?): Boolean {
        return nullOrEmpty(strToValidate)
    }

    /**
     * Check if the object is NULL
     *
     * @param o
     * @return
     */
    fun checkIfNullObject(o: Any?): Boolean {
        return o == null
    }

    /**
     * Check if the object is NOT NULL
     *
     * @param o
     * @return
     */
    fun checkIfNotNullObject(o: Any?): Boolean {
        return o != null
    }

    /**
     * Throw exception if the object is NULL
     *
     * @param o
     * @param exceptionMessage
     */
    fun throwIfNullObject(o: Any?, exceptionMessage: String?) {
        requireNotNull(o) { exceptionMessage!! }
    }

    /**
     * Throw exception if the Collection is NULL or Empty
     *
     * @param c
     * @param exceptionMessage
     */
    fun throwIfNullOrEmptyCollection(c: Collection<*>?, exceptionMessage: String?) {
        require(!(c == null || c.isEmpty())) { exceptionMessage!! }
    }

    /**
     * Throw exception if object is NOT NULL
     *
     * @param o
     * @param exceptionMessage
     */
    fun throwIfNotNullObject(o: Any?, exceptionMessage: String?) {
        require(o == null) { exceptionMessage!! }
    }

    /**
     * Check if not NULL and not Empty string
     *
     * @param strToValidate
     * @return
     */
    fun checkIfNotNullAndNotEmptyString(strToValidate: String?): Boolean {
        return !strToValidate.isNullOrEmpty()
    }

    /**
     * Throw exception if this an empty String
     *
     * @param strToValidate
     * @param exceptionMessage
     */
    fun throwIfNullOrEmptyString(strToValidate: String?, exceptionMessage: String?) {
        require(!(strToValidate == null || strToValidate.isEmpty())) { exceptionMessage!! }
    }

    /**
     * Throw exception if the passed condition is TRUE
     *
     * @param condition
     * @param exceptionMessage
     */
    fun throwIfConditionIsTrue(condition: Boolean, exceptionMessage: String?) {
        require(!condition) { exceptionMessage!! }
    }

    /**
     * Throw exception if the passed condition is FALSE
     *
     * @param condition
     * @param exceptionMessage
     */
    fun throwIfConditionIsFalse(condition: Boolean, exceptionMessage: String?) {
        require(condition) { exceptionMessage!! }
    }

    /**
     * Assert on the value
     *
     * @param condition
     * @param exceptionMessage
     */
    fun assertValue(condition: Boolean, exceptionMessage: String?) {
        require(condition) { exceptionMessage!! }
    }

    /**
     * Check if collection exists and it is not empty
     * @param c
     * @return
     */
    fun checkIfNotNullAndNotEmptyCollection(c: Collection<*>?): Boolean {
        return c != null && !c.isEmpty()
    }

    /**
     * Check if collection is NULL or empty
     *
     * @param c
     * @return
     */
    fun checkIfNullOrEmptyCollection(c: Collection<*>?): Boolean {
        return c == null || c.isEmpty()
    }

    /**
     * Print bytes
     *
     * @param totalBytes
     * @param s
     */
    fun printBytes(totalBytes: Int, s: String) {
        val bytes = s.toByteArray()
        for (i in 0 until totalBytes) {
            System.out.printf("%d ", bytes[i])
        }
        System.out.printf("%n")
    }

    /**
     * Print bytes
     *
     * @param totalBytes
     * @param s
     */
    fun printBytes(s: String) {
        val bytes = s.toByteArray()
        for (i in bytes.indices) {
            System.out.printf("%d ", bytes[i])
        }
        System.out.printf("%n")
    }

    /**
     * Print raw bytes as String
     *
     * @param s
     * @return
     */
    fun rawBytesAsString(s: String): String {
        val bytes = s.toByteArray()
        val sb = StringBuilder()
        for (i in bytes.indices) {
            sb.append(String.format("%d ", bytes[i]))
        }
        return sb.toString().trim { it <= ' ' }
    }

    /**
     * Get the UTC time as LocalDateTime
     *
     * @return
     */
    val localDateTimeInUTC: LocalDateTime
        get() {
            val nowUTC = ZonedDateTime.now(ZoneOffset.UTC)
            return nowUTC.toLocalDateTime()
        }

    /**
     * Sleep (parameter - sleep time in milliseconds)
     *
     * @param seconds
     */
    fun sleep(milliseconds: Long) {
        try {
            Thread.sleep(milliseconds)
        } catch (e: InterruptedException) {

            //e.printStackTrace();
            println("WARNING: Sleep() is interrupted!")
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Sleep (parameter - sleep time in seconds)
     *
     * @param seconds
     */
    fun sleepInSeconds(seconds: Int) {
        try {
            Thread.sleep(seconds * 1000L)
        } catch (e: InterruptedException) {
            //e.printStackTrace();
            println("WARNING: Sleep() is interrupted!")
            Thread.currentThread().interrupt()
        }
    }
} // end Utils

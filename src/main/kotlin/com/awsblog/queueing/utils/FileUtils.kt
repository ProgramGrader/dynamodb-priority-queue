package com.awsblog.queueing.utils

import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * File and resource based utilities
 *
 * @author zorani
 */
object FileUtils {
    /**
     * Check if the file exists
     *
     * @param filePath
     * @return
     */
    fun doesFileExists(filePath: String?): Boolean {
        if (Utils.checkIfNullOrEmptyString(filePath)) return false
        val f = File(filePath)
        return f.exists()
    }

    /**
     * Retrieve file content as a String with provided file name
     *
     * @param fileName file name for the file content to be retrieved from
     *
     * @return content of the file returned as a String
     */
    fun getFileContentAsString(fileName: String?): String? {
        var buf: ByteArrayOutputStream? = null
        try {
            BufferedInputStream(FileInputStream(fileName)).use { bis ->
                buf = ByteArrayOutputStream()
                var result = bis.read()
                while (result != -1) {
                    buf!!.write(result.toByte().toInt())
                    result = bis.read()
                }
                buf!!.close()

                // StandardCharsets.UTF_8.name() > JDK 7
                return buf!!.toString(StandardCharsets.UTF_8.name())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Retrieve file content as a String with provided file name
     *
     * @param fileName file name for the file content to be retrieved from
     *
     * @return content of the file returned as a String
     */
    fun getFileContentAsByteArray(fileName: String?): ByteArray {
        var buf: ByteArrayOutputStream? = null
        try {
            BufferedInputStream(FileInputStream(fileName)).use { bis ->
                buf = ByteArrayOutputStream()
                var result = bis.read()
                while (result != -1) {
                    buf!!.write(result.toByte().toInt())
                    result = bis.read()
                }
                buf!!.close()
                return buf!!.toByteArray()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ByteArray(0)
    }

    /**
     * Retrieve the resource content as a String with provided resource name
     *
     * @param resourceFileName resource file name for the file content to be retrieved from
     *
     * @return content of the file returned as a String
     */
    fun getFileFromResourcesAsString(resourceFileName: String?): String? {

        // try two methods to get the file from the resource folder
        var stream = FileUtils::class.java.getResourceAsStream(resourceFileName)
        if (stream == null) stream = FileUtils::class.java.getResourceAsStream("/$resourceFileName")
        if (stream == null) stream = FileUtils::class.java.classLoader.getResourceAsStream(resourceFileName)
        if (stream == null) stream = FileUtils::class.java.classLoader.getResourceAsStream("/$resourceFileName")
        requireNotNull(stream) { "Resource file name [$resourceFileName] is not found!" }
        try {
            BufferedInputStream(stream).use { bis ->
                val buf = ByteArrayOutputStream()
                var result = bis.read()
                while (result != -1) {
                    buf.write(result.toByte().toInt())
                    result = bis.read()
                }
                buf.close()

                // StandardCharsets.UTF_8.name() > JDK 7
                return buf.toString(StandardCharsets.UTF_8.name())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Write data to file
     *
     * @param fileName
     * @param content
     * @return
     */
    fun writeToFile(fileName: String?, content: String): Boolean {
        try {
            Files.write(Paths.get(fileName), content.toByteArray())
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }
} // end FileUtils

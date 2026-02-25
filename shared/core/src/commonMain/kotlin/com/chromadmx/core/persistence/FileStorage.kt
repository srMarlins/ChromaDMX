package com.chromadmx.core.persistence

/**
 * Platform-agnostic file storage abstraction for presets and configuration.
 */
interface FileStorage {
    /** Save text content to a file. Path is relative to the storage root. */
    fun saveFile(path: String, content: String)

    /** Read text content from a file. Returns null if file not found. */
    fun readFile(path: String): String?

    /** Delete a file. Returns true if successful. */
    fun deleteFile(path: String): Boolean

    /** List files in a directory. Returns relative paths. */
    fun listFiles(directory: String): List<String>

    /** Check if a file or directory exists. */
    fun exists(path: String): Boolean

    /** Create a directory if it doesn't exist. */
    fun mkdirs(directory: String)
}

package com.chromadmx.core.persistence

import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IosFileStorage : FileStorage {
    private val fileManager = NSFileManager.defaultManager
    private val rootDir = (fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).last() as NSURL).path!!

    override fun saveFile(path: String, content: String) {
        val fullPath = "$rootDir/$path"
        val dirPath = if (fullPath.contains("/")) fullPath.substringBeforeLast("/") else null
        if (dirPath != null && !fileManager.fileExistsAtPath(dirPath)) {
            fileManager.createDirectoryAtPath(dirPath, true, null, null)
        }
        (content as NSString).writeToFile(fullPath, true, NSUTF8StringEncoding, null)
    }

    override fun readFile(path: String): String? {
        val fullPath = "$rootDir/$path"
        if (!fileManager.fileExistsAtPath(fullPath)) return null
        return NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null)
    }

    override fun deleteFile(path: String): Boolean {
        val fullPath = "$rootDir/$path"
        return fileManager.removeItemAtPath(fullPath, null)
    }

    override fun listFiles(directory: String): List<String> {
        val fullPath = "$rootDir/$directory"
        return fileManager.contentsOfDirectoryAtPath(fullPath, null)?.map { it.toString() } ?: emptyList()
    }

    override fun exists(path: String): Boolean {
        val fullPath = "$rootDir/$path"
        return fileManager.fileExistsAtPath(fullPath)
    }

    override fun mkdirs(directory: String) {
        val fullPath = "$rootDir/$directory"
        fileManager.createDirectoryAtPath(fullPath, true, null, null)
    }
}

package com.chromadmx.agent

import com.chromadmx.core.persistence.FileStorage

class FakeFileStorage : FileStorage {
    private val files = mutableMapOf<String, String>()
    override fun saveFile(path: String, content: String) { files[path] = content }
    override fun readFile(path: String): String? = files[path]
    override fun deleteFile(path: String): Boolean = files.remove(path) != null
    override fun listFiles(directory: String): List<String> =
        files.keys.filter { it.startsWith(directory) }.map { it.substringAfterLast("/") }
    override fun exists(path: String): Boolean = files.containsKey(path)
    override fun mkdirs(directory: String) {}
}

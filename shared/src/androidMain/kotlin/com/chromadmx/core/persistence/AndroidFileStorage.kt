package com.chromadmx.core.persistence

import android.content.Context
import java.io.File

class AndroidFileStorage(private val context: Context) : FileStorage {
    private val rootDir: File get() = context.filesDir

    override fun saveFile(path: String, content: String) {
        val file = File(rootDir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    override fun readFile(path: String): String? {
        val file = File(rootDir, path)
        return if (file.exists()) file.readText() else null
    }

    override fun deleteFile(path: String): Boolean {
        val file = File(rootDir, path)
        return file.delete()
    }

    override fun listFiles(directory: String): List<String> {
        val dir = File(rootDir, directory)
        return dir.listFiles()?.map { it.name } ?: emptyList()
    }

    override fun exists(path: String): Boolean {
        return File(rootDir, path).exists()
    }

    override fun mkdirs(directory: String) {
        File(rootDir, directory).mkdirs()
    }
}

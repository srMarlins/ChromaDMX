package com.chromadmx.agent.scene

/**
 * Stub implementation of SceneStore to fix compilation.
 * In a real implementation, this would handle saving/loading scenes from disk.
 */
class SceneStore {
    fun load(name: String): Scene? = null
    fun list(): List<String> = emptyList()
}

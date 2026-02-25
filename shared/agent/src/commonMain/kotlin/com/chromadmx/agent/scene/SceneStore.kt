package com.chromadmx.agent.scene

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * In-memory store for named [ScenePreset] snapshots.
 *
 * Scenes are keyed by name. Saving a scene with an existing name overwrites it.
 * All operations are synchronized for thread safety across agent/UI threads.
 */
class SceneStore : SynchronizedObject() {
    private val lock = this
    private val scenes = mutableMapOf<String, ScenePreset>()

    init {
        // Register built-in presets on first launch
        BuiltInPresets.ALL.forEach { save(it) }
    }

    /** Save a scene. Overwrites any existing scene with the same name. */
    fun save(scene: ScenePreset) {
        synchronized(lock) { scenes[scene.name] = scene }
    }

    /** Load a scene by name, or null if not found. */
    fun load(name: String): ScenePreset? = synchronized(lock) { scenes[name] }

    /** List all saved scene names. */
    fun list(): List<String> = synchronized(lock) { scenes.keys.toList() }

    /** Delete a scene by name. Returns true if a scene was removed. Built-in scenes cannot be deleted. */
    fun delete(name: String): Boolean = synchronized(lock) {
        val scene = scenes[name]
        if (scene?.isBuiltIn == true) return false
        scenes.remove(name) != null
    }
}

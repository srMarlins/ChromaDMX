package com.chromadmx.agent.scene

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * In-memory store for named [Scene] snapshots.
 *
 * Scenes are keyed by name. Saving a scene with an existing name overwrites it.
 * All operations are synchronized for thread safety across agent/UI threads.
 */
class SceneStore : SynchronizedObject() {
    private val lock = this
    private val scenes = mutableMapOf<String, Scene>()

    /** Save a scene. Overwrites any existing scene with the same name. */
    fun save(scene: Scene) {
        synchronized(lock) { scenes[scene.name] = scene }
    }

    /** Load a scene by name, or null if not found. */
    fun load(name: String): Scene? = synchronized(lock) { scenes[name] }

    /** List all saved scene names. */
    fun list(): List<String> = synchronized(lock) { scenes.keys.toList() }

    /** Delete a scene by name. Returns true if a scene was removed. */
    fun delete(name: String): Boolean = synchronized(lock) { scenes.remove(name) != null }
}

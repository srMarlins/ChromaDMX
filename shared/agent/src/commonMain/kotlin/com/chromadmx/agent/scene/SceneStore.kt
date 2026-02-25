package com.chromadmx.agent.scene

/**
 * In-memory store for named [Scene] snapshots.
 *
 * Scenes are keyed by name. Saving a scene with an existing name overwrites it.
 */
class SceneStore {
    private val scenes = mutableMapOf<String, Scene>()

    /** Save a scene. Overwrites any existing scene with the same name. */
    fun save(scene: Scene) {
        scenes[scene.name] = scene
    }

    /** Load a scene by name, or null if not found. */
    fun load(name: String): Scene? = scenes[name]

    /** List all saved scene names. */
    fun list(): List<String> = scenes.keys.toList()

    /** Delete a scene by name. Returns true if a scene was removed. */
    fun delete(name: String): Boolean = scenes.remove(name) != null
}

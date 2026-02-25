package com.chromadmx.agent.scene

/**
 * In-memory store for [Scene] objects.
 *
 * Provides basic save/load/list functionality for UI and agent use.
 */
class SceneStore {
    private val scenes = mutableMapOf<String, Scene>()

    fun save(scene: Scene) {
        scenes[scene.name] = scene
    }

    fun load(name: String): Scene? {
        return scenes[name]
    }

    fun list(): List<String> {
        return scenes.keys.toList()
    }
}

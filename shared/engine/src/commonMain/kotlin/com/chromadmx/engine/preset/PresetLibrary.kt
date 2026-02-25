package com.chromadmx.engine.preset

import com.chromadmx.core.model.*
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.effect.EffectLayer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Manages the library of [ScenePreset]s.
 *
 * Handles persistence, filtering, and built-in protection.
 */
class PresetLibrary(
    private val storage: FileStorage,
    private val effectRegistry: EffectRegistry,
    private val effectStack: EffectStack
) : SynchronizedObject() {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val presetsDir = "presets"
    private val favoritesFile = "favorites.json"

    init {
        storage.mkdirs(presetsDir)
        ensureBuiltIns()
    }

    /**
     * Persist any missing built-in presets.
     *
     * Called once at construction. Each built-in preset is saved only if a
     * file with its ID does not already exist, so user customizations or
     * updated versions are never silently overwritten.
     */
    private fun ensureBuiltIns() {
        for (preset in builtInPresets()) {
            if (!storage.exists("$presetsDir/${preset.id}.json")) {
                savePreset(preset)
            }
        }
    }

    /** Save a preset to disk. Overwrites existing. */
    fun savePreset(preset: ScenePreset) {
        synchronized(this) {
            val fileName = "$presetsDir/${preset.id}.json"
            storage.saveFile(fileName, json.encodeToString(preset))
        }
    }

    /** Load a preset metadata by ID. */
    fun getPreset(id: String): ScenePreset? {
        return synchronized(this) {
            val fileName = "$presetsDir/$id.json"
            val content = storage.readFile(fileName) ?: return null
            try {
                json.decodeFromString<ScenePreset>(content)
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Delete a preset by ID. Prevents deleting built-in presets. */
    fun deletePreset(id: String): Boolean {
        return synchronized(this) {
            val preset = getPreset(id) ?: return false
            if (preset.isBuiltIn) return false
            storage.deleteFile("$presetsDir/$id.json")
        }
    }

    /** List all presets, optionally filtered by genre. */
    fun listPresets(genre: Genre? = null): List<ScenePreset> {
        return synchronized(this) {
            val files = storage.listFiles(presetsDir)
            files.mapNotNull { fileName ->
                if (!fileName.endsWith(".json")) return@mapNotNull null
                val id = fileName.removeSuffix(".json")
                getPreset(id)
            }.filter { genre == null || it.genre == genre }
        }
    }

    /** Apply a preset to the effect engine by ID. */
    fun loadPreset(id: String): Boolean {
        val preset = getPreset(id) ?: return false
        val newLayers = preset.layers.mapNotNull { config ->
            val effect = effectRegistry.get(config.effectId) ?: return@mapNotNull null
            EffectLayer(
                effect = effect,
                params = config.params,
                blendMode = config.blendMode,
                opacity = config.opacity,
                enabled = config.enabled
            )
        }
        effectStack.replaceLayers(newLayers)
        effectStack.masterDimmer = preset.masterDimmer
        return true
    }

    /** Export all presets as a single JSON string. */
    fun exportPresets(): String {
        val all = listPresets()
        return json.encodeToString(all)
    }

    /** Import presets from a JSON string. */
    fun importPresets(jsonString: String) {
        try {
            val presets = json.decodeFromString<List<ScenePreset>>(jsonString)
            presets.forEach { savePreset(it) }
        } catch (e: Exception) {
            // Silently fail for now or could be enhanced with better error handling
        }
    }

    /** Get the ordered list of favorite preset IDs. */
    fun getFavorites(): List<String> {
        return synchronized(this) {
            val content = storage.readFile(favoritesFile) ?: return emptyList()
            try {
                json.decodeFromString<List<String>>(content)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /** Set the ordered list of favorite preset IDs. */
    fun setFavorites(ids: List<String>) {
        synchronized(this) {
            storage.saveFile(favoritesFile, json.encodeToString(ids))
        }
    }
}

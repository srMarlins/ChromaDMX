package com.chromadmx.service

import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.ScenePreset
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.engine.preset.PresetLibrary
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Serializable bundle containing all exportable app data.
 *
 * @property version Schema version for forward-compatible imports.
 * @property fixtures All persisted fixtures with 3D positions.
 * @property presets All scene presets (built-in and user-created).
 * @property settings Key-value map of app settings.
 */
@Serializable
data class ExportBundle(
    val version: Int = CURRENT_VERSION,
    val fixtures: List<Fixture3D> = emptyList(),
    val presets: List<ScenePreset> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Result of an import operation.
 */
sealed interface ImportResult {
    data object Success : ImportResult
    data class Error(val message: String) : ImportResult
}

/**
 * Service for exporting and importing all app data as a single JSON bundle.
 *
 * Reads from and writes to the [FixtureStore], [PresetLibrary], and [SettingsStore]
 * to produce a portable [ExportBundle] that can be serialized to JSON.
 */
class DataExportService(
    private val fixtureStore: FixtureStore,
    private val presetLibrary: PresetLibrary,
    private val settingsStore: SettingsStore,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Export all app data into a JSON string.
     *
     * Collects the current snapshot of fixtures, presets, and settings,
     * bundles them into an [ExportBundle], and serializes to JSON.
     */
    suspend fun export(): String {
        val fixtures = fixtureStore.allFixtures().first()
        val presets = presetLibrary.listPresets()
        val settings = collectSettings()

        val bundle = ExportBundle(
            fixtures = fixtures,
            presets = presets,
            settings = settings,
        )
        return json.encodeToString(bundle)
    }

    /**
     * Import app data from a JSON string.
     *
     * Validates the JSON schema, then replaces fixtures and presets
     * with the imported data. Settings are applied individually.
     *
     * @param jsonString The JSON string to import.
     * @return [ImportResult] indicating success or describing the error.
     */
    suspend fun import(jsonString: String): ImportResult {
        val bundle = try {
            json.decodeFromString<ExportBundle>(jsonString)
        } catch (e: Exception) {
            return ImportResult.Error("Invalid JSON format: ${e.message}")
        }

        if (bundle.version > ExportBundle.CURRENT_VERSION) {
            return ImportResult.Error(
                "Unsupported export version ${bundle.version}. " +
                    "Maximum supported: ${ExportBundle.CURRENT_VERSION}"
            )
        }

        return try {
            applyBundle(bundle)
            ImportResult.Success
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    private suspend fun applyBundle(bundle: ExportBundle) {
        // Replace fixtures
        fixtureStore.deleteAll()
        if (bundle.fixtures.isNotEmpty()) {
            fixtureStore.saveAll(bundle.fixtures)
        }

        // Replace presets (save each individually — PresetLibrary handles persistence)
        bundle.presets.forEach { preset ->
            presetLibrary.savePreset(preset)
        }

        // Apply settings
        applySettings(bundle.settings)
    }

    private suspend fun collectSettings(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["masterDimmer"] = settingsStore.masterDimmer.first().toString()
        map["themePreference"] = settingsStore.themePreference.first()
        map["isSimulation"] = settingsStore.isSimulation.first().toString()
        map["transportMode"] = settingsStore.transportMode.first()
        map["setupCompleted"] = settingsStore.setupCompleted.first().toString()
        val activePreset = settingsStore.activePresetId.first()
        if (activePreset != null) {
            map["activePresetId"] = activePreset
        }
        return map
    }

    private suspend fun applySettings(settings: Map<String, String>) {
        settings["masterDimmer"]?.toFloatOrNull()?.let {
            settingsStore.setMasterDimmer(it)
        }
        settings["themePreference"]?.let {
            settingsStore.setThemePreference(it)
        }
        settings["isSimulation"]?.toBooleanStrictOrNull()?.let {
            settingsStore.setIsSimulation(it)
        }
        settings["transportMode"]?.let {
            settingsStore.setTransportMode(it)
        }
        settings["activePresetId"]?.let {
            settingsStore.setActivePresetId(it)
        }
        settings["setupCompleted"]?.toBooleanStrictOrNull()?.let {
            settingsStore.setSetupCompleted(it)
        }
    }
}

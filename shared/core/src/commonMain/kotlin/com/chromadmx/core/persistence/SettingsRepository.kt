package com.chromadmx.core.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.chromadmx.core.db.ChromaDmxDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Key-value settings repository using SQLDelight.
 * Provides reactive flows for all app settings.
 */
class SettingsRepository(private val db: ChromaDmxDatabase) {
    private val queries = db.settingsQueries

    init {
        queries.ensureDefaults()
    }

    private val settingsFlow = queries.selectSettings()
        .asFlow()
        .mapToOne(Dispatchers.Default)

    val masterDimmer: Flow<Float> = settingsFlow.map { it.master_dimmer.toFloat() }
    val isSimulation: Flow<Boolean> = settingsFlow.map { it.is_simulation != 0L }
    val activePresetId: Flow<String?> = settingsFlow.map { it.active_preset_id }
    val themePreference: Flow<String> = settingsFlow.map { it.theme_preference }
    val transportMode: Flow<String> = settingsFlow.map { it.transport_mode }
    val setupCompleted: Flow<Boolean> = settingsFlow.map { it.setup_completed != 0L }

    fun setMasterDimmer(value: Float) {
        queries.updateMasterDimmer(value.toDouble())
    }

    fun setSimulation(enabled: Boolean) {
        queries.updateIsSimulation(if (enabled) 1L else 0L)
    }

    fun setActivePresetId(id: String?) {
        queries.updateActivePresetId(id)
    }

    fun setThemePreference(theme: String) {
        queries.updateThemePreference(theme)
    }

    fun setTransportMode(mode: String) {
        queries.updateTransportMode(mode)
    }

    fun setSetupCompleted(completed: Boolean) {
        queries.updateSetupCompleted(if (completed) 1L else 0L)
    }
}

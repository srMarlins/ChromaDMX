package com.chromadmx.core.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.chromadmx.core.db.ChromaDmxDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Abstraction for app settings, enabling fake implementations in tests.
 */
interface SettingsStore {
    val masterDimmer: Flow<Float>
    val themePreference: Flow<String>
    val isSimulation: Flow<Boolean>
    val transportMode: Flow<String>
    val activePresetId: Flow<String?>
    val setupCompleted: Flow<Boolean>

    suspend fun setMasterDimmer(value: Float)
    suspend fun setThemePreference(value: String)
    suspend fun setIsSimulation(value: Boolean)
    suspend fun setTransportMode(value: String)
    suspend fun setActivePresetId(value: String?)
    suspend fun setSetupCompleted(value: Boolean)
}

/**
 * SQLDelight-backed repository for app settings persistence.
 *
 * Uses a single-row app_settings table with typed columns.
 * Provides reactive [Flow]-based reads and synchronous setters.
 */
class SettingsRepository(private val db: ChromaDmxDatabase) : SettingsStore {

    private val queries = db.settingsQueries

    init {
        queries.ensureDefaults()
    }

    private val settingsFlow = queries.selectSettings()
        .asFlow()
        .mapToOne(Dispatchers.Default)

    override val masterDimmer: Flow<Float> = settingsFlow.map { it.master_dimmer.toFloat() }
    override val isSimulation: Flow<Boolean> = settingsFlow.map { it.is_simulation != 0L }
    override val activePresetId: Flow<String?> = settingsFlow.map { it.active_preset_id }
    override val themePreference: Flow<String> = settingsFlow.map { it.theme_preference }
    override val transportMode: Flow<String> = settingsFlow.map { it.transport_mode }
    override val setupCompleted: Flow<Boolean> = settingsFlow.map { it.setup_completed != 0L }

    override suspend fun setMasterDimmer(value: Float) {
        queries.updateMasterDimmer(value.toDouble())
    }

    override suspend fun setIsSimulation(value: Boolean) {
        queries.updateIsSimulation(if (value) 1L else 0L)
    }

    override suspend fun setActivePresetId(value: String?) {
        queries.updateActivePresetId(value)
    }

    override suspend fun setThemePreference(value: String) {
        queries.updateThemePreference(value)
    }

    override suspend fun setTransportMode(value: String) {
        queries.updateTransportMode(value)
    }

    override suspend fun setSetupCompleted(value: Boolean) {
        queries.updateSetupCompleted(if (value) 1L else 0L)
    }
}

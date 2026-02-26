package com.chromadmx.core.persistence

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.flow.Flow

/**
 * Key-value settings repository using multiplatform-settings.
 * Provides reactive flows for all app settings.
 */
@OptIn(ExperimentalSettingsApi::class)
class SettingsRepository(private val settings: FlowSettings) {

    // Display
    val masterDimmer: Flow<Float> = settings.getFloatFlow("master_dimmer", 1f)
    val themePreference: Flow<String> = settings.getStringFlow("theme", "MatchaDark")

    // Simulation
    val isSimulation: Flow<Boolean> = settings.getBooleanFlow("simulation", false)

    // Transport
    val transportMode: Flow<String> = settings.getStringFlow("transport_mode", "Real")

    // Active preset
    val activePresetId: Flow<String?> = settings.getStringOrNullFlow("active_preset")

    // Setup completion
    val setupCompleted: Flow<Boolean> = settings.getBooleanFlow("setup_completed", false)

    suspend fun setMasterDimmer(value: Float) { settings.putFloat("master_dimmer", value) }
    suspend fun setThemePreference(value: String) { settings.putString("theme", value) }
    suspend fun setIsSimulation(value: Boolean) { settings.putBoolean("simulation", value) }
    suspend fun setTransportMode(value: String) { settings.putString("transport_mode", value) }
    suspend fun setActivePresetId(value: String?) {
        if (value != null) settings.putString("active_preset", value)
        else settings.remove("active_preset")
    }
    suspend fun setSetupCompleted(value: Boolean) { settings.putBoolean("setup_completed", value) }
}

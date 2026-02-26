package com.chromadmx.core.persistence

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow

/**
 * Repository for application-wide settings and preferences.
 * Uses multiplatform-settings for cross-platform key-value storage.
 */
@OptIn(ExperimentalSettingsApi::class)
class SettingsRepository(private val observableSettings: ObservableSettings) {
    private val flowSettings = observableSettings.toFlowSettings()

    val masterDimmer: Flow<Float> = flowSettings.getFloatFlow(KEY_MASTER_DIMMER, 1.0f)
    val isSimulation: Flow<Boolean> = flowSettings.getBooleanFlow(KEY_IS_SIMULATION, false)
    val activePresetId: Flow<String?> = flowSettings.getStringOrNullFlow(KEY_ACTIVE_PRESET_ID)
    val themePreference: Flow<String> = flowSettings.getStringFlow(KEY_THEME_PREFERENCE, "Dark")
    val transportMode: Flow<String> = flowSettings.getStringFlow(KEY_TRANSPORT_MODE, "Real")

    fun setMasterDimmer(value: Float) {
        observableSettings[KEY_MASTER_DIMMER] = value
    }

    fun setSimulation(enabled: Boolean) {
        observableSettings[KEY_IS_SIMULATION] = enabled
    }

    fun setActivePresetId(id: String?) {
        if (id == null) {
            observableSettings.remove(KEY_ACTIVE_PRESET_ID)
        } else {
            observableSettings[KEY_ACTIVE_PRESET_ID] = id
        }
    }

    fun setThemePreference(theme: String) {
        observableSettings[KEY_THEME_PREFERENCE] = theme
    }

    fun setTransportMode(mode: String) {
        observableSettings[KEY_TRANSPORT_MODE] = mode
    }

    companion object {
        private const val KEY_MASTER_DIMMER = "master_dimmer"
        private const val KEY_IS_SIMULATION = "is_simulation"
        private const val KEY_ACTIVE_PRESET_ID = "active_preset_id"
        private const val KEY_THEME_PREFERENCE = "theme_preference"
        private const val KEY_TRANSPORT_MODE = "transport_mode"
    }
}

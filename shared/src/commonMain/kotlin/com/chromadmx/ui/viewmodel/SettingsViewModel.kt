package com.chromadmx.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Settings overlay.
 */
class SettingsViewModel(
    private val scope: CoroutineScope,
) {
    private val _simulationEnabled = MutableStateFlow(false)
    val simulationEnabled: StateFlow<Boolean> = _simulationEnabled.asStateFlow()

    private val _agentApiKey = MutableStateFlow("")
    val agentApiKey: StateFlow<String> = _agentApiKey.asStateFlow()

    fun setSimulationEnabled(enabled: Boolean) {
        _simulationEnabled.value = enabled
    }

    fun setAgentApiKey(key: String) {
        _agentApiKey.value = key
    }

    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
    }
}

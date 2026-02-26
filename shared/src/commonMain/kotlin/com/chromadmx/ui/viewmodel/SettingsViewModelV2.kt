package com.chromadmx.ui.viewmodel

import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.networking.DmxTransportRouter
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.TransportMode
import com.chromadmx.ui.state.AgentStatus
import com.chromadmx.ui.state.SettingsEvent
import com.chromadmx.ui.state.SettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings view model following the Unidirectional Data Flow (UDF) pattern.
 *
 * Exposes a single [state] flow and a single [onEvent] entry point,
 * replacing the individual MutableStateFlows and setter methods in the
 * original [SettingsViewModel].
 *
 * Dependencies:
 * - [SettingsStore] for persisting settings (simulation, onboarding)
 * - [DmxTransportRouter] for switching transport modes at runtime
 * - [FixtureDiscovery] for triggering network rescans
 */
class SettingsViewModelV2(
    private val settingsRepository: SettingsStore,
    private val transportRouter: DmxTransportRouter,
    private val fixtureDiscovery: FixtureDiscovery,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        // Derive simulation state from the persisted repository value.
        scope.launch {
            settingsRepository.isSimulation.collect { sim ->
                _state.update { it.copy(simulationEnabled = sim) }
            }
        }
    }

    /**
     * Single entry point for all UI events. Dispatches to the
     * appropriate handler based on event type.
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetPollInterval ->
                _state.update { it.copy(pollInterval = event.interval) }

            is SettingsEvent.SetProtocol ->
                _state.update { it.copy(protocol = event.protocol) }

            is SettingsEvent.SetManualIp ->
                _state.update { it.copy(manualIp = event.ip) }

            is SettingsEvent.SetManualUniverse ->
                _state.update { it.copy(manualUniverse = event.universe) }

            is SettingsEvent.SetManualStartAddress ->
                _state.update { it.copy(manualStartAddress = event.address) }

            is SettingsEvent.ForceRescan ->
                forceRescan()

            is SettingsEvent.AddFixtureProfile ->
                _state.update { it.copy(fixtureProfiles = it.fixtureProfiles + event.profile) }

            is SettingsEvent.DeleteFixtureProfile ->
                _state.update {
                    it.copy(fixtureProfiles = it.fixtureProfiles.filter { p -> p.profileId != event.profileId })
                }

            is SettingsEvent.ToggleSimulation ->
                toggleSimulation(event.enabled)

            is SettingsEvent.SetRigPreset ->
                _state.update { it.copy(selectedRigPreset = event.preset) }

            is SettingsEvent.ResetSimulation ->
                resetSimulation()

            is SettingsEvent.UpdateAgentConfig ->
                _state.update { it.copy(agentConfig = event.config) }

            is SettingsEvent.TestAgentConnection ->
                testAgentConnection()

            is SettingsEvent.ResetOnboarding ->
                resetOnboarding()

            is SettingsEvent.ExportAppData -> { /* TODO */ }
            is SettingsEvent.ImportAppData -> { /* TODO */ }
        }
    }

    private fun forceRescan() {
        fixtureDiscovery.startScan()
    }

    private fun toggleSimulation(enabled: Boolean) {
        _state.update { it.copy(simulationEnabled = enabled) }
        scope.launch {
            settingsRepository.setIsSimulation(enabled)
        }
        val mode = if (enabled) TransportMode.Simulated else TransportMode.Real
        transportRouter.switchTo(mode)
    }

    private fun resetSimulation() {
        _state.update { it.copy(simulationEnabled = false) }
        scope.launch {
            settingsRepository.setIsSimulation(false)
        }
        transportRouter.switchTo(TransportMode.Real)
    }

    private fun testAgentConnection() {
        scope.launch {
            _state.update { it.copy(agentStatus = AgentStatus.Testing) }
            delay(AGENT_TEST_DELAY_MS)
            val config = _state.value.agentConfig
            if (config.apiKey.length > MIN_API_KEY_LENGTH) {
                _state.update { it.copy(agentStatus = AgentStatus.Success) }
            } else {
                _state.update { it.copy(agentStatus = AgentStatus.Error("Invalid API Key")) }
            }
        }
    }

    private fun resetOnboarding() {
        scope.launch {
            settingsRepository.setSetupCompleted(false)
        }
    }

    companion object {
        /** Delay in milliseconds for the agent connection test. */
        const val AGENT_TEST_DELAY_MS = 1500L

        /** Minimum API key length for a "valid" key in the connection test. */
        const val MIN_API_KEY_LENGTH = 5

        /** Model IDs available for selection in the settings UI. */
        val AVAILABLE_MODELS = listOf(
            "gemini_2_5_flash",
            "gemini_2_5_pro",
            "sonnet_4",
            "sonnet_4_5",
        )
    }
}

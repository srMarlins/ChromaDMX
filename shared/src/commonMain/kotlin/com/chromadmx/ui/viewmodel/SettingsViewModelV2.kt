package com.chromadmx.ui.viewmodel

import com.chromadmx.core.DebugFlags
import com.chromadmx.subscription.model.Entitlement
import com.chromadmx.subscription.model.SubscriptionTier
import com.chromadmx.subscription.repository.SubscriptionStore
import com.chromadmx.subscription.service.SubscriptionManager
import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.networking.DmxTransportRouter
import com.chromadmx.networking.FixtureDiscoveryRouter
import com.chromadmx.networking.TransportMode
import com.chromadmx.service.DataExportService
import com.chromadmx.service.ImportResult
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.ui.state.AgentStatus
import com.chromadmx.ui.theme.PixelColorTheme
import com.chromadmx.ui.state.DataTransferStatus
import com.chromadmx.ui.state.SettingsEvent
import com.chromadmx.ui.state.SettingsUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings view model following the Unidirectional Data Flow (UDF) pattern.
 *
 * Exposes a single [state] flow and a single [onEvent] entry point,
 * replacing the individual MutableStateFlows and setter methods in the
 * original [SettingsViewModel].
 *
 * Dependencies:
 * - [SettingsStore] for persisting settings (simulation, onboarding)
 * - [DmxTransportRouter] for switching DMX output transport modes at runtime
 * - [FixtureDiscoveryRouter] for switching node discovery modes and triggering rescans
 */
class SettingsViewModelV2(
    private val settingsRepository: SettingsStore,
    private val transportRouter: DmxTransportRouter,
    private val discoveryRouter: FixtureDiscoveryRouter,
    private val scope: CoroutineScope,
    private val fixtureStore: FixtureStore? = null,
    private val dataExportService: DataExportService? = null,
    private val subscriptionManager: SubscriptionManager? = null,
    private val subscriptionStore: SubscriptionStore? = null,
) {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    val subscriptionTier: StateFlow<SubscriptionTier> = subscriptionManager?.currentTier
        ?: MutableStateFlow(SubscriptionTier.FREE)

    val isDebugMode: Boolean get() = DebugFlags.isDebugBuild

    val canExportData: Boolean
        get() = subscriptionManager?.hasEntitlement(Entitlement.DataExport) ?: false

    init {
        // Seed the built-in fixture profiles so they appear immediately.
        _state.update { it.copy(fixtureProfiles = BuiltInProfiles.all().toImmutableList()) }

        // One-time startup: sync routers from persisted simulation state.
        // Subsequent router switches are handled directly by toggleSimulation/resetSimulation.
        scope.launch {
            val sim = settingsRepository.isSimulation.first()
            if (sim) {
                transportRouter.switchTo(TransportMode.Simulated)
                discoveryRouter.switchTo(TransportMode.Simulated)
                discoveryRouter.startScan()
            }
        }

        // Derive simulation UI state from the persisted repository value.
        scope.launch {
            settingsRepository.isSimulation.collect { sim ->
                _state.update { it.copy(simulationEnabled = sim) }
            }
        }

        // Derive theme preference from the persisted repository value.
        scope.launch {
            settingsRepository.themePreference.collect { name ->
                val theme = PixelColorTheme.entries.firstOrNull { it.name == name }
                    ?: PixelColorTheme.MatchaDark
                _state.update { it.copy(themePreference = theme) }
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
                _state.update { it.copy(fixtureProfiles = (it.fixtureProfiles + event.profile).toImmutableList()) }

            is SettingsEvent.UpdateFixtureProfile ->
                _state.update { st ->
                    st.copy(
                        fixtureProfiles = st.fixtureProfiles.map { p ->
                            if (p.profileId == event.profile.profileId) event.profile else p
                        }.toImmutableList()
                    )
                }

            is SettingsEvent.DeleteFixtureProfile ->
                _state.update {
                    it.copy(fixtureProfiles = it.fixtureProfiles.filter { p -> p.profileId != event.profileId }.toImmutableList())
                }

            is SettingsEvent.ToggleSimulation ->
                toggleSimulation(event.enabled)

            is SettingsEvent.SetRigPreset ->
                handleSetRigPreset(event.preset)

            is SettingsEvent.ResetSimulation ->
                resetSimulation()

            is SettingsEvent.UpdateAgentConfig ->
                _state.update { it.copy(agentConfig = event.config) }

            is SettingsEvent.TestAgentConnection ->
                testAgentConnection()

            is SettingsEvent.ResetOnboarding ->
                resetOnboarding()

            is SettingsEvent.ExportAppData -> handleExport()
            is SettingsEvent.ImportAppData -> handleImport(event.json)
            is SettingsEvent.DismissDataTransferStatus ->
                _state.update { it.copy(dataTransferStatus = DataTransferStatus.Idle) }

            is SettingsEvent.SetThemePreference ->
                setThemePreference(event.theme)

            is SettingsEvent.SetDebugTier ->
                setDebugTier(event.tier)
        }
    }

    private fun forceRescan() {
        discoveryRouter.startScan()
    }

    private fun handleSetRigPreset(preset: com.chromadmx.simulation.fixtures.RigPreset) {
        _state.update { it.copy(selectedRigPreset = preset) }
        val store = fixtureStore ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                val rig = SimulatedFixtureRig(preset)
                store.deleteAll()
                store.saveAll(rig.fixtures)
            }
        }
    }

    private fun toggleSimulation(enabled: Boolean) {
        _state.update { it.copy(simulationEnabled = enabled) }
        scope.launch {
            withContext(Dispatchers.IO) { settingsRepository.setIsSimulation(enabled) }
        }
        val mode = if (enabled) TransportMode.Simulated else TransportMode.Real
        transportRouter.switchTo(mode)
        discoveryRouter.switchTo(mode)
        if (enabled) {
            discoveryRouter.startScan()
        }
    }

    private fun resetSimulation() {
        _state.update { it.copy(simulationEnabled = false) }
        scope.launch {
            withContext(Dispatchers.IO) { settingsRepository.setIsSimulation(false) }
        }
        transportRouter.switchTo(TransportMode.Real)
        discoveryRouter.switchTo(TransportMode.Real)
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
            withContext(Dispatchers.IO) { settingsRepository.setSetupCompleted(false) }
        }
    }

    private fun setDebugTier(tier: SubscriptionTier) {
        if (!DebugFlags.isDebugBuild) return
        val store = subscriptionStore ?: return
        scope.launch {
            withContext(Dispatchers.IO) { store.setTier(tier) }
        }
    }

    private fun setThemePreference(theme: PixelColorTheme) {
        _state.update { it.copy(themePreference = theme) }
        scope.launch {
            settingsRepository.setThemePreference(theme.name)
        }
    }

    private fun handleExport() {
        val service = dataExportService ?: run {
            _state.update {
                it.copy(dataTransferStatus = DataTransferStatus.Error("Export service not available"))
            }
            return
        }
        scope.launch {
            _state.update { it.copy(dataTransferStatus = DataTransferStatus.InProgress) }
            try {
                val json = service.export()
                _state.update { it.copy(dataTransferStatus = DataTransferStatus.ExportReady(json)) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(dataTransferStatus = DataTransferStatus.Error("Export failed: ${e.message}"))
                }
            }
        }
    }

    private fun handleImport(json: String) {
        val service = dataExportService ?: run {
            _state.update {
                it.copy(dataTransferStatus = DataTransferStatus.Error("Import service not available"))
            }
            return
        }
        scope.launch {
            _state.update { it.copy(dataTransferStatus = DataTransferStatus.InProgress) }
            when (val result = service.import(json)) {
                is ImportResult.Success -> {
                    _state.update { it.copy(dataTransferStatus = DataTransferStatus.ImportSuccess) }
                }
                is ImportResult.Error -> {
                    _state.update {
                        it.copy(dataTransferStatus = DataTransferStatus.Error(result.message))
                    }
                }
            }
        }
    }

    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
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

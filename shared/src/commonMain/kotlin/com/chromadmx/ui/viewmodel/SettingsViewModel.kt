package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.core.model.FixtureProfile
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.simulation.fixtures.RigPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the connection test status for the AI agent.
 */
sealed class AgentStatus {
    data object Idle : AgentStatus()
    data object Testing : AgentStatus()
    data object Success : AgentStatus()
    data class Error(val message: String) : AgentStatus()
}

class SettingsViewModel(
    private val nodeDiscovery: NodeDiscovery,
    private val scope: CoroutineScope,
) {
    // Network Settings
    private val _pollInterval = MutableStateFlow(NodeDiscovery.DEFAULT_POLL_INTERVAL_MS)
    val pollInterval: StateFlow<Long> = _pollInterval.asStateFlow()

    private val _protocol = MutableStateFlow("Art-Net") // Art-Net or sACN
    val protocol: StateFlow<String> = _protocol.asStateFlow()

    private val _manualIp = MutableStateFlow("192.168.1.100")
    val manualIp: StateFlow<String> = _manualIp.asStateFlow()

    private val _manualUniverse = MutableStateFlow("0")
    val manualUniverse: StateFlow<String> = _manualUniverse.asStateFlow()

    private val _manualStartAddress = MutableStateFlow("1")
    val manualStartAddress: StateFlow<String> = _manualStartAddress.asStateFlow()

    // Fixture Profiles
    private val _fixtureProfiles = MutableStateFlow<List<FixtureProfile>>(emptyList())
    val fixtureProfiles: StateFlow<List<FixtureProfile>> = _fixtureProfiles.asStateFlow()

    // Simulation Settings
    private val _simulationEnabled = MutableStateFlow(false)
    val simulationEnabled: StateFlow<Boolean> = _simulationEnabled.asStateFlow()

    private val _selectedRigPreset = MutableStateFlow(RigPreset.SMALL_DJ)
    val selectedRigPreset: StateFlow<RigPreset> = _selectedRigPreset.asStateFlow()

    // Agent Settings
    private val _agentConfig = MutableStateFlow(AgentConfig())
    val agentConfig: StateFlow<AgentConfig> = _agentConfig.asStateFlow()

    private val _agentStatus = MutableStateFlow<AgentStatus>(AgentStatus.Idle)
    val agentStatus: StateFlow<AgentStatus> = _agentStatus.asStateFlow()

    fun setPollInterval(interval: Long) {
        _pollInterval.value = interval
    }

    fun setProtocol(protocol: String) {
        _protocol.value = protocol
    }

    fun setManualIp(ip: String) {
        _manualIp.value = ip
    }

    fun setManualUniverse(universe: String) {
        _manualUniverse.value = universe
    }

    fun setManualStartAddress(address: String) {
        _manualStartAddress.value = address
    }

    fun forceRescan() {
        scope.launch {
            nodeDiscovery.sendPoll()
        }
    }

    fun addFixtureProfile(profile: FixtureProfile) {
        _fixtureProfiles.value += profile
    }

    fun deleteFixtureProfile(profileId: String) {
        _fixtureProfiles.value = _fixtureProfiles.value.filter { it.profileId != profileId }
    }

    fun toggleSimulation(enabled: Boolean) {
        _simulationEnabled.value = enabled
    }

    fun setRigPreset(preset: RigPreset) {
        _selectedRigPreset.value = preset
    }

    fun resetSimulation() {
        // Reset simulation logic
    }

    fun updateAgentConfig(config: AgentConfig) {
        _agentConfig.value = config
    }

    fun testAgentConnection() {
        scope.launch {
            _agentStatus.value = AgentStatus.Testing
            delay(1500)
            if (_agentConfig.value.apiKey.length > 5) {
                _agentStatus.value = AgentStatus.Success
            } else {
                _agentStatus.value = AgentStatus.Error("Invalid API Key")
            }
        }
    }

    fun resetOnboarding() {
        // Reset onboarding logic
    }

    fun exportAppData() {
        // Export logic: would serialize all state to JSON and share/save file
    }

    fun importAppData() {
        // Import logic: would open file picker and deserialize JSON
    }

    companion object {
        /** Model IDs available for selection in the settings UI. */
        val AVAILABLE_MODELS = listOf(
            "gemini_2_5_flash",
            "gemini_2_5_pro",
            "sonnet_4",
            "sonnet_4_5",
        )
    }
}

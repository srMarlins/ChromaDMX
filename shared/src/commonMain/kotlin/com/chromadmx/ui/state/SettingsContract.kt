package com.chromadmx.ui.state

import androidx.compose.runtime.Immutable
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.core.model.FixtureProfile
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.subscription.model.SubscriptionTier
import com.chromadmx.ui.theme.PixelColorTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * DMX protocol type.
 */
enum class ProtocolType {
    ART_NET,
    SACN
}

/**
 * Connection test status for the AI agent.
 */
sealed interface AgentStatus {
    data object Idle : AgentStatus
    data object Testing : AgentStatus
    data object Success : AgentStatus
    data class Error(val message: String) : AgentStatus
}

/**
 * Status of an export or import operation.
 */
sealed interface DataTransferStatus {
    data object Idle : DataTransferStatus
    data object InProgress : DataTransferStatus
    data class ExportReady(val json: String) : DataTransferStatus
    data object ImportSuccess : DataTransferStatus
    data class Error(val message: String) : DataTransferStatus
}

/**
 * UI State for the Settings screen.
 */
@Immutable
data class SettingsUiState(
    // Network Settings
    val pollInterval: Long = NodeDiscovery.DEFAULT_POLL_INTERVAL_MS,
    val protocol: ProtocolType = ProtocolType.ART_NET,
    val manualIp: String = "192.168.1.100",
    val manualUniverse: String = "0",
    val manualStartAddress: String = "1",

    // Fixture Profiles
    val fixtureProfiles: ImmutableList<FixtureProfile> = persistentListOf(),

    // Simulation Settings
    val simulationEnabled: Boolean = false,
    val selectedRigPreset: RigPreset = RigPreset.SMALL_DJ,

    // Agent Settings
    val agentConfig: AgentConfig = AgentConfig(),
    val agentStatus: AgentStatus = AgentStatus.Idle,

    // Export / Import
    val dataTransferStatus: DataTransferStatus = DataTransferStatus.Idle,

    // Theme
    val themePreference: PixelColorTheme = PixelColorTheme.MatchaDark,
)

/**
 * Events for the Settings screen.
 */
sealed interface SettingsEvent {
    data class SetPollInterval(val interval: Long) : SettingsEvent
    data class SetProtocol(val protocol: ProtocolType) : SettingsEvent
    data class SetManualIp(val ip: String) : SettingsEvent
    data class SetManualUniverse(val universe: String) : SettingsEvent
    data class SetManualStartAddress(val address: String) : SettingsEvent
    data object ForceRescan : SettingsEvent
    data class AddFixtureProfile(val profile: FixtureProfile) : SettingsEvent
    data class UpdateFixtureProfile(val profile: FixtureProfile) : SettingsEvent
    data class DeleteFixtureProfile(val profileId: String) : SettingsEvent
    data class ToggleSimulation(val enabled: Boolean) : SettingsEvent
    data class SetRigPreset(val preset: RigPreset) : SettingsEvent
    data object ResetSimulation : SettingsEvent
    data class UpdateAgentConfig(val config: AgentConfig) : SettingsEvent
    data object TestAgentConnection : SettingsEvent
    data object ResetOnboarding : SettingsEvent
    data object ExportAppData : SettingsEvent
    data class ImportAppData(val json: String) : SettingsEvent
    data object DismissDataTransferStatus : SettingsEvent
    data class SetThemePreference(val theme: PixelColorTheme) : SettingsEvent
    data class SetDebugTier(val tier: SubscriptionTier) : SettingsEvent
}

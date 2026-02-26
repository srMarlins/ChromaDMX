package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.core.model.ChannelType
import com.chromadmx.core.model.DmxNode
import com.chromadmx.core.model.FixtureProfile
import com.chromadmx.core.model.FixtureType
import com.chromadmx.core.model.Channel
import com.chromadmx.core.persistence.SettingsRepository
import com.chromadmx.networking.ConnectionState
import com.chromadmx.networking.DmxTransport
import com.chromadmx.networking.DmxTransportRouter
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.TransportMode
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.ui.state.AgentStatus
import com.chromadmx.ui.state.ProtocolType
import com.chromadmx.ui.state.SettingsEvent
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// -- Fakes --

/**
 * Fake [DmxTransport] that tracks start/stop/frame calls
 * without performing any real I/O.
 */
private class FakeDmxTransport : DmxTransport {
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    private var _isRunning = false
    override val isRunning: Boolean get() = _isRunning

    override fun start() { _isRunning = true; _connectionState.value = ConnectionState.Connected }
    override fun stop() { _isRunning = false; _connectionState.value = ConnectionState.Disconnected }
    override fun sendFrame(universe: Int, channels: ByteArray) {}
    override fun updateFrame(universeData: Map<Int, ByteArray>) {}
}

/**
 * Fake [FixtureDiscovery] that records scan start/stop calls.
 */
private class FakeFixtureDiscovery : FixtureDiscovery {
    private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
    override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    var startScanCount = 0
        private set
    var stopScanCount = 0
        private set

    override fun startScan() {
        startScanCount++
        _isScanning.value = true
    }

    override fun stopScan() {
        stopScanCount++
        _isScanning.value = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSettingsApi::class)
class SettingsViewModelV2Test {

    private fun createSettingsRepository(): SettingsRepository {
        return SettingsRepository(MapSettings().toFlowSettings())
    }

    private fun createRouter(scope: CoroutineScope): DmxTransportRouter {
        return DmxTransportRouter(
            real = FakeDmxTransport(),
            simulated = FakeDmxTransport(),
            scope = scope,
        )
    }

    /**
     * Helper to create VM + router + discovery triple.
     * Uses [UnconfinedTestDispatcher] by default so that launched
     * coroutines (init collectors, scope.launch calls) run eagerly.
     */
    private fun createVm(
        settingsRepository: SettingsRepository = createSettingsRepository(),
        transportRouter: DmxTransportRouter? = null,
        fixtureDiscovery: FakeFixtureDiscovery = FakeFixtureDiscovery(),
        scope: CoroutineScope,
    ): Triple<SettingsViewModelV2, DmxTransportRouter, FakeFixtureDiscovery> {
        val router = transportRouter ?: createRouter(scope)
        val vm = SettingsViewModelV2(
            settingsRepository = settingsRepository,
            transportRouter = router,
            fixtureDiscovery = fixtureDiscovery,
            scope = scope,
        )
        return Triple(vm, router, fixtureDiscovery)
    }

    // -- Initial state --

    @Test
    fun initialStateHasDefaults() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)
        val state = vm.state.value
        assertFalse(state.simulationEnabled)
        assertEquals(ProtocolType.ART_NET, state.protocol)
        assertEquals("192.168.1.100", state.manualIp)
        assertEquals("0", state.manualUniverse)
        assertEquals("1", state.manualStartAddress)
        assertTrue(state.fixtureProfiles.isEmpty())
        assertEquals(RigPreset.SMALL_DJ, state.selectedRigPreset)
        assertIs<AgentStatus.Idle>(state.agentStatus)
    }

    // -- Protocol change --

    @Test
    fun setProtocolUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetProtocol(ProtocolType.SACN))
        assertEquals(ProtocolType.SACN, vm.state.value.protocol)
    }

    @Test
    fun setProtocolBackToArtNet() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetProtocol(ProtocolType.SACN))
        vm.onEvent(SettingsEvent.SetProtocol(ProtocolType.ART_NET))
        assertEquals(ProtocolType.ART_NET, vm.state.value.protocol)
    }

    // -- Transport mode switch via ToggleSimulation --

    @Test
    fun toggleSimulationOnSwitchesRouterToSimulated() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, router, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.ToggleSimulation(true))

        assertTrue(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Simulated, router.mode.value)
    }

    @Test
    fun toggleSimulationOffSwitchesRouterToReal() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, router, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.ToggleSimulation(true))
        vm.onEvent(SettingsEvent.ToggleSimulation(false))

        assertFalse(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Real, router.mode.value)
    }

    @Test
    fun toggleSimulationPersistsToRepository() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val repo = createSettingsRepository()
        val (vm, _, _) = createVm(settingsRepository = repo, scope = scope)

        vm.onEvent(SettingsEvent.ToggleSimulation(true))

        assertTrue(repo.isSimulation.first())
    }

    // -- Force rescan --

    @Test
    fun forceRescanTriggersDiscoveryStartScan() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val discovery = FakeFixtureDiscovery()
        val (vm, _, _) = createVm(fixtureDiscovery = discovery, scope = scope)

        assertEquals(0, discovery.startScanCount)
        vm.onEvent(SettingsEvent.ForceRescan)
        assertEquals(1, discovery.startScanCount)
    }

    @Test
    fun forceRescanCanBeCalledMultipleTimes() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val discovery = FakeFixtureDiscovery()
        val (vm, _, _) = createVm(fixtureDiscovery = discovery, scope = scope)

        vm.onEvent(SettingsEvent.ForceRescan)
        vm.onEvent(SettingsEvent.ForceRescan)
        vm.onEvent(SettingsEvent.ForceRescan)
        assertEquals(3, discovery.startScanCount)
    }

    // -- Simulation toggle state --

    @Test
    fun simulationEnabledDefaultsToFalse() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)
        assertFalse(vm.state.value.simulationEnabled)
    }

    @Test
    fun toggleSimulationTrueUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.ToggleSimulation(true))
        assertTrue(vm.state.value.simulationEnabled)
    }

    // -- Agent config update --

    @Test
    fun updateAgentConfigUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        val config = AgentConfig(
            apiKey = "test-key-123456",
            modelId = "sonnet_4_5",
            maxIterations = 50,
            temperature = 0.3f,
        )
        vm.onEvent(SettingsEvent.UpdateAgentConfig(config))
        assertEquals(config, vm.state.value.agentConfig)
    }

    @Test
    fun updateAgentConfigPreservesOtherState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetProtocol(ProtocolType.SACN))
        val config = AgentConfig(apiKey = "key-abcdef", modelId = "sonnet_4")
        vm.onEvent(SettingsEvent.UpdateAgentConfig(config))

        assertEquals(ProtocolType.SACN, vm.state.value.protocol)
        assertEquals(config, vm.state.value.agentConfig)
    }

    // -- Agent connection test --

    @Test
    fun testAgentConnectionGoesToTestingThenSuccess() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        // Set a valid API key (length > 5)
        val config = AgentConfig(apiKey = "valid-api-key-123")
        vm.onEvent(SettingsEvent.UpdateAgentConfig(config))

        vm.onEvent(SettingsEvent.TestAgentConnection)
        // With UnconfinedTestDispatcher, the coroutine starts eagerly up to the first suspension (delay)
        assertIs<AgentStatus.Testing>(vm.state.value.agentStatus)

        // Advance past the 1500ms delay
        advanceTimeBy(1600)

        assertIs<AgentStatus.Success>(vm.state.value.agentStatus)
    }

    @Test
    fun testAgentConnectionFailsWithShortKey() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        // Set an invalid API key (length <= 5)
        val config = AgentConfig(apiKey = "abc")
        vm.onEvent(SettingsEvent.UpdateAgentConfig(config))

        vm.onEvent(SettingsEvent.TestAgentConnection)
        // Advance past the 1500ms delay
        advanceTimeBy(1600)

        val status = vm.state.value.agentStatus
        assertIs<AgentStatus.Error>(status)
        assertEquals("Invalid API Key", status.message)
    }

    @Test
    fun testAgentConnectionTransitionsThroughTesting() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        val config = AgentConfig(apiKey = "valid-api-key-123")
        vm.onEvent(SettingsEvent.UpdateAgentConfig(config))
        vm.onEvent(SettingsEvent.TestAgentConnection)

        // With UnconfinedTestDispatcher, coroutine runs eagerly to first suspension (delay)
        assertIs<AgentStatus.Testing>(vm.state.value.agentStatus)

        // Advance past the 1500ms delay
        advanceTimeBy(1600)

        assertIs<AgentStatus.Success>(vm.state.value.agentStatus)
    }

    // -- Network settings --

    @Test
    fun setPollIntervalUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetPollInterval(5000L))
        assertEquals(5000L, vm.state.value.pollInterval)
    }

    @Test
    fun setManualIpUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetManualIp("10.0.0.1"))
        assertEquals("10.0.0.1", vm.state.value.manualIp)
    }

    @Test
    fun setManualUniverseUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetManualUniverse("3"))
        assertEquals("3", vm.state.value.manualUniverse)
    }

    @Test
    fun setManualStartAddressUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetManualStartAddress("128"))
        assertEquals("128", vm.state.value.manualStartAddress)
    }

    // -- Fixture profiles --

    @Test
    fun addFixtureProfileUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        val profile = FixtureProfile(
            profileId = "par-rgb",
            name = "RGB Par",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("Red", ChannelType.RED, 0),
                Channel("Green", ChannelType.GREEN, 1),
                Channel("Blue", ChannelType.BLUE, 2),
            ),
        )
        vm.onEvent(SettingsEvent.AddFixtureProfile(profile))
        assertEquals(1, vm.state.value.fixtureProfiles.size)
        assertEquals("par-rgb", vm.state.value.fixtureProfiles[0].profileId)
    }

    @Test
    fun deleteFixtureProfileRemovesFromState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        val profile = FixtureProfile(
            profileId = "par-rgb",
            name = "RGB Par",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("Red", ChannelType.RED, 0),
                Channel("Green", ChannelType.GREEN, 1),
                Channel("Blue", ChannelType.BLUE, 2),
            ),
        )
        vm.onEvent(SettingsEvent.AddFixtureProfile(profile))
        assertEquals(1, vm.state.value.fixtureProfiles.size)

        vm.onEvent(SettingsEvent.DeleteFixtureProfile("par-rgb"))
        assertTrue(vm.state.value.fixtureProfiles.isEmpty())
    }

    @Test
    fun deleteNonexistentProfileIsNoOp() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        val profile = FixtureProfile(
            profileId = "par-rgb",
            name = "RGB Par",
            type = FixtureType.PAR,
            channels = emptyList(),
        )
        vm.onEvent(SettingsEvent.AddFixtureProfile(profile))
        vm.onEvent(SettingsEvent.DeleteFixtureProfile("nonexistent-id"))
        assertEquals(1, vm.state.value.fixtureProfiles.size)
    }

    // -- Rig preset --

    @Test
    fun setRigPresetUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetRigPreset(RigPreset.FESTIVAL_STAGE))
        assertEquals(RigPreset.FESTIVAL_STAGE, vm.state.value.selectedRigPreset)
    }

    // -- Reset simulation --

    @Test
    fun resetSimulationDisablesSimAndSwitchesToReal() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, router, _) = createVm(scope = scope)

        // Enable simulation first
        vm.onEvent(SettingsEvent.ToggleSimulation(true))
        assertTrue(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Simulated, router.mode.value)

        // Reset simulation
        vm.onEvent(SettingsEvent.ResetSimulation)
        assertFalse(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Real, router.mode.value)
    }

    // -- Reset onboarding --

    @Test
    fun resetOnboardingPersistsToRepository() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val repo = createSettingsRepository()
        val (vm, _, _) = createVm(settingsRepository = repo, scope = scope)

        // First, mark setup as completed
        repo.setSetupCompleted(true)
        assertTrue(repo.setupCompleted.first())

        // Reset onboarding
        vm.onEvent(SettingsEvent.ResetOnboarding)

        assertFalse(repo.setupCompleted.first())
    }

    // -- State derivation from repository --

    @Test
    fun stateDerivesSimulationFromRepository() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        // Use the test dispatcher for FlowSettings so emissions are synchronous
        val repo = SettingsRepository(MapSettings().toFlowSettings(testDispatcher))
        // Set simulation to true in the repo before creating the VM
        repo.setIsSimulation(true)

        val (vm, _, _) = createVm(settingsRepository = repo, scope = scope)
        // Allow the init collector to receive the initial emission from FlowSettings
        advanceUntilIdle()

        assertTrue(vm.state.value.simulationEnabled)
    }
}

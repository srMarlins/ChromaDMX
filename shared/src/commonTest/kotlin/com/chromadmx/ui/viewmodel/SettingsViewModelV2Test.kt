package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.core.model.ChannelType
import com.chromadmx.core.model.DmxNode
import com.chromadmx.core.model.FixtureProfile
import com.chromadmx.core.model.FixtureType
import com.chromadmx.core.model.Channel
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.networking.ConnectionState
import com.chromadmx.networking.DmxTransport
import com.chromadmx.networking.DmxTransportRouter
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.FixtureDiscoveryRouter
import com.chromadmx.networking.TransportMode
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.ui.state.AgentStatus
import com.chromadmx.ui.state.ProtocolType
import com.chromadmx.ui.state.SettingsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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

/**
 * Fake [FixtureStore] backed by an in-memory list for testing.
 */
private class FakeFixtureStore : FixtureStore {
    private val _fixtures = MutableStateFlow<List<Fixture3D>>(emptyList())
    val savedFixtures: List<Fixture3D> get() = _fixtures.value
    var deleteAllCount = 0; private set

    override fun allFixtures(): Flow<List<Fixture3D>> = _fixtures
    override fun saveFixture(fixture: Fixture3D) {
        _fixtures.value = _fixtures.value + fixture
    }
    override fun saveAll(fixtures: List<Fixture3D>) {
        _fixtures.value = _fixtures.value + fixtures
    }
    override fun deleteFixture(fixtureId: String) {
        _fixtures.value = _fixtures.value.filter { it.fixture.fixtureId != fixtureId }
    }
    override fun deleteAll() {
        deleteAllCount++
        _fixtures.value = emptyList()
    }
}

/**
 * Fake [SettingsStore] backed by MutableStateFlows for in-memory testing.
 */
private class FakeSettingsStore : SettingsStore {
    private val _masterDimmer = MutableStateFlow(1.0f)
    private val _themePreference = MutableStateFlow("MatchaDark")
    private val _isSimulation = MutableStateFlow(false)
    private val _transportMode = MutableStateFlow("Real")
    private val _activePresetId = MutableStateFlow<String?>(null)
    private val _setupCompleted = MutableStateFlow(false)

    override val masterDimmer: Flow<Float> = _masterDimmer
    override val themePreference: Flow<String> = _themePreference
    override val isSimulation: Flow<Boolean> = _isSimulation
    override val transportMode: Flow<String> = _transportMode
    override val activePresetId: Flow<String?> = _activePresetId
    override val setupCompleted: Flow<Boolean> = _setupCompleted

    override suspend fun setMasterDimmer(value: Float) { _masterDimmer.value = value }
    override suspend fun setThemePreference(value: String) { _themePreference.value = value }
    override suspend fun setIsSimulation(value: Boolean) { _isSimulation.value = value }
    override suspend fun setTransportMode(value: String) { _transportMode.value = value }
    override suspend fun setActivePresetId(value: String?) { _activePresetId.value = value }
    override suspend fun setSetupCompleted(value: Boolean) { _setupCompleted.value = value }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelV2Test {

    private fun createSettingsStore(): FakeSettingsStore {
        return FakeSettingsStore()
    }

    private fun createRouter(scope: CoroutineScope): DmxTransportRouter {
        return DmxTransportRouter(
            real = FakeDmxTransport(),
            simulated = FakeDmxTransport(),
            scope = scope,
        )
    }

    private fun createDiscoveryRouter(scope: CoroutineScope): FixtureDiscoveryRouter {
        return FixtureDiscoveryRouter(
            real = FakeFixtureDiscovery(),
            simulated = FakeFixtureDiscovery(),
            scope = scope,
        )
    }

    /**
     * Helper to create VM + routers triple.
     * Uses [UnconfinedTestDispatcher] by default so that launched
     * coroutines (init collectors, scope.launch calls) run eagerly.
     */
    private fun createVm(
        settingsStore: FakeSettingsStore = createSettingsStore(),
        transportRouter: DmxTransportRouter? = null,
        discoveryRouter: FixtureDiscoveryRouter? = null,
        fixtureStore: FakeFixtureStore? = null,
        scope: CoroutineScope,
    ): Triple<SettingsViewModelV2, DmxTransportRouter, FixtureDiscoveryRouter> {
        val router = transportRouter ?: createRouter(scope)
        val discRouter = discoveryRouter ?: createDiscoveryRouter(scope)
        val vm = SettingsViewModelV2(
            settingsRepository = settingsStore,
            transportRouter = router,
            discoveryRouter = discRouter,
            scope = scope,
            fixtureStore = fixtureStore,
        )
        return Triple(vm, router, discRouter)
    }

    /**
     * Wait for async operations dispatched on [kotlinx.coroutines.Dispatchers.IO]
     * to complete. Needed because the test scheduler cannot advance real IO work.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private fun waitForIo() {
        Thread.sleep(200)
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
        // Init seeds built-in fixture profiles
        assertTrue(state.fixtureProfiles.isNotEmpty())
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
    fun toggleSimulationOnSwitchesBothRoutersToSimulated() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, router, discRouter) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.ToggleSimulation(true))

        assertTrue(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Simulated, router.mode.value)
        assertEquals(TransportMode.Simulated, discRouter.mode.value)
    }

    @Test
    fun toggleSimulationOffSwitchesBothRoutersToReal() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, router, discRouter) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.ToggleSimulation(true))
        vm.onEvent(SettingsEvent.ToggleSimulation(false))

        assertFalse(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Real, router.mode.value)
        assertEquals(TransportMode.Real, discRouter.mode.value)
    }

    @Test
    fun toggleSimulationPersistsToStore() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val store = createSettingsStore()
        val (vm, _, _) = createVm(settingsStore = store, scope = scope)

        vm.onEvent(SettingsEvent.ToggleSimulation(true))
        waitForIo()

        assertTrue(store.isSimulation.first())
    }

    // -- Force rescan --

    @Test
    fun forceRescanTriggersDiscoveryStartScan() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val realFake = FakeFixtureDiscovery()
        val discRouter = FixtureDiscoveryRouter(
            real = realFake,
            simulated = FakeFixtureDiscovery(),
            scope = scope,
        )
        val (vm, _, _) = createVm(discoveryRouter = discRouter, scope = scope)

        assertEquals(0, realFake.startScanCount)
        vm.onEvent(SettingsEvent.ForceRescan)
        assertEquals(1, realFake.startScanCount)
    }

    @Test
    fun forceRescanCanBeCalledMultipleTimes() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val realFake = FakeFixtureDiscovery()
        val discRouter = FixtureDiscoveryRouter(
            real = realFake,
            simulated = FakeFixtureDiscovery(),
            scope = scope,
        )
        val (vm, _, _) = createVm(discoveryRouter = discRouter, scope = scope)

        vm.onEvent(SettingsEvent.ForceRescan)
        vm.onEvent(SettingsEvent.ForceRescan)
        vm.onEvent(SettingsEvent.ForceRescan)
        assertEquals(3, realFake.startScanCount)
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

        val baseCount = vm.state.value.fixtureProfiles.size
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
        assertEquals(baseCount + 1, vm.state.value.fixtureProfiles.size)
        assertTrue(vm.state.value.fixtureProfiles.any { it.profileId == "par-rgb" })
    }

    @Test
    fun deleteFixtureProfileRemovesFromState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        val baseCount = vm.state.value.fixtureProfiles.size
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
        assertEquals(baseCount + 1, vm.state.value.fixtureProfiles.size)

        vm.onEvent(SettingsEvent.DeleteFixtureProfile("par-rgb"))
        assertEquals(baseCount, vm.state.value.fixtureProfiles.size)
        assertFalse(vm.state.value.fixtureProfiles.any { it.profileId == "par-rgb" })
    }

    @Test
    fun deleteNonexistentProfileIsNoOp() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        val baseCount = vm.state.value.fixtureProfiles.size
        val profile = FixtureProfile(
            profileId = "par-rgb",
            name = "RGB Par",
            type = FixtureType.PAR,
            channels = emptyList(),
        )
        vm.onEvent(SettingsEvent.AddFixtureProfile(profile))
        vm.onEvent(SettingsEvent.DeleteFixtureProfile("nonexistent-id"))
        assertEquals(baseCount + 1, vm.state.value.fixtureProfiles.size)
    }

    // -- Rig preset --

    @Test
    fun setRigPresetUpdatesState() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, _, _) = createVm(scope = scope)

        vm.onEvent(SettingsEvent.SetRigPreset(RigPreset.FESTIVAL_STAGE))
        assertEquals(RigPreset.FESTIVAL_STAGE, vm.state.value.selectedRigPreset)
    }

    @Test
    fun setRigPresetPersistsFixtures() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val store = FakeFixtureStore()
        val (vm, _, _) = createVm(fixtureStore = store, scope = scope)

        vm.onEvent(SettingsEvent.SetRigPreset(RigPreset.FESTIVAL_STAGE))
        // Persistence runs on Dispatchers.IO — wait for it to complete
        waitForIo()
        advanceUntilIdle()

        assertEquals(1, store.deleteAllCount)
        assertTrue(store.savedFixtures.isNotEmpty())
        assertEquals(RigPreset.FESTIVAL_STAGE, vm.state.value.selectedRigPreset)
    }

    // -- Reset simulation --

    @Test
    fun resetSimulationDisablesSimAndSwitchesBothRoutersToReal() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val (vm, router, discRouter) = createVm(scope = scope)

        // Enable simulation first
        vm.onEvent(SettingsEvent.ToggleSimulation(true))
        assertTrue(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Simulated, router.mode.value)
        assertEquals(TransportMode.Simulated, discRouter.mode.value)

        // Reset simulation
        vm.onEvent(SettingsEvent.ResetSimulation)
        assertFalse(vm.state.value.simulationEnabled)
        assertEquals(TransportMode.Real, router.mode.value)
        assertEquals(TransportMode.Real, discRouter.mode.value)
    }

    // -- Reset onboarding --

    @Test
    fun resetOnboardingPersistsToStore() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val store = createSettingsStore()
        val (vm, _, _) = createVm(settingsStore = store, scope = scope)

        // First, mark setup as completed
        store.setSetupCompleted(true)
        assertTrue(store.setupCompleted.first())

        // Reset onboarding
        vm.onEvent(SettingsEvent.ResetOnboarding)
        // Persistence runs on Dispatchers.IO — wait for it to complete
        waitForIo()

        assertFalse(store.setupCompleted.first())
    }

    // -- State derivation from store --

    @Test
    fun stateDerivesSimulationFromStore() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val store = createSettingsStore()
        // Set simulation to true in the store before creating the VM
        store.setIsSimulation(true)

        val (vm, _, _) = createVm(settingsStore = store, scope = scope)
        // Allow the init collector to receive the initial emission
        advanceUntilIdle()

        assertTrue(vm.state.value.simulationEnabled)
    }
}

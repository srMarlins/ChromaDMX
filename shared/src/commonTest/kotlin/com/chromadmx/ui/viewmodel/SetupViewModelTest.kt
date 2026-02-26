package com.chromadmx.ui.viewmodel

import com.chromadmx.core.model.DmxNode
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Genre
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.ui.state.GenreOption
import com.chromadmx.ui.state.SetupEvent
import com.chromadmx.ui.state.SetupStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    // -- Fakes --

    /**
     * Fake implementation of [FixtureDiscovery] for testing.
     * Allows configuring nodes to emit and controls scanning state.
     */
    private class FakeFixtureDiscovery : FixtureDiscovery {
        private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
        override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

        /** Nodes to emit when startScan() is called. */
        var nodesToEmit: List<DmxNode> = emptyList()

        /** Track how many times startScan was called. */
        var startScanCount = 0
            private set

        /** Track how many times stopScan was called. */
        var stopScanCount = 0
            private set

        override fun startScan() {
            startScanCount++
            _isScanning.value = true
            _discoveredNodes.value = nodesToEmit
        }

        override fun stopScan() {
            stopScanCount++
            _isScanning.value = false
        }

        /** Manually push nodes (simulates async discovery). */
        fun emitNodes(nodes: List<DmxNode>) {
            _discoveredNodes.value = nodes
        }
    }

    /**
     * Fake implementation of [FixtureStore] for testing.
     * Tracks saved fixtures without needing a real database.
     */
    private class FakeFixtureStore : FixtureStore {
        private val _fixtures = MutableStateFlow<List<Fixture3D>>(emptyList())
        var savedFixtures: List<Fixture3D> = emptyList()
            private set
        var deleteAllCalled = false
            private set

        override fun allFixtures(): Flow<List<Fixture3D>> = _fixtures

        override fun saveFixture(fixture: Fixture3D) {
            savedFixtures = savedFixtures + fixture
            _fixtures.value = savedFixtures
        }

        override fun saveAll(fixtures: List<Fixture3D>) {
            savedFixtures = fixtures
            _fixtures.value = fixtures
        }

        override fun deleteAll() {
            deleteAllCalled = true
            savedFixtures = emptyList()
            _fixtures.value = emptyList()
        }

        override fun deleteFixture(fixtureId: String) {
            savedFixtures = savedFixtures.filter { it.fixture.fixtureId != fixtureId }
            _fixtures.value = savedFixtures
        }
    }

    /**
     * Fake implementation of [SettingsStore] for testing.
     * Uses simple in-memory storage.
     */
    private class FakeSettingsStore : SettingsStore {
        private var _setupCompleted = false
        private var _isSimulation = false

        override val setupCompleted: Flow<Boolean> get() = flowOf(_setupCompleted)
        override val isSimulation: Flow<Boolean> get() = flowOf(_isSimulation)
        override val masterDimmer: Flow<Float> get() = flowOf(1f)
        override val themePreference: Flow<String> get() = flowOf("MatchaDark")
        override val transportMode: Flow<String> get() = flowOf("Real")
        override val activePresetId: Flow<String?> get() = flowOf(null)

        override suspend fun setSetupCompleted(value: Boolean) { _setupCompleted = value }
        override suspend fun setIsSimulation(value: Boolean) { _isSimulation = value }
        override suspend fun setMasterDimmer(value: Float) {}
        override suspend fun setThemePreference(value: String) {}
        override suspend fun setTransportMode(value: String) {}
        override suspend fun setActivePresetId(value: String?) {}

        // Accessors for test assertions
        val setupCompletedValue get() = _setupCompleted
        val isSimulationValue get() = _isSimulation
    }

    // -- Test helpers --

    /**
     * Create a ViewModel with an UnconfinedTestDispatcher-based scope so
     * that init coroutines (flow collection) run eagerly and synchronously.
     */
    private fun createVm(
        discovery: FakeFixtureDiscovery = FakeFixtureDiscovery(),
        fixtureStore: FakeFixtureStore = FakeFixtureStore(),
        settingsStore: FakeSettingsStore = FakeSettingsStore(),
        scope: CoroutineScope,
    ): SetupViewModel {
        return SetupViewModel(
            fixtureDiscovery = discovery,
            fixtureStore = fixtureStore,
            settingsStore = settingsStore,
            scope = scope,
        )
    }

    /**
     * Build a [CoroutineScope] backed by [UnconfinedTestDispatcher] so
     * launched coroutines execute eagerly (no advanceUntilIdle needed).
     */
    private fun unconfinedScope(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler): CoroutineScope {
        return CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
    }

    // -- Initial state tests --

    @Test
    fun initialStateHasCorrectDefaults() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))
        val state = vm.state.first()
        assertEquals(SetupStep.SPLASH, state.currentStep)
        assertTrue(state.discoveredNodes.isEmpty())
        // isScanning is true because init auto-starts scan
        assertTrue(state.isScanning)
        assertFalse(state.isSimulationMode)
        assertEquals(RigPreset.SMALL_DJ, state.selectedRigPreset)
        assertEquals(0, state.simulationFixtureCount)
        assertNull(state.selectedGenre)
        assertFalse(state.isGenerating)
        assertEquals(0f, state.generationProgress)
        assertNull(state.generationError)
    }

    // -- Scenario 1: scan starts automatically and discovers nodes --

    @Test
    fun scanStartsAutomaticallyOnInit() = runTest {
        val discovery = FakeFixtureDiscovery()
        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        // startScan should have been called during init
        assertEquals(1, discovery.startScanCount)
        // The isScanning flow should have propagated
        assertTrue(vm.state.value.isScanning)
    }

    @Test
    fun discoveredNodesAppearInState() = runTest {
        val discovery = FakeFixtureDiscovery()
        val nodes = listOf(
            DmxNode(ipAddress = "192.168.1.10", shortName = "Par1"),
            DmxNode(ipAddress = "192.168.1.11", shortName = "Par2"),
        )
        discovery.nodesToEmit = nodes

        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        assertEquals(2, vm.state.value.discoveredNodes.size)
        assertEquals("192.168.1.10", vm.state.value.discoveredNodes[0].ipAddress)
        assertEquals("192.168.1.11", vm.state.value.discoveredNodes[1].ipAddress)
    }

    @Test
    fun lateDiscoveryUpdatesState() = runTest {
        val discovery = FakeFixtureDiscovery()
        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        // Initially no nodes
        assertTrue(vm.state.value.discoveredNodes.isEmpty())

        // Nodes arrive later
        val nodes = listOf(DmxNode(ipAddress = "10.0.0.1", shortName = "LED1"))
        discovery.emitNodes(nodes)

        assertEquals(1, vm.state.value.discoveredNodes.size)
        assertEquals("10.0.0.1", vm.state.value.discoveredNodes[0].ipAddress)
    }

    // -- Scenario 2: entering simulation mode --

    @Test
    fun enterSimulationModeSetsFlag() = runTest {
        val discovery = FakeFixtureDiscovery()
        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        // Move to NetworkDiscovery first
        vm.onEvent(SetupEvent.Advance)
        assertEquals(SetupStep.NETWORK_DISCOVERY, vm.state.value.currentStep)

        vm.onEvent(SetupEvent.EnterSimulationMode)

        assertTrue(vm.state.value.isSimulationMode)
    }

    @Test
    fun enterSimulationModeStopsScanAndAdvances() = runTest {
        val discovery = FakeFixtureDiscovery()
        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        vm.onEvent(SetupEvent.Advance) // -> NETWORK_DISCOVERY
        vm.onEvent(SetupEvent.EnterSimulationMode)

        // Should have stopped scanning
        assertTrue(discovery.stopScanCount > 0)
        assertFalse(vm.state.value.isScanning)
        // Should have advanced to FIXTURE_SCAN
        assertEquals(SetupStep.FIXTURE_SCAN, vm.state.value.currentStep)
    }

    // -- Scenario 3: selecting rig preset updates fixture count --

    @Test
    fun selectRigPresetUpdatesCount() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        vm.onEvent(SetupEvent.SelectRigPreset(RigPreset.SMALL_DJ))

        assertEquals(RigPreset.SMALL_DJ, vm.state.value.selectedRigPreset)
        assertTrue(vm.state.value.simulationFixtureCount > 0)
    }

    @Test
    fun selectClubPresetUpdatesFixtureCount() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        vm.onEvent(SetupEvent.SelectRigPreset(RigPreset.TRUSS_RIG))

        assertEquals(RigPreset.TRUSS_RIG, vm.state.value.selectedRigPreset)
        assertTrue(vm.state.value.simulationFixtureCount > 0)
    }

    @Test
    fun selectFestivalPresetHasMoreFixtures() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        vm.onEvent(SetupEvent.SelectRigPreset(RigPreset.SMALL_DJ))
        val smallCount = vm.state.value.simulationFixtureCount

        vm.onEvent(SetupEvent.SelectRigPreset(RigPreset.FESTIVAL_STAGE))
        val festivalCount = vm.state.value.simulationFixtureCount

        assertTrue(festivalCount > smallCount)
    }

    // -- Scenario 4: proceeding saves fixtures and signals completion --

    @Test
    fun advancingThroughFullFlowReachesComplete() = runTest {
        val discovery = FakeFixtureDiscovery()
        val settingsStore = FakeSettingsStore()
        val vm = createVm(
            discovery = discovery,
            settingsStore = settingsStore,
            scope = unconfinedScope(testScheduler),
        )

        // SPLASH -> NETWORK_DISCOVERY
        vm.onEvent(SetupEvent.Advance)
        assertEquals(SetupStep.NETWORK_DISCOVERY, vm.state.value.currentStep)

        // Enter simulation mode -> FIXTURE_SCAN
        vm.onEvent(SetupEvent.EnterSimulationMode)
        assertEquals(SetupStep.FIXTURE_SCAN, vm.state.value.currentStep)

        // FIXTURE_SCAN -> VIBE_CHECK
        vm.onEvent(SetupEvent.Advance)
        assertEquals(SetupStep.VIBE_CHECK, vm.state.value.currentStep)

        // VIBE_CHECK -> STAGE_PREVIEW
        vm.onEvent(SetupEvent.Advance)
        assertEquals(SetupStep.STAGE_PREVIEW, vm.state.value.currentStep)

        // STAGE_PREVIEW -> COMPLETE (marks setup as done)
        vm.onEvent(SetupEvent.Advance)
        assertEquals(SetupStep.COMPLETE, vm.state.value.currentStep)

        // Settings should have been persisted
        assertTrue(settingsStore.setupCompletedValue)
    }

    @Test
    fun skipToCompletePersistsSettings() = runTest {
        val settingsStore = FakeSettingsStore()
        val vm = createVm(settingsStore = settingsStore, scope = unconfinedScope(testScheduler))

        vm.onEvent(SetupEvent.SkipToComplete)

        assertEquals(SetupStep.COMPLETE, vm.state.value.currentStep)
        assertTrue(settingsStore.setupCompletedValue)
    }

    @Test
    fun advanceAtStagePreviewSavesSimulationFixtures() = runTest {
        val discovery = FakeFixtureDiscovery()
        val fixtureStore = FakeFixtureStore()
        val settingsStore = FakeSettingsStore()
        val vm = createVm(
            discovery = discovery,
            fixtureStore = fixtureStore,
            settingsStore = settingsStore,
            scope = unconfinedScope(testScheduler),
        )

        // Navigate: SPLASH -> NETWORK_DISCOVERY -> sim mode -> FIXTURE_SCAN -> VIBE_CHECK -> STAGE_PREVIEW
        vm.onEvent(SetupEvent.Advance) // -> NETWORK_DISCOVERY
        vm.onEvent(SetupEvent.EnterSimulationMode) // -> FIXTURE_SCAN
        vm.onEvent(SetupEvent.SelectRigPreset(RigPreset.SMALL_DJ))
        vm.onEvent(SetupEvent.Advance) // -> VIBE_CHECK
        vm.onEvent(SetupEvent.Advance) // -> STAGE_PREVIEW
        assertEquals(SetupStep.STAGE_PREVIEW, vm.state.value.currentStep)

        // STAGE_PREVIEW -> COMPLETE should persist fixtures
        vm.onEvent(SetupEvent.Advance)

        assertEquals(SetupStep.COMPLETE, vm.state.value.currentStep)
        // Fixtures should have been saved since we were in simulation mode
        assertTrue(fixtureStore.savedFixtures.isNotEmpty())
        assertTrue(settingsStore.isSimulationValue)
    }

    // -- Scenario 5: retry scan restarts discovery --

    @Test
    fun retryNetworkScanRestartsScanning() = runTest {
        val discovery = FakeFixtureDiscovery()
        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        // Move to NETWORK_DISCOVERY
        vm.onEvent(SetupEvent.Advance)

        val scanCountBefore = discovery.startScanCount
        vm.onEvent(SetupEvent.RetryNetworkScan)

        assertTrue(discovery.startScanCount > scanCountBefore)
        assertTrue(vm.state.value.isScanning)
    }

    @Test
    fun retryNetworkScanStopsExistingScan() = runTest {
        val discovery = FakeFixtureDiscovery()
        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        vm.onEvent(SetupEvent.Advance) // -> NETWORK_DISCOVERY
        vm.onEvent(SetupEvent.RetryNetworkScan)

        // Should have called stopScan before restarting
        assertTrue(discovery.stopScanCount > 0)
    }

    // -- Scenario 6: canProceed / step transition logic --

    @Test
    fun advanceFromSplashGoesToNetworkDiscovery() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))
        assertEquals(SetupStep.SPLASH, vm.state.value.currentStep)

        vm.onEvent(SetupEvent.Advance)
        assertEquals(SetupStep.NETWORK_DISCOVERY, vm.state.value.currentStep)
    }

    @Test
    fun advanceBeyondCompleteIsNoOp() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        // Advance through all steps
        repeat(6) { vm.onEvent(SetupEvent.Advance) }
        assertEquals(SetupStep.COMPLETE, vm.state.value.currentStep)

        // Another advance should be a no-op
        vm.onEvent(SetupEvent.Advance)
        assertEquals(SetupStep.COMPLETE, vm.state.value.currentStep)
    }

    @Test
    fun stepsAdvanceInOrder() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        val expectedSteps = listOf(
            SetupStep.SPLASH,
            SetupStep.NETWORK_DISCOVERY,
            SetupStep.FIXTURE_SCAN,
            SetupStep.VIBE_CHECK,
            SetupStep.STAGE_PREVIEW,
            SetupStep.COMPLETE,
        )

        assertEquals(expectedSteps[0], vm.state.value.currentStep)
        for (i in 1 until expectedSteps.size) {
            vm.onEvent(SetupEvent.Advance)
            assertEquals(expectedSteps[i], vm.state.value.currentStep)
        }
    }

    // -- Genre selection --

    @Test
    fun selectGenreUpdatesState() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        val genre = GenreOption("techno", "Techno", 0xFFFF0040, Genre.TECHNO)
        vm.onEvent(SetupEvent.SelectGenre(genre))

        assertEquals(genre, vm.state.value.selectedGenre)
    }

    @Test
    fun confirmGenreAdvancesStep() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        // Navigate to VIBE_CHECK
        vm.onEvent(SetupEvent.Advance) // -> NETWORK_DISCOVERY
        vm.onEvent(SetupEvent.Advance) // -> FIXTURE_SCAN
        vm.onEvent(SetupEvent.Advance) // -> VIBE_CHECK
        assertEquals(SetupStep.VIBE_CHECK, vm.state.value.currentStep)

        val genre = GenreOption("house", "House", 0xFFFF8800, Genre.HOUSE)
        vm.onEvent(SetupEvent.SelectGenre(genre))
        vm.onEvent(SetupEvent.ConfirmGenre)

        assertEquals(SetupStep.STAGE_PREVIEW, vm.state.value.currentStep)
    }

    // -- SkipStagePreview --

    @Test
    fun skipStagePreviewAdvancesToComplete() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        // Navigate to STAGE_PREVIEW
        repeat(4) { vm.onEvent(SetupEvent.Advance) }
        assertEquals(SetupStep.STAGE_PREVIEW, vm.state.value.currentStep)

        vm.onEvent(SetupEvent.SkipStagePreview)

        assertEquals(SetupStep.COMPLETE, vm.state.value.currentStep)
    }

    // -- Available genres --

    @Test
    fun availableGenresContainsAllEightGenres() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        assertEquals(8, vm.state.value.availableGenres.size)
    }

    @Test
    fun availableGenresMatchCoreGenreEnum() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        val coreGenres = Genre.entries.toSet()
        val vmGenres = vm.state.value.availableGenres.mapNotNull { it.genre }.toSet()
        assertEquals(coreGenres, vmGenres)
    }

    // -- PerformRepeatLaunchCheck --

    @Test
    fun repeatLaunchCheckDefaults() = runTest {
        val vm = createVm(scope = unconfinedScope(testScheduler))

        assertFalse(vm.state.value.networkChangedSinceLastLaunch)
        assertFalse(vm.state.value.repeatLaunchCheckComplete)
    }

    @Test
    fun performRepeatLaunchCheckCompletesWithoutNodes() = runTest {
        val discovery = FakeFixtureDiscovery()
        val vm = createVm(discovery = discovery, scope = unconfinedScope(testScheduler))

        vm.onEvent(SetupEvent.PerformRepeatLaunchCheck)

        assertTrue(vm.state.value.repeatLaunchCheckComplete)
    }
}

package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.scene.Scene
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.DmxNode
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.transport.PlatformUdpTransport
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.ui.state.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for StageViewModelV2 (sliced-state architecture).
 *
 * Validates that each of the 5 state slices (PerformanceState, FixtureState,
 * PresetState, NetworkState, ViewState) is updated correctly in response to
 * StageEvent dispatches.
 */
class StageViewModelSlicedTest {

    // ── Fakes ──────────────────────────────────────────────────────────

    private class FakeBeatClock : BeatClock {
        private val _bpm = MutableStateFlow(120f)
        override val bpm: StateFlow<Float> = _bpm
        private val _beatPhase = MutableStateFlow(0f)
        override val beatPhase: StateFlow<Float> = _beatPhase
        private val _barPhase = MutableStateFlow(0f)
        override val barPhase: StateFlow<Float> = _barPhase
        private val _isRunning = MutableStateFlow(false)
        override val isRunning: StateFlow<Boolean> = _isRunning
        private val _beatState = MutableStateFlow(BeatState.IDLE)
        override val beatState: StateFlow<BeatState> = _beatState
        override fun start() { _isRunning.value = true }
        override fun stop() { _isRunning.value = false }
        fun setBpm(bpm: Float) {
            _bpm.value = bpm
            _beatState.value = BeatState(bpm, 0f, 0f, 0f)
        }

        var tapCount = 0
            private set

        fun tap() { tapCount++ }
    }

    private class FakeFixtureDiscovery : FixtureDiscovery {
        private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
        override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes
        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning
        var scanStarted = false; private set
        override fun startScan() { scanStarted = true; _isScanning.value = true }
        override fun stopScan() { _isScanning.value = false }
        fun setNodes(nodes: List<DmxNode>) { _discoveredNodes.value = nodes }
    }

    private val fakeStorage = object : FileStorage {
        private val files = mutableMapOf<String, String>()
        override fun saveFile(path: String, content: String) { files[path] = content }
        override fun readFile(path: String): String? = files[path]
        override fun deleteFile(path: String): Boolean = files.remove(path) != null
        override fun listFiles(directory: String): List<String> =
            files.keys.filter { it.startsWith(directory) }.map { it.substringAfterLast("/") }
        override fun exists(path: String): Boolean = files.containsKey(path)
        override fun mkdirs(directory: String) {}
    }

    private val registry = EffectRegistry().apply { register(SolidColorEffect()) }

    private fun makeFixtures(count: Int): List<Fixture3D> = (0 until count).map { i ->
        Fixture3D(
            fixture = Fixture("f$i", "Fixture $i", i * 3, 3, 0),
            position = Vec3(i.toFloat(), 0f, 0f)
        )
    }

    private fun createVm(
        fixtures: List<Fixture3D> = emptyList(),
        beatClock: BeatClock = FakeBeatClock(),
        discovery: FixtureDiscovery = FakeFixtureDiscovery(),
        scope: kotlinx.coroutines.CoroutineScope,
    ): Triple<StageViewModelV2, EffectEngine, PresetLibrary> {
        val engine = EffectEngine(scope, fixtures)
        val presetLibrary = PresetLibrary(fakeStorage, registry, engine.effectStack)
        val vm = StageViewModelV2(
            engine = engine,
            effectRegistry = registry,
            presetLibrary = presetLibrary,
            beatClock = beatClock,
            fixtureDiscovery = discovery,
            nodeDiscovery = null,
            scope = scope,
            fixtureRepository = null,
            fixtureController = null,
        )
        return Triple(vm, engine, presetLibrary)
    }

    // ── PerformanceState tests ─────────────────────────────────────────

    @Test
    fun setMasterDimmerUpdatesPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertEquals(1.0f, vm.performanceState.value.masterDimmer)

        vm.onEvent(StageEvent.SetMasterDimmer(0.5f))
        assertEquals(0.5f, vm.performanceState.value.masterDimmer)
    }

    @Test
    fun setMasterDimmerClamps() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.SetMasterDimmer(2.0f))
        assertEquals(1.0f, vm.performanceState.value.masterDimmer)

        vm.onEvent(StageEvent.SetMasterDimmer(-0.5f))
        assertEquals(0.0f, vm.performanceState.value.masterDimmer)
    }

    @Test
    fun setEffectUpdatesLayers() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertTrue(vm.performanceState.value.layers.isEmpty())

        vm.onEvent(StageEvent.SetEffect(0, "solid-color", EffectParams.EMPTY.with("color", Color.RED)))
        assertEquals(1, vm.performanceState.value.layers.size)
    }

    @Test
    fun addLayerUpdatesPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.AddLayer)
        assertEquals(1, vm.performanceState.value.layers.size)
    }

    @Test
    fun removeLayerUpdatesPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.AddLayer)
        assertEquals(1, vm.performanceState.value.layers.size)
        vm.onEvent(StageEvent.RemoveLayer(0))
        assertTrue(vm.performanceState.value.layers.isEmpty())
    }

    @Test
    fun setLayerOpacityUpdatesPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.AddLayer)
        vm.onEvent(StageEvent.SetLayerOpacity(0, 0.3f))
        assertEquals(0.3f, vm.performanceState.value.layers[0].opacity, 0.001f)
    }

    @Test
    fun setLayerBlendModeUpdatesPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.AddLayer)
        vm.onEvent(StageEvent.SetLayerBlendMode(0, BlendMode.ADDITIVE))
        assertEquals(BlendMode.ADDITIVE, vm.performanceState.value.layers[0].blendMode)
    }

    @Test
    fun toggleLayerEnabledUpdatesPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.AddLayer)
        assertTrue(vm.performanceState.value.layers[0].enabled)
        vm.onEvent(StageEvent.ToggleLayerEnabled(0))
        assertFalse(vm.performanceState.value.layers[0].enabled)
    }

    @Test
    fun reorderLayerUpdatesPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        // Add two layers
        vm.onEvent(StageEvent.SetEffect(0, "solid-color", EffectParams.EMPTY.with("color", Color.RED)))
        vm.onEvent(StageEvent.SetEffect(1, "solid-color", EffectParams.EMPTY.with("color", Color.BLUE)))
        assertEquals(2, vm.performanceState.value.layers.size)
        vm.onEvent(StageEvent.ReorderLayer(0, 1))
        // After reorder, layers should be swapped
        assertEquals(2, vm.performanceState.value.layers.size)
    }

    // ── FixtureState tests ─────────────────────────────────────────────

    @Test
    fun selectFixtureUpdatesFixtureState() = runTest {
        val fixtures = makeFixtures(3)
        val (vm, _, _) = createVm(fixtures = fixtures, scope = backgroundScope)
        assertNull(vm.fixtureState.value.selectedFixtureIndex)

        vm.onEvent(StageEvent.SelectFixture(1))
        assertEquals(1, vm.fixtureState.value.selectedFixtureIndex)

        vm.onEvent(StageEvent.SelectFixture(null))
        assertNull(vm.fixtureState.value.selectedFixtureIndex)
    }

    @Test
    fun toggleEditModeUpdatesFixtureState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertFalse(vm.fixtureState.value.isEditMode)

        vm.onEvent(StageEvent.ToggleEditMode)
        assertTrue(vm.fixtureState.value.isEditMode)

        vm.onEvent(StageEvent.ToggleEditMode)
        assertFalse(vm.fixtureState.value.isEditMode)
    }

    @Test
    fun addFixtureUpdatesFixtureState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertTrue(vm.fixtureState.value.fixtures.isEmpty())

        val fixture = Fixture3D(
            fixture = Fixture("new", "New", 0, 3, 0),
            position = Vec3.ZERO
        )
        vm.onEvent(StageEvent.AddFixture(fixture))
        assertEquals(1, vm.fixtureState.value.fixtures.size)
    }

    @Test
    fun removeFixtureUpdatesFixtureState() = runTest {
        val fixtures = makeFixtures(2)
        val (vm, _, _) = createVm(fixtures = fixtures, scope = backgroundScope)
        assertEquals(2, vm.fixtureState.value.fixtures.size)

        vm.onEvent(StageEvent.RemoveFixture(0))
        assertEquals(1, vm.fixtureState.value.fixtures.size)
    }

    @Test
    fun removeFixtureClearsSelectionIfSelected() = runTest {
        val fixtures = makeFixtures(2)
        val (vm, _, _) = createVm(fixtures = fixtures, scope = backgroundScope)
        vm.onEvent(StageEvent.SelectFixture(0))
        assertEquals(0, vm.fixtureState.value.selectedFixtureIndex)

        vm.onEvent(StageEvent.RemoveFixture(0))
        assertNull(vm.fixtureState.value.selectedFixtureIndex)
    }

    @Test
    fun updateFixturePositionUpdatesFixtureState() = runTest {
        val fixtures = makeFixtures(2)
        val (vm, _, _) = createVm(fixtures = fixtures, scope = backgroundScope)

        val newPos = Vec3(5f, 10f, 3f)
        vm.onEvent(StageEvent.UpdateFixturePosition(0, newPos))
        assertEquals(newPos, vm.fixtureState.value.fixtures[0].position)
    }

    @Test
    fun updateZHeightUpdatesFixtureState() = runTest {
        val fixtures = makeFixtures(3)
        val (vm, _, _) = createVm(fixtures = fixtures, scope = backgroundScope)

        assertEquals(0f, vm.fixtureState.value.fixtures[1].position.z)
        vm.onEvent(StageEvent.UpdateZHeight(1, 3.5f))
        assertEquals(3.5f, vm.fixtureState.value.fixtures[1].position.z)
        // x,y unchanged
        assertEquals(1f, vm.fixtureState.value.fixtures[1].position.x)
        assertEquals(0f, vm.fixtureState.value.fixtures[1].position.y)
    }

    @Test
    fun assignGroupUpdatesFixtureState() = runTest {
        val fixtures = makeFixtures(2)
        val (vm, _, _) = createVm(fixtures = fixtures, scope = backgroundScope)

        assertNull(vm.fixtureState.value.fixtures[0].groupId)
        vm.onEvent(StageEvent.AssignGroup(0, "grp-truss"))
        assertEquals("grp-truss", vm.fixtureState.value.fixtures[0].groupId)
        // Other fixture unchanged
        assertNull(vm.fixtureState.value.fixtures[1].groupId)
    }

    @Test
    fun unassignGroupUpdatesFixtureState() = runTest {
        val fixtures = makeFixtures(1)
        val (vm, _, _) = createVm(fixtures = fixtures, scope = backgroundScope)

        vm.onEvent(StageEvent.AssignGroup(0, "grp-truss"))
        assertEquals("grp-truss", vm.fixtureState.value.fixtures[0].groupId)
        vm.onEvent(StageEvent.AssignGroup(0, null))
        assertNull(vm.fixtureState.value.fixtures[0].groupId)
    }

    @Test
    fun createGroupUpdatesFixtureState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertTrue(vm.fixtureState.value.groups.isEmpty())

        vm.onEvent(StageEvent.CreateGroup("Truss Left", 0xFF00FF00))
        assertEquals(1, vm.fixtureState.value.groups.size)
        assertEquals("Truss Left", vm.fixtureState.value.groups[0].name)
        assertEquals(0xFF00FF00, vm.fixtureState.value.groups[0].color)
    }

    @Test
    fun deleteGroupUpdatesFixtureState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)

        vm.onEvent(StageEvent.CreateGroup("Test Group", 0xFFFFFFFF))
        val groupId = vm.fixtureState.value.groups[0].groupId
        assertEquals(1, vm.fixtureState.value.groups.size)

        vm.onEvent(StageEvent.DeleteGroup(groupId))
        assertTrue(vm.fixtureState.value.groups.isEmpty())
    }

    // ── PresetState tests ──────────────────────────────────────────────

    @Test
    fun presetStateContainsAvailableEffects() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertTrue(vm.presetState.value.availableEffects.contains("solid-color"))
    }

    @Test
    fun presetStateContainsGenres() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertTrue(vm.presetState.value.availableGenres.isNotEmpty())
    }

    // ── NetworkState tests ─────────────────────────────────────────────

    @Test
    fun toggleNodeListUpdatesNetworkState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertFalse(vm.networkState.value.isNodeListOpen)

        vm.onEvent(StageEvent.ToggleNodeList)
        assertTrue(vm.networkState.value.isNodeListOpen)

        vm.onEvent(StageEvent.ToggleNodeList)
        assertFalse(vm.networkState.value.isNodeListOpen)
    }

    @Test
    fun diagnoseNodeClosesNodeList() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.ToggleNodeList)
        assertTrue(vm.networkState.value.isNodeListOpen)

        val dummyNode = DmxNode(
            ipAddress = "10.0.0.1",
            macAddress = "AA:BB:CC:DD:EE:FF",
            shortName = "Test",
            longName = "Test Node",
            firmwareVersion = 1,
            numPorts = 1,
            universes = listOf(0),
            style = 0,
            lastSeenMs = 0,
            firstSeenMs = 0,
            latencyMs = 0
        )
        vm.onEvent(StageEvent.DiagnoseNode(dummyNode))
        assertFalse(vm.networkState.value.isNodeListOpen)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun networkStateReceivesDiscoveredNodes() = runTest {
        val discovery = FakeFixtureDiscovery()
        val (vm, _, _) = createVm(discovery = discovery, scope = backgroundScope)

        val node = DmxNode(
            ipAddress = "10.0.0.1",
            macAddress = "AA:BB:CC:DD:EE:FF",
            shortName = "Node1",
            longName = "Test Node 1",
            firmwareVersion = 1,
            numPorts = 1,
            universes = listOf(0),
            style = 0,
            lastSeenMs = 1000,
            firstSeenMs = 1000,
            latencyMs = 5
        )
        discovery.setNodes(listOf(node))

        // Advance virtual time so collectors in backgroundScope process the emission.
        // Use runCurrent() to dispatch pending coroutines without advancing
        // through infinite delay loops in sync jobs.
        testScheduler.runCurrent()

        assertEquals(1, vm.networkState.value.nodes.size)
        assertEquals("Node1", vm.networkState.value.nodes[0].shortName)
    }

    // ── ViewState tests ────────────────────────────────────────────────

    @Test
    fun toggleViewModeCyclesToIso() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertEquals(ViewMode.TOP_DOWN, vm.viewState.value.mode)

        vm.onEvent(StageEvent.ToggleViewMode)
        assertEquals(ViewMode.ISO, vm.viewState.value.mode)
    }

    @Test
    fun toggleViewModeCyclesThrough() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertEquals(ViewMode.TOP_DOWN, vm.viewState.value.mode)

        vm.onEvent(StageEvent.ToggleViewMode)
        assertEquals(ViewMode.ISO, vm.viewState.value.mode)

        vm.onEvent(StageEvent.ToggleViewMode)
        assertEquals(ViewMode.AUDIENCE, vm.viewState.value.mode)

        vm.onEvent(StageEvent.ToggleViewMode)
        assertEquals(ViewMode.TOP_DOWN, vm.viewState.value.mode)
    }

    @Test
    fun setIsoAngleUpdatesViewState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertEquals(IsoAngle.FORTY_FIVE, vm.viewState.value.isoAngle)

        vm.onEvent(StageEvent.SetIsoAngle(IsoAngle.NINETY))
        assertEquals(IsoAngle.NINETY, vm.viewState.value.isoAngle)
    }

    @Test
    fun enableSimulationUpdatesViewState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        assertFalse(vm.viewState.value.isSimulationMode)

        vm.onEvent(StageEvent.EnableSimulation("Club Rig", 8))
        assertTrue(vm.viewState.value.isSimulationMode)
        assertEquals("Club Rig", vm.viewState.value.simulationPresetName)
        assertEquals(8, vm.viewState.value.simulationFixtureCount)
    }

    @Test
    fun disableSimulationUpdatesViewState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)

        vm.onEvent(StageEvent.EnableSimulation("Club Rig", 8))
        assertTrue(vm.viewState.value.isSimulationMode)

        vm.onEvent(StageEvent.DisableSimulation)
        assertFalse(vm.viewState.value.isSimulationMode)
        assertNull(vm.viewState.value.simulationPresetName)
        assertEquals(0, vm.viewState.value.simulationFixtureCount)
    }

    // ── Cross-slice isolation tests ────────────────────────────────────

    @Test
    fun fixtureEventDoesNotAffectPerformanceState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        val perfBefore = vm.performanceState.value

        vm.onEvent(StageEvent.SelectFixture(0))
        vm.onEvent(StageEvent.ToggleEditMode)

        // Performance state should not change
        assertEquals(perfBefore, vm.performanceState.value)
    }

    @Test
    fun viewEventDoesNotAffectNetworkState() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        val netBefore = vm.networkState.value

        vm.onEvent(StageEvent.ToggleViewMode)
        vm.onEvent(StageEvent.SetIsoAngle(IsoAngle.NINETY))

        // Network state should not change
        assertEquals(netBefore, vm.networkState.value)
    }

    // ── Out-of-bounds safety tests ─────────────────────────────────────

    @Test
    fun updateZHeightOutOfBoundsNoOp() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.UpdateZHeight(99, 3f))
        assertTrue(vm.fixtureState.value.fixtures.isEmpty())
    }

    @Test
    fun assignGroupOutOfBoundsNoOp() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        vm.onEvent(StageEvent.AssignGroup(99, "grp-test"))
        assertTrue(vm.fixtureState.value.fixtures.isEmpty())
    }

    @Test
    fun removeLayerOutOfBoundsNoOp() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        // Should not crash
        vm.onEvent(StageEvent.RemoveLayer(99))
        assertTrue(vm.performanceState.value.layers.isEmpty())
    }

    @Test
    fun setLayerOpacityOutOfBoundsNoOp() = runTest {
        val (vm, _, _) = createVm(scope = backgroundScope)
        // Should not crash
        vm.onEvent(StageEvent.SetLayerOpacity(99, 0.5f))
        assertTrue(vm.performanceState.value.layers.isEmpty())
    }
}

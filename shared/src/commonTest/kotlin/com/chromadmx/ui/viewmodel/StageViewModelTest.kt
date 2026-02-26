package com.chromadmx.ui.viewmodel

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.transport.PlatformUdpTransport
import com.chromadmx.tempo.clock.BeatClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StageViewModelTest {
    private fun makeFixtures(count: Int): List<Fixture3D> = (0 until count).map { i ->
        Fixture3D(
            fixture = Fixture("f$i", "Fixture $i", i * 3, 3, 0),
            position = Vec3(i.toFloat(), 0f, 0f)
        )
    }

    private val registry = EffectRegistry().apply { register(SolidColorEffect()) }

    private val stubBeatClock = object : BeatClock {
        override val beatState: StateFlow<BeatState> = MutableStateFlow(BeatState.IDLE)
        override val isRunning: StateFlow<Boolean> = MutableStateFlow(false)
        override val bpm: StateFlow<Float> = MutableStateFlow(120f)
        override val beatPhase: StateFlow<Float> = MutableStateFlow(0f)
        override val barPhase: StateFlow<Float> = MutableStateFlow(0f)
        override fun start() {}
        override fun stop() {}
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

    private fun makePresetLibrary(engine: EffectEngine): PresetLibrary =
        PresetLibrary(fakeStorage, registry, engine.effectStack)

    private fun stubNodeDiscovery() = NodeDiscovery(PlatformUdpTransport())

    @Test
    fun exposesFixturesList() = runTest {
        val fixtures = makeFixtures(4)
        val engine = EffectEngine(backgroundScope, fixtures)
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)
        assertEquals(4, vm.fixtures.value.size)
    }

    @Test
    fun exposesMasterDimmer() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)
        assertEquals(1.0f, vm.masterDimmer.value)
        vm.setMasterDimmer(0.5f)
        assertEquals(0.5f, vm.masterDimmer.value)
    }

    @Test
    fun exposesEffectLayers() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)
        assertTrue(vm.layers.value.isEmpty())
        vm.setEffect(0, "solid-color", EffectParams.EMPTY.with("color", Color.RED))
        assertEquals(1, vm.layers.value.size)
    }

    @Test
    fun addAndRemoveFixture() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)
        val fixture = Fixture3D(
            fixture = Fixture("new", "New", 0, 3, 0),
            position = Vec3.ZERO
        )
        vm.addFixture(fixture)
        assertEquals(1, vm.fixtures.value.size)
        vm.removeFixture(0)
        assertEquals(0, vm.fixtures.value.size)
    }

    // --- Edit mode tests ---

    @Test
    fun editModeToggle() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)
        assertFalse(vm.isEditMode.value)
        vm.toggleEditMode()
        assertTrue(vm.isEditMode.value)
        vm.toggleEditMode()
        assertFalse(vm.isEditMode.value)
    }

    @Test
    fun updateZHeight() = runTest {
        val fixtures = makeFixtures(3)
        val engine = EffectEngine(backgroundScope, fixtures)
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)

        assertEquals(0f, vm.fixtures.value[1].position.z)
        vm.updateZHeight(1, 3.5f)
        assertEquals(3.5f, vm.fixtures.value[1].position.z)
        // x,y should remain unchanged
        assertEquals(1f, vm.fixtures.value[1].position.x)
        assertEquals(0f, vm.fixtures.value[1].position.y)
    }

    @Test
    fun updateFixturePositionPersistsInMemory() = runTest {
        val fixtures = makeFixtures(2)
        val engine = EffectEngine(backgroundScope, fixtures)
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)

        val newPos = Vec3(5f, 10f, 3f)
        vm.updateFixturePosition(0, newPos)
        assertEquals(newPos, vm.fixtures.value[0].position)
    }

    // --- Group management tests ---

    @Test
    fun createGroup() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)

        assertTrue(vm.groups.value.isEmpty())
        val groupId = vm.createGroup("Truss Left", 0xFF00FF00)
        assertEquals(1, vm.groups.value.size)
        assertEquals("Truss Left", vm.groups.value[0].name)
        assertEquals(0xFF00FF00, vm.groups.value[0].color)
        assertTrue(groupId.startsWith("grp-"))
    }

    @Test
    fun deleteGroup() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)

        val groupId = vm.createGroup("Test Group")
        assertEquals(1, vm.groups.value.size)
        vm.deleteGroup(groupId)
        assertTrue(vm.groups.value.isEmpty())
    }

    @Test
    fun assignGroupToFixture() = runTest {
        val fixtures = makeFixtures(2)
        val engine = EffectEngine(backgroundScope, fixtures)
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)

        assertNull(vm.fixtures.value[0].groupId)
        vm.assignGroup(0, "grp-truss")
        assertEquals("grp-truss", vm.fixtures.value[0].groupId)
        // Other fixture unchanged
        assertNull(vm.fixtures.value[1].groupId)
    }

    @Test
    fun unassignGroup() = runTest {
        val fixtures = makeFixtures(1)
        val engine = EffectEngine(backgroundScope, fixtures)
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)

        vm.assignGroup(0, "grp-truss")
        assertEquals("grp-truss", vm.fixtures.value[0].groupId)
        vm.assignGroup(0, null)
        assertNull(vm.fixtures.value[0].groupId)
    }

    @Test
    fun updateZHeightOutOfBoundsNoOp() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)
        // Should not crash
        vm.updateZHeight(99, 3f)
        assertTrue(vm.fixtures.value.isEmpty())
    }

    @Test
    fun assignGroupOutOfBoundsNoOp() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, stubNodeDiscovery(), backgroundScope)
        // Should not crash
        vm.assignGroup(99, "grp-test")
        assertTrue(vm.fixtures.value.isEmpty())
    }
}

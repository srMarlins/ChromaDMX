package com.chromadmx.ui.viewmodel

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.tempo.clock.BeatClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun exposesFixturesList() = runTest {
        val fixtures = makeFixtures(4)
        val engine = EffectEngine(backgroundScope, fixtures)
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, backgroundScope)
        assertEquals(4, vm.fixtures.value.size)
    }

    @Test
    fun exposesMasterDimmer() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, backgroundScope)
        assertEquals(1.0f, vm.masterDimmer.value)
        vm.setMasterDimmer(0.5f)
        assertEquals(0.5f, vm.masterDimmer.value)
    }

    @Test
    fun exposesEffectLayers() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, backgroundScope)
        assertTrue(vm.layers.value.isEmpty())
        vm.setEffect(0, "solid-color", EffectParams.EMPTY.with("color", Color.RED))
        assertEquals(1, vm.layers.value.size)
    }

    @Test
    fun addAndRemoveFixture() = runTest {
        val engine = EffectEngine(backgroundScope, emptyList())
        val vm = StageViewModel(engine, registry, makePresetLibrary(engine), stubBeatClock, backgroundScope)
        val fixture = Fixture3D(
            fixture = Fixture("new", "New", 0, 3, 0),
            position = Vec3.ZERO
        )
        vm.addFixture(fixture)
        assertEquals(1, vm.fixtures.value.size)
        vm.removeFixture(0)
        assertEquals(0, vm.fixtures.value.size)
    }
}

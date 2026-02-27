package com.chromadmx.engine.pipeline

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effects.Chase3DEffect
import com.chromadmx.engine.effects.GradientSweep3DEffect
import com.chromadmx.engine.effects.ParticleBurst3DEffect
import com.chromadmx.engine.effects.PerlinNoise3DEffect
import com.chromadmx.engine.effects.RadialPulse3DEffect
import com.chromadmx.engine.effects.RainbowSweep3DEffect
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.effects.StrobeEffect
import com.chromadmx.engine.effects.WaveEffect3DEffect
import com.chromadmx.engine.preset.PresetLibrary
import kotlin.test.Test
import kotlin.test.assertEquals

import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope

/**
 * Regression test for the "black fixtures" bug.
 *
 * Reproduces the exact production lifecycle:
 * 1. Engine starts with empty fixtures (as in ChromaDiModule)
 * 2. PresetLibrary is created (serializes built-in presets to storage)
 * 3. Fixtures are added later via updateFixtures() (as in repoSyncJob)
 * 4. A preset is loaded via loadPreset() (as in handleApplyScene after user tap)
 * 5. Engine ticks
 * 6. Colors are read from the TripleBuffer
 * 7. At least one fixture should be non-black
 */
class ColorPipelineTest {

    private class FakeFileStorage : FileStorage {
        private val files = mutableMapOf<String, String>()
        override fun saveFile(path: String, content: String) { files[path] = content }
        override fun readFile(path: String): String? = files[path]
        override fun deleteFile(path: String): Boolean = files.remove(path) != null
        override fun listFiles(directory: String): List<String> =
            files.keys.filter { it.startsWith(directory) }.map { it.substringAfterLast("/") }
        override fun exists(path: String): Boolean = files.containsKey(path)
        override fun mkdirs(directory: String) {}
    }

    private fun fullRegistry(): EffectRegistry = EffectRegistry().apply {
        register(SolidColorEffect())
        register(StrobeEffect())
        register(Chase3DEffect())
        register(GradientSweep3DEffect())
        register(RainbowSweep3DEffect())
        register(RadialPulse3DEffect())
        register(WaveEffect3DEffect())
        register(ParticleBurst3DEffect())
        register(PerlinNoise3DEffect())
    }

    private fun testFixtures(): List<Fixture3D> = List(8) { i ->
        Fixture3D(
            fixture = Fixture(
                fixtureId = "fix-$i",
                name = "Fixture $i",
                channelStart = i * 3 + 1,
                channelCount = 3,
                universeId = 0
            ),
            position = Vec3(x = i / 8f, y = 0f, z = 0f)
        )
    }

    /**
     * Reproduces the production lifecycle end-to-end.
     * Engine starts empty, fixtures come later, then preset is loaded.
     * Verifies colors flow through the TripleBuffer correctly.
     */
    @Test
    fun productionLifecycleProducesNonBlackColors() {
        val scope = TestScope()
        val registry = fullRegistry()
        val storage = FakeFileStorage()

        // Step 1: Engine starts with EMPTY fixtures (matches ChromaDiModule)
        val engine = EffectEngine(scope)

        // Step 2: PresetLibrary is created (serializes built-ins to disk)
        val presetLibrary = PresetLibrary(storage, registry, engine.effectStack)

        // Step 3: Fixtures arrive later (as in repoSyncJob collecting from DB)
        val fixtures = testFixtures()
        engine.updateFixtures(fixtures)

        // Step 4: User taps a preset — loads via disk round-trip (serialized -> deserialized)
        val presets = presetLibrary.listPresets()
        val neonPulse = presets.first { it.name == "Neon Pulse" }
        val loaded = presetLibrary.loadPreset(neonPulse.id)
        assertTrue(loaded, "loadPreset should return true")

        // Step 5: Engine ticks (simulate a few frames)
        val beat = BeatState(bpm = 128f, beatPhase = 0.25f, barPhase = 0.06f, elapsed = 1.0f)
        engine.beatStateProvider = { beat }
        repeat(5) { engine.tick() }

        // Step 6: Read from TripleBuffer (as syncColorsFromEngine does)
        val buffer = engine.colorOutput
        buffer.swapRead()
        val colors = buffer.readSlot()

        // Step 7: At least some fixtures should be non-black
        val hasColor = colors.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
        assertTrue(hasColor, "After loading Neon Pulse preset, at least one fixture should have non-black color. " +
            "Colors: ${colors.map { "(${it.r},${it.g},${it.b})" }}")
    }

    /**
     * Verifies that updateFixtures() followed by tick() produces colors
     * when the TripleBuffer is replaced and then a new frame is written.
     */
    @Test
    fun updateFixturesFollowedByTickProducesColors() {
        val scope = TestScope()
        val registry = fullRegistry()
        val storage = FakeFileStorage()
        val engine = EffectEngine(scope)
        val presetLibrary = PresetLibrary(storage, registry, engine.effectStack)

        // Load preset BEFORE fixtures arrive
        val presets = presetLibrary.listPresets()
        val sunset = presets.first { it.name == "Sunset Sweep" }
        presetLibrary.loadPreset(sunset.id)

        // Now fixtures arrive — this replaces the TripleBuffer
        engine.updateFixtures(testFixtures())

        // Engine ticks with new fixtures and loaded preset
        val beat = BeatState(bpm = 128f, beatPhase = 0.25f, barPhase = 0.06f, elapsed = 1.0f)
        engine.beatStateProvider = { beat }
        engine.tick()

        // Read from the NEW TripleBuffer
        val buffer = engine.colorOutput
        buffer.swapRead()
        val colors = buffer.readSlot()

        val hasColor = colors.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
        assertTrue(hasColor, "After updateFixtures + tick, preset colors should be visible. " +
            "Colors: ${colors.map { "(${it.r},${it.g},${it.b})" }}")
    }

    /**
     * Verifies that syncColorsFromEngine pattern works:
     * reads engine.colorOutput, calls swapRead(), reads readSlot().
     * Tests that the reader sees colors even when the buffer was recently replaced.
     */
    @Test
    fun syncColorsPatternReadsCorrectDataAfterBufferReplacement() {
        val scope = TestScope()
        val registry = fullRegistry()
        val storage = FakeFileStorage()
        val engine = EffectEngine(scope)
        val presetLibrary = PresetLibrary(storage, registry, engine.effectStack)

        // Setup: fixtures + preset
        engine.updateFixtures(testFixtures())
        val presets = presetLibrary.listPresets()
        presetLibrary.loadPreset(presets.first { it.name == "Fire & Ice" }.id)

        val beat = BeatState(bpm = 128f, beatPhase = 0.25f, barPhase = 0.06f, elapsed = 1.0f)
        engine.beatStateProvider = { beat }

        // Tick once to write colors
        engine.tick()

        // Simulate syncColorsFromEngine() pattern
        val buffer = engine.colorOutput
        val swapped = buffer.swapRead()
        assertTrue(swapped, "swapRead should return true after engine tick")
        val colors = buffer.readSlot().toList()

        val hasColor = colors.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
        assertTrue(hasColor, "syncColors pattern should read non-black colors")
    }

    /**
     * Edge case: updateFixtures is called WHILE the engine has layers loaded.
     * The new TripleBuffer should receive colors on the next tick.
     */
    @Test
    fun updateFixturesWhilePresetLoadedDoesNotLoseColors() {
        val scope = TestScope()
        val registry = fullRegistry()
        val storage = FakeFileStorage()
        val fixtures = testFixtures()
        val engine = EffectEngine(scope, fixtures) // Start with fixtures this time
        val presetLibrary = PresetLibrary(storage, registry, engine.effectStack)

        // Load preset
        val presets = presetLibrary.listPresets()
        presetLibrary.loadPreset(presets.first { it.name == "Ocean Waves" }.id)

        val beat = BeatState(bpm = 128f, beatPhase = 0.25f, barPhase = 0.06f, elapsed = 1.0f)
        engine.beatStateProvider = { beat }

        // Engine produces colors
        engine.tick()
        val buf1 = engine.colorOutput
        buf1.swapRead()
        val colors1 = buf1.readSlot()
        val hasColor1 = colors1.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
        assertTrue(hasColor1, "Should have colors before updateFixtures")

        // Now updateFixtures replaces the TripleBuffer
        engine.updateFixtures(fixtures)

        // Tick again — should write to the NEW buffer
        engine.tick()

        // Read from the NEW buffer
        val buf2 = engine.colorOutput
        buf2.swapRead()
        val colors2 = buf2.readSlot()
        val hasColor2 = colors2.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
        assertTrue(hasColor2, "After updateFixtures + tick, colors should still be visible. " +
            "Colors: ${colors2.map { "(${it.r},${it.g},${it.b})" }}")
    }

    /**
     * Verifies that the atomic snapshot design ensures fixtures and buffers
     * are always consistent — tick() never sees mismatched sizes.
     */
    @Test
    fun snapshotGuaranteesConsistentFixturesAndBuffers() {
        val scope = TestScope()
        val engine = EffectEngine(scope)

        // Initial state: empty
        assertEquals(0, engine.fixtures.size)
        assertEquals(0, engine.colorOutput.readSlot().size)

        // Add 8 fixtures
        val fixtures8 = testFixtures()
        engine.updateFixtures(fixtures8)

        // Both should be size 8
        assertEquals(8, engine.fixtures.size)
        assertEquals(8, engine.colorOutput.writeSlot().size)

        // Shrink to 4 fixtures — buffer should also shrink
        val fixtures4 = testFixtures().take(4)
        engine.updateFixtures(fixtures4)

        assertEquals(4, engine.fixtures.size)
        assertEquals(4, engine.colorOutput.writeSlot().size)
    }

    /**
     * Verifies that loading all 6 built-in presets through the disk round-trip
     * (serialize -> deserialize) produces non-black output with the engine.
     *
     * This catches serialization bugs where EffectParams values lose their types
     * (e.g., Color -> JsonObject -> fails to deserialize back to Color).
     */
    @Test
    fun allBuiltInPresetsProduceColorsThroughDiskRoundTrip() {
        val scope = TestScope()
        val registry = fullRegistry()
        val storage = FakeFileStorage()
        val engine = EffectEngine(scope, testFixtures())
        val presetLibrary = PresetLibrary(storage, registry, engine.effectStack)
        val beat = BeatState(bpm = 128f, beatPhase = 0.25f, barPhase = 0.06f, elapsed = 1.0f)
        engine.beatStateProvider = { beat }

        val allPresets = presetLibrary.listPresets()
        assertEquals(6, allPresets.size, "Should have 6 built-in presets")

        for (preset in allPresets) {
            // Load preset through the full disk round-trip
            val loaded = presetLibrary.loadPreset(preset.id)
            assertTrue(loaded, "loadPreset should succeed for ${preset.name}")

            // Try several time values (some effects only produce visible output at certain times)
            val timeValues = listOf(0.1f, 0.2f, 0.5f, 1.0f, 2.0f)
            val hasColor = timeValues.any { time ->
                val colors = engine.evaluateFrame(time, beat)
                colors.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
            }
            assertTrue(hasColor, "Preset '${preset.name}' (${preset.id}) should produce non-black output " +
                "when loaded through disk round-trip")
        }
    }

    /**
     * Verifies the TripleBuffer correctly publishes data after updateFixtures().
     * The bug scenario: engine writes to a buffer, then updateFixtures() replaces it,
     * reader reads the new (empty) buffer before engine ticks with it.
     */
    @Test
    fun readerSeesColorsAfterOneTickOnNewBuffer() {
        val scope = TestScope()
        val registry = fullRegistry()
        val storage = FakeFileStorage()
        val fixtures = testFixtures()
        val engine = EffectEngine(scope, fixtures)
        val presetLibrary = PresetLibrary(storage, registry, engine.effectStack)

        val presets = presetLibrary.listPresets()
        presetLibrary.loadPreset(presets.first { it.name == "Sunset Sweep" }.id)
        engine.beatStateProvider = { BeatState(bpm = 128f, beatPhase = 0.25f, barPhase = 0.06f, elapsed = 1.0f) }

        // Engine produces colors on original buffer
        engine.tick()

        // Simulate: updateFixtures replaces the buffer (like repoSyncJob re-emitting)
        engine.updateFixtures(fixtures)

        // Before engine ticks, reader sees the new buffer (empty/black)
        val bufBefore = engine.colorOutput
        val hasDirtyBefore = bufBefore.swapRead()
        // New buffer should NOT be dirty yet (engine hasn't ticked)
        assertTrue(!hasDirtyBefore, "New buffer should not be dirty before engine tick")

        // Engine ticks once on the new buffer
        engine.tick()

        // Now reader should see colors
        val bufAfter = engine.colorOutput
        val hasDirtyAfter = bufAfter.swapRead()
        assertTrue(hasDirtyAfter, "Buffer should be dirty after engine tick")

        val colors = bufAfter.readSlot()
        val hasColor = colors.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
        assertTrue(hasColor, "Reader should see non-black colors after one tick on new buffer")
    }
}

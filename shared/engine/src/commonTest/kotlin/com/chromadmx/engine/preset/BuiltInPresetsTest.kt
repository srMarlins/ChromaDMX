package com.chromadmx.engine.preset

import com.chromadmx.core.model.*
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.effects.*
import com.chromadmx.engine.pipeline.EffectEngine
import kotlinx.coroutines.test.TestScope
import kotlin.test.*

/**
 * Tests for the 6 built-in universal presets.
 */
class BuiltInPresetsTest {

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

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

    /** Build an [EffectRegistry] with all V1 effects registered. */
    private fun fullRegistry(): EffectRegistry = EffectRegistry().apply {
        register(Chase3DEffect())
        register(GradientSweep3DEffect())
        register(ParticleBurst3DEffect())
        register(PerlinNoise3DEffect())
        register(RadialPulse3DEffect())
        register(RainbowSweep3DEffect())
        register(SolidColorEffect())
        register(StrobeEffect())
        register(WaveEffect3DEffect())
    }

    /** 8 fixtures spread along x = 0..0.875, y/z = 0. */
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

    private val presets = builtInPresets()

    /* ------------------------------------------------------------------ */
    /*  Basic structural tests                                             */
    /* ------------------------------------------------------------------ */

    @Test
    fun sixPresetsAreDefined() {
        assertEquals(6, presets.size)
    }

    @Test
    fun allPresetsAreMarkedBuiltIn() {
        presets.forEach { preset ->
            assertTrue(preset.isBuiltIn, "${preset.name} should be built-in")
        }
    }

    @Test
    fun eachPresetHasAtLeastOneLayer() {
        presets.forEach { preset ->
            assertTrue(preset.layers.isNotEmpty(), "${preset.name} should have layers")
        }
    }

    @Test
    fun thumbnailColorsAreNonEmpty() {
        presets.forEach { preset ->
            assertTrue(
                preset.thumbnailColors.isNotEmpty(),
                "${preset.name} should have thumbnailColors"
            )
        }
    }

    @Test
    fun allEffectIdsAreValid() {
        val registry = fullRegistry()
        val registeredIds = registry.ids()

        presets.forEach { preset ->
            preset.layers.forEach { layer ->
                assertTrue(
                    layer.effectId in registeredIds,
                    "Preset '${preset.name}' references unknown effect '${layer.effectId}'"
                )
            }
        }
    }

    @Test
    fun presetIdsAreUnique() {
        val ids = presets.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Preset IDs should be unique")
    }

    @Test
    fun presetNamesAreUnique() {
        val names = presets.map { it.name }
        assertEquals(names.size, names.toSet().size, "Preset names should be unique")
    }

    /* ------------------------------------------------------------------ */
    /*  Evaluation: each preset produces distinct non-black output         */
    /* ------------------------------------------------------------------ */

    @Test
    fun neonPulseProducesNonBlackOutput() {
        assertPresetProducesColor("builtin_neon_pulse")
    }

    @Test
    fun sunsetSweepProducesNonBlackOutput() {
        assertPresetProducesColor("builtin_sunset_sweep")
    }

    @Test
    fun strobeStormProducesNonBlackOutput() {
        // Strobe depends on beat phase — use a phase < dutyCycle so it fires.
        assertPresetProducesColor(
            "builtin_strobe_storm",
            beat = BeatState(bpm = 174f, beatPhase = 0.05f, barPhase = 0.01f, elapsed = 1.0f)
        )
    }

    @Test
    fun oceanWavesProducesNonBlackOutput() {
        assertPresetProducesColor("builtin_ocean_waves")
    }

    @Test
    fun fireAndIceProducesNonBlackOutput() {
        assertPresetProducesColor("builtin_fire_and_ice")
    }

    @Test
    fun midnightRainbowProducesNonBlackOutput() {
        assertPresetProducesColor("builtin_midnight_rainbow")
    }

    /* ------------------------------------------------------------------ */
    /*  Integration: PresetLibrary auto-installs built-ins                 */
    /* ------------------------------------------------------------------ */

    @Test
    fun presetLibraryAutoInstallsBuiltIns() {
        val registry = fullRegistry()
        val storage = FakeFileStorage()
        val stack = EffectStack()

        // Creating the library should auto-install the 6 built-ins.
        val library = PresetLibrary(storage, registry, stack)
        val all = library.listPresets()

        assertEquals(6, all.size, "All 6 built-in presets should be auto-installed")
        assertTrue(all.all { it.isBuiltIn }, "All auto-installed presets should be built-in")
    }

    @Test
    fun ensureBuiltInsDoesNotOverwriteExisting() {
        val registry = fullRegistry()
        val storage = FakeFileStorage()
        val stack = EffectStack()

        // First creation — installs built-ins.
        val library1 = PresetLibrary(storage, registry, stack)
        val original = library1.getPreset("builtin_neon_pulse")
        assertNotNull(original)

        // Second creation with same storage — should NOT overwrite.
        val library2 = PresetLibrary(storage, registry, stack)
        val reloaded = library2.getPreset("builtin_neon_pulse")
        assertNotNull(reloaded)
        assertEquals(original, reloaded, "Built-in preset should not be overwritten on re-init")
    }

    /* ------------------------------------------------------------------ */
    /*  Evaluation helper                                                  */
    /* ------------------------------------------------------------------ */

    private fun assertPresetProducesColor(
        presetId: String,
        beat: BeatState = BeatState(bpm = 128f, beatPhase = 0.25f, barPhase = 0.06f, elapsed = 1.0f)
    ) {
        val preset = presets.first { it.id == presetId }
        val registry = fullRegistry()
        val fixtures = testFixtures()
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        // Build the effect layers from the preset configuration.
        val layers = preset.layers.mapNotNull { config ->
            val effect = registry.get(config.effectId) ?: return@mapNotNull null
            EffectLayer(
                effect = effect,
                params = config.params,
                blendMode = config.blendMode,
                opacity = config.opacity,
                enabled = config.enabled
            )
        }
        engine.effectStack.replaceLayers(layers)
        engine.effectStack.masterDimmer = preset.masterDimmer

        // Try several time values to handle effects whose active region
        // moves through space (e.g. RadialPulse expanding shell).
        val timeValues = listOf(0.1f, 0.2f, 0.5f, 1.0f, 2.0f)
        val hasColor = timeValues.any { time ->
            val colors = engine.evaluateFrame(time, beat)
            colors.any { c -> c.r > 0.001f || c.g > 0.001f || c.b > 0.001f }
        }
        assertTrue(hasColor, "Preset '${preset.name}' should produce non-black output at some time")
    }
}

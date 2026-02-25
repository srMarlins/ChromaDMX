package com.chromadmx.engine.pipeline

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.SpatialEffect
import com.chromadmx.engine.effects.GradientSweep3DEffect
import com.chromadmx.engine.effects.PerlinNoise3DEffect
import com.chromadmx.engine.effects.RainbowSweep3DEffect
import com.chromadmx.engine.effects.SolidColorEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime
import kotlinx.coroutines.test.TestScope

class EffectEngineTest {

    private fun makeFixtures(count: Int): List<Fixture3D> {
        return (0 until count).map { i ->
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "fix-$i",
                    name = "Fixture $i",
                    channelStart = i * 3 + 1,
                    channelCount = 3,
                    universeId = 1
                ),
                position = Vec3(
                    x = (i % 16) / 16f,
                    y = ((i / 16) % 16) / 16f,
                    z = (i / 256) / 4f
                )
            )
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Basic engine tests                                                 */
    /* ------------------------------------------------------------------ */

    @Test
    fun engineEvaluatesSolidColorForAllFixtures() {
        val fixtures = makeFixtures(10)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.RED),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        val frame = engine.evaluateFrame(0f, BeatState.IDLE)
        assertEquals(10, frame.size)
        for (color in frame) {
            assertEquals(Color.RED, color)
        }
    }

    @Test
    fun engineTickWritesToTripleBuffer() {
        val fixtures = makeFixtures(5)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.GREEN),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Tick writes to triple buffer
        engine.tick()

        // Read from triple buffer
        val output = engine.colorOutput.read()
        assertEquals(5, output.size)
        for (color in output) {
            assertEquals(Color.GREEN, color)
        }
    }

    @Test
    fun engineMasterDimmerAffectsOutput() {
        val fixtures = makeFixtures(3)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.WHITE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.masterDimmer = 0.5f

        val frame = engine.evaluateFrame(0f, BeatState.IDLE)
        for (color in frame) {
            assertEquals(0.5f, color.r, 0.001f)
            assertEquals(0.5f, color.g, 0.001f)
            assertEquals(0.5f, color.b, 0.001f)
        }
    }

    @Test
    fun engineWithMultipleLayers() {
        val fixtures = makeFixtures(4)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.RED),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.GREEN),
                blendMode = BlendMode.ADDITIVE,
                opacity = 1.0f
            )
        )

        val frame = engine.evaluateFrame(0f, BeatState.IDLE)
        for (color in frame) {
            // RED + GREEN = YELLOW (1,1,0)
            assertEquals(1.0f, color.r, 0.001f)
            assertEquals(1.0f, color.g, 0.001f)
            assertEquals(0.0f, color.b, 0.001f)
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Performance test: 240 fixtures under 1ms per frame                */
    /* ------------------------------------------------------------------ */

    @Test
    fun performanceTest240FixturesSolidColorUnder1ms() {
        val fixtures = makeFixtures(240)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.RED),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Warm up
        repeat(10) { engine.evaluateFrame(0f, BeatState.IDLE) }

        // Measure
        val duration = measureTime {
            repeat(100) { i ->
                engine.evaluateFrame(i * 0.016f, BeatState.IDLE)
            }
        }

        val avgMs = duration.inWholeMilliseconds / 100.0
        println("SolidColor 240 fixtures: avg ${avgMs}ms per frame")
        assertTrue(avgMs < 1.0, "SolidColor 240 fixtures should be under 1ms, was ${avgMs}ms")
    }

    @Test
    fun performanceTest240FixturesGradientSweepUnder1ms() {
        val fixtures = makeFixtures(240)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = GradientSweep3DEffect(),
                params = EffectParams()
                    .with("axis", "x")
                    .with("speed", 1.0f)
                    .with("palette", listOf(Color.RED, Color.GREEN, Color.BLUE)),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Warm up
        repeat(10) { engine.evaluateFrame(0f, BeatState.IDLE) }

        val duration = measureTime {
            repeat(100) { i ->
                engine.evaluateFrame(i * 0.016f, BeatState.IDLE)
            }
        }

        val avgMs = duration.inWholeMilliseconds / 100.0
        println("GradientSweep 240 fixtures: avg ${avgMs}ms per frame")
        assertTrue(avgMs < 1.0, "GradientSweep 240 fixtures should be under 1ms, was ${avgMs}ms")
    }

    @Test
    fun performanceTest240FixturesPerlinNoiseUnder1ms() {
        val fixtures = makeFixtures(240)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = PerlinNoise3DEffect(),
                params = EffectParams()
                    .with("scale", 2.0f)
                    .with("speed", 0.5f)
                    .with("palette", listOf(Color.BLACK, Color.WHITE)),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Warm up
        repeat(10) { engine.evaluateFrame(0f, BeatState.IDLE) }

        val duration = measureTime {
            repeat(100) { i ->
                engine.evaluateFrame(i * 0.016f, BeatState.IDLE)
            }
        }

        val avgMs = duration.inWholeMilliseconds / 100.0
        println("PerlinNoise 240 fixtures: avg ${avgMs}ms per frame")
        assertTrue(avgMs < 1.0, "PerlinNoise 240 fixtures should be under 1ms, was ${avgMs}ms")
    }

    @Test
    fun performanceTest240FixturesMultiLayerUnder1ms() {
        val fixtures = makeFixtures(240)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        // Two-layer stack: gradient + rainbow additive
        engine.effectStack.addLayer(
            EffectLayer(
                effect = GradientSweep3DEffect(),
                params = EffectParams()
                    .with("axis", "x")
                    .with("speed", 1.0f)
                    .with("palette", listOf(Color.RED, Color.BLUE)),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.addLayer(
            EffectLayer(
                effect = RainbowSweep3DEffect(),
                params = EffectParams()
                    .with("axis", "y")
                    .with("speed", 0.5f)
                    .with("spread", 1.0f),
                blendMode = BlendMode.ADDITIVE,
                opacity = 0.5f
            )
        )

        // Warm up
        repeat(10) { engine.evaluateFrame(0f, BeatState.IDLE) }

        val duration = measureTime {
            repeat(100) { i ->
                engine.evaluateFrame(i * 0.016f, BeatState.IDLE)
            }
        }

        val avgMs = duration.inWholeMilliseconds / 100.0
        println("MultiLayer 240 fixtures: avg ${avgMs}ms per frame")
        assertTrue(avgMs < 1.0, "MultiLayer 240 fixtures should be under 1ms, was ${avgMs}ms")
    }
}

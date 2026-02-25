package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.MovementLayer
import com.chromadmx.engine.pipeline.EffectEngine
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime
import kotlinx.coroutines.test.TestScope

/**
 * Performance tests ensuring 240 fixtures with movement effects
 * compute in under 1ms per frame.
 */
class MovementPerformanceTest {

    private fun makeFixtures(count: Int): List<Fixture3D> {
        return (0 until count).map { i ->
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "fix-$i",
                    name = "Fixture $i",
                    channelStart = i * 10 + 1,
                    channelCount = 10,
                    universeId = 1,
                    profileId = "generic-moving-head"
                ),
                position = Vec3(
                    x = (i % 16) / 16f,
                    y = ((i / 16) % 16) / 16f,
                    z = (i / 256) / 4f
                )
            )
        }
    }

    @Test
    fun performanceTest240FixturesColorPlusMovementUnder1ms() {
        val fixtures = makeFixtures(240)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        // Color layer + movement layer
        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.BLUE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.addMovementLayer(
            MovementLayer(
                effect = CircleMovementEffect(),
                params = EffectParams()
                    .with("radius", 0.25f)
                    .with("speed", 1.0f)
                    .with("centerPan", 0.5f)
                    .with("centerTilt", 0.5f)
            )
        )

        // Warm up
        repeat(10) { engine.evaluateFrameOutput(0f, BeatState.IDLE) }

        // Measure
        val duration = measureTime {
            repeat(100) { i ->
                engine.evaluateFrameOutput(i * 0.016f, BeatState.IDLE)
            }
        }

        val avgMs = duration.inWholeMilliseconds / 100.0
        println("Color+Circle 240 fixtures: avg ${avgMs}ms per frame")
        assertTrue(avgMs < 1.0, "Color+Circle 240 fixtures should be under 1ms, was ${avgMs}ms")
    }

    @Test
    fun performanceTest240FixturesMultipleMovementLayersUnder1ms() {
        val fixtures = makeFixtures(240)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        // Color layer + two movement layers + gobo layer
        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.RED),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.addMovementLayer(
            MovementLayer(
                effect = SweepMovementEffect(),
                params = EffectParams()
                    .with("axis", "both")
                    .with("range", 0.5f)
                    .with("speed", 1.0f)
            )
        )
        engine.effectStack.addMovementLayer(
            MovementLayer(
                effect = GoboBeatSyncEffect(),
                params = EffectParams()
                    .with("slotCount", 8)
                    .with("changeOnBeat", true)
            )
        )

        // Warm up
        repeat(10) { engine.evaluateFrameOutput(0f, BeatState.IDLE) }

        // Measure
        val duration = measureTime {
            repeat(100) { i ->
                val beat = BeatState(bpm = 120f, beatPhase = (i % 10) / 10f,
                    barPhase = i / 40f, elapsed = i * 0.016f)
                engine.evaluateFrameOutput(i * 0.016f, beat)
            }
        }

        val avgMs = duration.inWholeMilliseconds / 100.0
        println("Color+Sweep+Gobo 240 fixtures: avg ${avgMs}ms per frame")
        assertTrue(avgMs < 1.0, "Color+Sweep+Gobo 240 fixtures should be under 1ms, was ${avgMs}ms")
    }

    @Test
    fun performanceTest240FixturesFollowEffectUnder1ms() {
        val fixtures = makeFixtures(240)
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
        engine.effectStack.addMovementLayer(
            MovementLayer(
                effect = FollowEffect(),
                params = EffectParams()
                    .with("targetX", 0.5f)
                    .with("targetY", 0.0f)
                    .with("targetZ", -2.0f)
            )
        )

        // Warm up
        repeat(10) { engine.evaluateFrameOutput(0f, BeatState.IDLE) }

        // Measure
        val duration = measureTime {
            repeat(100) { i ->
                engine.evaluateFrameOutput(i * 0.016f, BeatState.IDLE)
            }
        }

        val avgMs = duration.inWholeMilliseconds / 100.0
        println("Color+Follow 240 fixtures: avg ${avgMs}ms per frame")
        assertTrue(avgMs < 1.0, "Color+Follow 240 fixtures should be under 1ms, was ${avgMs}ms")
    }
}

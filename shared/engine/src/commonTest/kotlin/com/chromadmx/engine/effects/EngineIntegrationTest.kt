package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.effect.MovementLayer
import com.chromadmx.engine.pipeline.EffectEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope

class EngineIntegrationTest {

    private val beat = BeatState.IDLE

    private fun makeFixtures(count: Int): List<Fixture3D> {
        return (0 until count).map { i ->
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "fix-$i",
                    name = "Fixture $i",
                    channelStart = i * 10 + 1,
                    channelCount = 10,
                    universeId = 1
                ),
                position = Vec3(
                    x = i.toFloat() / count,
                    y = 0f,
                    z = 0f
                )
            )
        }
    }

    /* ------------------------------------------------------------------ */
    /*  EffectStack: color + movement compose correctly                    */
    /* ------------------------------------------------------------------ */

    @Test
    fun colorOnlyStackReturnsDefaultMovement() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = SolidColorEffect(),
                    params = EffectParams().with("color", Color.RED)
                )
            )
        )

        val evaluator = stack.buildFrame(0f, beat)
        val output = evaluator.evaluateFixtureOutput(Vec3.ZERO)

        assertEquals(Color.RED, output.color)
        assertNull(output.pan)
        assertNull(output.tilt)
        assertNull(output.gobo)
    }

    @Test
    fun movementOnlyStackReturnsBlackColor() {
        val stack = EffectStack(
            movementLayers = listOf(
                MovementLayer(
                    effect = SweepMovementEffect(),
                    params = EffectParams()
                        .with("axis", "pan")
                        .with("range", 0.5f)
                        .with("speed", 1.0f)
                        .with("centerPan", 0.5f)
                )
            )
        )

        val evaluator = stack.buildFrame(0f, beat)
        val output = evaluator.evaluateFixtureOutput(Vec3.ZERO)

        // No color layers => black after master dimmer
        assertEquals(Color.BLACK, output.color)
        // But movement should be present
        assertNotNull(output.pan)
    }

    @Test
    fun colorAndMovementLayersComposeTogether() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = SolidColorEffect(),
                    params = EffectParams().with("color", Color.BLUE)
                )
            ),
            movementLayers = listOf(
                MovementLayer(
                    effect = CircleMovementEffect(),
                    params = EffectParams()
                        .with("radius", 0.2f)
                        .with("speed", 1.0f)
                        .with("centerPan", 0.5f)
                        .with("centerTilt", 0.5f)
                )
            )
        )

        val evaluator = stack.buildFrame(0f, beat)
        val output = evaluator.evaluateFixtureOutput(Vec3.ZERO)

        // Color from the color layer
        assertEquals(Color.BLUE, output.color)
        // Movement from the movement layer
        assertNotNull(output.pan)
        assertNotNull(output.tilt)
    }

    @Test
    fun multipleMovementLayersCompose() {
        val stack = EffectStack(
            movementLayers = listOf(
                MovementLayer(
                    effect = SweepMovementEffect(),
                    params = EffectParams()
                        .with("axis", "pan")
                        .with("range", 0.5f)
                        .with("speed", 1.0f)
                        .with("centerPan", 0.5f),
                    blendMode = BlendMode.NORMAL
                ),
                MovementLayer(
                    effect = GoboBeatSyncEffect(),
                    params = EffectParams()
                        .with("slotCount", 4)
                        .with("changeOnBeat", true),
                    blendMode = BlendMode.NORMAL
                )
            )
        )

        val evaluator = stack.buildFrame(0f, beat)
        val output = evaluator.evaluateFixtureOutput(Vec3.ZERO)

        // Pan from sweep layer
        assertNotNull(output.pan)
        // Gobo from gobo layer
        assertNotNull(output.gobo)
    }

    @Test
    fun disabledMovementLayerIsSkipped() {
        val stack = EffectStack(
            movementLayers = listOf(
                MovementLayer(
                    effect = SweepMovementEffect(),
                    params = EffectParams()
                        .with("axis", "pan")
                        .with("range", 0.5f),
                    enabled = false
                )
            )
        )

        val evaluator = stack.buildFrame(0f, beat)
        val output = evaluator.evaluateFixtureOutput(Vec3.ZERO)

        // Disabled movement layer should not contribute
        assertNull(output.pan)
    }

    @Test
    fun masterDimmerAffectsColorButNotMovement() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = SolidColorEffect(),
                    params = EffectParams().with("color", Color.WHITE)
                )
            ),
            movementLayers = listOf(
                MovementLayer(
                    effect = SweepMovementEffect(),
                    params = EffectParams()
                        .with("axis", "pan")
                        .with("range", 0.5f)
                        .with("speed", 1.0f)
                        .with("centerPan", 0.5f)
                )
            ),
            masterDimmer = 0.5f
        )

        val evaluator = stack.buildFrame(0f, beat)
        val output = evaluator.evaluateFixtureOutput(Vec3.ZERO)

        // Color dimmed by master dimmer
        assertEquals(0.5f, output.color.r, 0.01f)
        assertEquals(0.5f, output.color.g, 0.01f)
        assertEquals(0.5f, output.color.b, 0.01f)

        // Movement NOT affected by master dimmer (pan should still be at center)
        assertNotNull(output.pan)
        assertEquals(0.5f, output.pan!!, 0.01f)
    }

    @Test
    fun additiveMovementLayersAddOffsets() {
        val stack = EffectStack(
            movementLayers = listOf(
                MovementLayer(
                    effect = CircleMovementEffect(),
                    params = EffectParams()
                        .with("radius", 0.1f)
                        .with("speed", 0.0f) // frozen at time 0
                        .with("centerPan", 0.3f)
                        .with("centerTilt", 0.3f),
                    blendMode = BlendMode.NORMAL
                ),
                MovementLayer(
                    effect = CircleMovementEffect(),
                    params = EffectParams()
                        .with("radius", 0.1f)
                        .with("speed", 0.0f) // frozen at time 0
                        .with("centerPan", 0.2f)
                        .with("centerTilt", 0.2f),
                    blendMode = BlendMode.ADDITIVE
                )
            )
        )

        val evaluator = stack.buildFrame(0f, beat)
        val output = evaluator.evaluateFixtureOutput(Vec3.ZERO)

        // First layer at time=0: pan = 0.3 + 0.1 = 0.4, tilt = 0.3
        // Second layer at time=0: pan = 0.2 + 0.1 = 0.3, tilt = 0.2
        // Additive: pan = 0.4 + 0.3 = 0.7, tilt = 0.3 + 0.2 = 0.5
        assertNotNull(output.pan)
        assertNotNull(output.tilt)
        assertEquals(0.7f, output.pan!!, 0.05f)
        assertEquals(0.5f, output.tilt!!, 0.05f)
    }

    /* ------------------------------------------------------------------ */
    /*  EffectEngine integration                                           */
    /* ------------------------------------------------------------------ */

    @Test
    fun engineEvaluateFrameOutputReturnsFullOutput() {
        val fixtures = makeFixtures(5)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.GREEN)
            )
        )
        engine.effectStack.addMovementLayer(
            MovementLayer(
                effect = CircleMovementEffect(),
                params = EffectParams()
                    .with("radius", 0.2f)
                    .with("speed", 1.0f)
                    .with("centerPan", 0.5f)
                    .with("centerTilt", 0.5f)
            )
        )

        val outputs = engine.evaluateFrameOutput(0f, BeatState.IDLE)
        assertEquals(5, outputs.size)

        for (output in outputs) {
            assertEquals(Color.GREEN, output.color)
            assertNotNull(output.pan)
            assertNotNull(output.tilt)
        }
    }

    @Test
    fun engineEvaluateFrameStillWorksForColorOnly() {
        val fixtures = makeFixtures(3)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.RED)
            )
        )

        // Color-only evaluation still works
        val colors = engine.evaluateFrame(0f, BeatState.IDLE)
        assertEquals(3, colors.size)
        for (color in colors) {
            assertEquals(Color.RED, color)
        }
    }

    @Test
    fun engineTickWritesBothBuffers() {
        val fixtures = makeFixtures(4)
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.BLUE)
            )
        )
        engine.effectStack.addMovementLayer(
            MovementLayer(
                effect = SweepMovementEffect(),
                params = EffectParams()
                    .with("axis", "pan")
                    .with("range", 0.5f)
                    .with("speed", 1.0f)
                    .with("centerPan", 0.5f)
            )
        )

        engine.tick()

        val colors = engine.colorOutput.read()
        assertEquals(4, colors.size)
        for (color in colors) {
            assertEquals(Color.BLUE, color)
        }

        val outputs = engine.fixtureOutputBuffer.read()
        assertEquals(4, outputs.size)
        for (output in outputs) {
            assertEquals(Color.BLUE, output.color)
            assertNotNull(output.pan)
        }
    }

    @Test
    fun backwardCompatEvaluateColorStillWorks() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = SolidColorEffect(),
                    params = EffectParams().with("color", Color.RED)
                )
            ),
            movementLayers = listOf(
                MovementLayer(
                    effect = CircleMovementEffect(),
                    params = EffectParams()
                        .with("radius", 0.2f)
                        .with("speed", 1.0f)
                )
            )
        )

        // The old evaluate() method should still return just the color
        val color = stack.evaluate(Vec3.ZERO, 0f, beat)
        assertEquals(Color.RED, color)
    }

    @Test
    fun stackMovementLayerManagement() {
        val stack = EffectStack()
        assertEquals(0, stack.movementLayerCount)

        stack.addMovementLayer(
            MovementLayer(
                effect = SweepMovementEffect(),
                params = EffectParams()
            )
        )
        assertEquals(1, stack.movementLayerCount)

        stack.addMovementLayer(
            MovementLayer(
                effect = CircleMovementEffect(),
                params = EffectParams()
            )
        )
        assertEquals(2, stack.movementLayerCount)

        stack.removeMovementLayerAt(0)
        assertEquals(1, stack.movementLayerCount)
        assertEquals(CircleMovementEffect.ID, stack.movementLayers[0].effect.id)

        stack.clearMovementLayers()
        assertEquals(0, stack.movementLayerCount)
    }

    @Test
    fun followEffectAcrossMultipleFixtures() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("left", "Left", 1, 10, 1),
                position = Vec3(-2f, 2f, 0f)
            ),
            Fixture3D(
                fixture = Fixture("center", "Center", 11, 10, 1),
                position = Vec3(0f, 2f, 0f)
            ),
            Fixture3D(
                fixture = Fixture("right", "Right", 21, 10, 1),
                position = Vec3(2f, 2f, 0f)
            )
        )

        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.WHITE)
            )
        )
        engine.effectStack.addMovementLayer(
            MovementLayer(
                effect = FollowEffect(),
                params = EffectParams()
                    .with("targetX", 0.0f)
                    .with("targetY", 0.0f)
                    .with("targetZ", -2.0f)
            )
        )

        val outputs = engine.evaluateFrameOutput(0f, BeatState.IDLE)
        assertEquals(3, outputs.size)

        // All should have pan and tilt
        outputs.forEach { output ->
            assertNotNull(output.pan)
            assertNotNull(output.tilt)
            assertEquals(Color.WHITE, output.color)
        }

        // Left fixture should pan right, right fixture should pan left
        // Center fixture should be roughly center pan
        val leftPan = outputs[0].pan!!
        val centerPan = outputs[1].pan!!
        val rightPan = outputs[2].pan!!

        assertTrue(leftPan > centerPan, "Left fixture should pan right more than center")
        assertTrue(centerPan > rightPan, "Center fixture should pan right more than right")
    }

    @Test
    fun hasMovementLayersReflectsState() {
        val stack = EffectStack()
        val evaluator1 = stack.buildFrame(0f, beat)
        assertTrue(!evaluator1.hasMovementLayers)

        stack.addMovementLayer(
            MovementLayer(
                effect = SweepMovementEffect(),
                params = EffectParams()
            )
        )
        val evaluator2 = stack.buildFrame(0f, beat)
        assertTrue(evaluator2.hasMovementLayers)
    }
}

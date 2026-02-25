package com.chromadmx.simulation.camera

import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.simulation.camera.SimulatedCamera.FixtureState
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulatedCameraTest {

    private fun makeFixture3D(id: String, x: Float, y: Float, z: Float): Fixture3D {
        return Fixture3D(
            fixture = Fixture(
                fixtureId = id,
                name = id,
                channelStart = 0,
                channelCount = 3,
                universeId = 0
            ),
            position = Vec3(x, y, z)
        )
    }

    // ------------------------------------------------------------------ //
    //  Frame generation basics                                            //
    // ------------------------------------------------------------------ //

    @Test
    fun generateFrame_correctSize() {
        val camera = SimulatedCamera(width = 320, height = 240, noiseLevel = 0f)
        val frame = camera.generateFrame(emptyList())
        assertEquals(320 * 240, frame.size)
    }

    @Test
    fun generateFrame_emptyFixtures_ambientOnly() {
        val camera = SimulatedCamera(
            width = 100,
            height = 100,
            noiseLevel = 0f,
            ambientLevel = 20
        )
        val frame = camera.generateFrame(emptyList())

        // All pixels should be ambient level
        for (i in frame.indices) {
            assertEquals(20, frame[i].toInt() and 0xFF)
        }
    }

    @Test
    fun generateFrame_zeroAmbient_isBlack() {
        val camera = SimulatedCamera(
            width = 100,
            height = 100,
            noiseLevel = 0f,
            ambientLevel = 0
        )
        val frame = camera.generateFrame(emptyList())

        for (i in frame.indices) {
            assertEquals(0, frame[i].toInt() and 0xFF)
        }
    }

    // ------------------------------------------------------------------ //
    //  Blob rendering                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun generateFrame_singleFixtureOn_hasBrightBlob() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 0,
            blobRadius = 20f,
            // Camera at origin, looking forward (positive y), fixture directly ahead
            cameraPosition = Vec3(0f, -5f, 1.5f),
            cameraTarget = Vec3(0f, 2f, 2.5f)
        )

        val fixture = makeFixture3D("test", 0f, 1f, 2.5f)
        val states = listOf(FixtureState(fixture, Color.WHITE))
        val frame = camera.generateFrame(states)

        // The fixture should project near the center of the frame
        // Find the brightest pixel
        var maxBrightness = 0
        for (i in frame.indices) {
            val value = frame[i].toInt() and 0xFF
            if (value > maxBrightness) maxBrightness = value
        }

        assertTrue(maxBrightness > 100, "Expected bright blob, got max brightness $maxBrightness")
    }

    @Test
    fun generateFrame_fixtureOff_noBlob() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 0,
            blobRadius = 20f
        )

        val fixture = makeFixture3D("test", 0f, 1f, 2.5f)
        val states = listOf(FixtureState(fixture, Color.BLACK))
        val frame = camera.generateFrame(states)

        // All pixels should be 0 (black)
        var maxBrightness = 0
        for (i in frame.indices) {
            val value = frame[i].toInt() and 0xFF
            if (value > maxBrightness) maxBrightness = value
        }
        assertEquals(0, maxBrightness)
    }

    @Test
    fun generateFrame_redFixture_dimmerThanWhite() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 0,
            blobRadius = 20f
        )

        val fixture = makeFixture3D("test", 0f, 1f, 2.5f)

        // White fixture
        val whiteFrame = camera.generateFrame(listOf(FixtureState(fixture, Color.WHITE)))
        var whitePeak = 0
        for (b in whiteFrame) {
            val v = b.toInt() and 0xFF
            if (v > whitePeak) whitePeak = v
        }

        // Red fixture (luminance ~0.299)
        val redFrame = camera.generateFrame(listOf(FixtureState(fixture, Color.RED)))
        var redPeak = 0
        for (b in redFrame) {
            val v = b.toInt() and 0xFF
            if (v > redPeak) redPeak = v
        }

        assertTrue(redPeak < whitePeak, "Red ($redPeak) should be dimmer than white ($whitePeak)")
        assertTrue(redPeak > 0, "Red should be visible")
    }

    // ------------------------------------------------------------------ //
    //  Multiple fixtures                                                  //
    // ------------------------------------------------------------------ //

    @Test
    fun generateFrame_twoFixtures_twoBrightSpots() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 0,
            blobRadius = 10f,
            cameraPosition = Vec3(0f, -8f, 2f),
            cameraTarget = Vec3(0f, 0f, 2.5f)
        )

        // Two fixtures separated horizontally
        val left = makeFixture3D("left", -3f, 1f, 2.5f)
        val right = makeFixture3D("right", 3f, 1f, 2.5f)

        val states = listOf(
            FixtureState(left, Color.WHITE),
            FixtureState(right, Color.WHITE)
        )
        val frame = camera.generateFrame(states)

        // Both should produce bright spots
        val leftProj = camera.projectToImage(left.position)
        val rightProj = camera.projectToImage(right.position)

        assertTrue(leftProj.isVisible, "Left fixture should be visible")
        assertTrue(rightProj.isVisible, "Right fixture should be visible")

        val leftBrightness = camera.peakBrightness(
            frame,
            leftProj.x.toInt().coerceIn(0, camera.width - 1),
            leftProj.y.toInt().coerceIn(0, camera.height - 1),
            searchRadius = 20
        )
        val rightBrightness = camera.peakBrightness(
            frame,
            rightProj.x.toInt().coerceIn(0, camera.width - 1),
            rightProj.y.toInt().coerceIn(0, camera.height - 1),
            searchRadius = 20
        )

        assertTrue(leftBrightness > 50, "Left fixture should be bright, got $leftBrightness")
        assertTrue(rightBrightness > 50, "Right fixture should be bright, got $rightBrightness")
    }

    // ------------------------------------------------------------------ //
    //  Simple on/off API                                                  //
    // ------------------------------------------------------------------ //

    @Test
    fun generateFrame_onOffApi_works() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 0,
            blobRadius = 15f
        )

        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val onIds = setOf("dj-par-0", "dj-par-4")

        val frame = camera.generateFrame(rig.fixtures, onIds)
        assertEquals(640 * 480, frame.size)

        // There should be some bright pixels (from the two on fixtures)
        var maxBrightness = 0
        for (b in frame) {
            val v = b.toInt() and 0xFF
            if (v > maxBrightness) maxBrightness = v
        }
        assertTrue(maxBrightness > 50, "Expected some bright pixels from on fixtures")
    }

    // ------------------------------------------------------------------ //
    //  Noise                                                              //
    // ------------------------------------------------------------------ //

    @Test
    fun generateFrame_withNoise_variesPixels() {
        val camera = SimulatedCamera(
            width = 100,
            height = 100,
            noiseLevel = 0.1f,
            ambientLevel = 128,
            random = Random(42)
        )

        val frame = camera.generateFrame(emptyList())

        // With noise, not all pixels should be exactly ambient level
        var differentCount = 0
        for (i in frame.indices) {
            val value = frame[i].toInt() and 0xFF
            if (value != 128) differentCount++
        }
        assertTrue(differentCount > 100, "Noise should cause pixel variation, got $differentCount different pixels")
    }

    @Test
    fun generateFrame_zeroNoise_uniformAmbient() {
        val camera = SimulatedCamera(
            width = 100,
            height = 100,
            noiseLevel = 0f,
            ambientLevel = 50
        )

        val frame = camera.generateFrame(emptyList())

        for (i in frame.indices) {
            assertEquals(50, frame[i].toInt() and 0xFF)
        }
    }

    // ------------------------------------------------------------------ //
    //  Projection                                                         //
    // ------------------------------------------------------------------ //

    @Test
    fun projectToImage_fixtureInFront_isVisible() {
        val camera = SimulatedCamera(
            cameraPosition = Vec3(0f, -5f, 2f),
            cameraTarget = Vec3(0f, 0f, 2f)
        )

        val projected = camera.projectToImage(Vec3(0f, 0f, 2f))
        assertTrue(projected.isVisible)
        assertTrue(projected.depth > 0)
    }

    @Test
    fun projectToImage_fixtureBehind_notVisible() {
        val camera = SimulatedCamera(
            cameraPosition = Vec3(0f, -5f, 2f),
            cameraTarget = Vec3(0f, 0f, 2f)
        )

        // Behind the camera
        val projected = camera.projectToImage(Vec3(0f, -10f, 2f))
        assertTrue(!projected.isVisible)
    }

    @Test
    fun projectToImage_centerFixture_nearImageCenter() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            cameraPosition = Vec3(0f, -5f, 2.5f),
            cameraTarget = Vec3(0f, 0f, 2.5f)
        )

        val projected = camera.projectToImage(Vec3(0f, 0f, 2.5f))
        assertTrue(projected.isVisible)

        // Should be approximately in the center of the image
        val centerX = camera.width / 2f
        val centerY = camera.height / 2f

        assertTrue(
            kotlin.math.abs(projected.x - centerX) < 50f,
            "Expected near center x ($centerX), got ${projected.x}"
        )
        assertTrue(
            kotlin.math.abs(projected.y - centerY) < 50f,
            "Expected near center y ($centerY), got ${projected.y}"
        )
    }

    @Test
    fun projectToImage_leftFixture_leftOfCenter() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            cameraPosition = Vec3(0f, -5f, 2.5f),
            cameraTarget = Vec3(0f, 0f, 2.5f)
        )

        val centerProj = camera.projectToImage(Vec3(0f, 0f, 2.5f))
        val leftProj = camera.projectToImage(Vec3(-2f, 0f, 2.5f))

        assertTrue(leftProj.isVisible)
        assertTrue(leftProj.x < centerProj.x,
            "Left fixture should project left of center: left=${leftProj.x}, center=${centerProj.x}")
    }

    // ------------------------------------------------------------------ //
    //  FixtureState                                                       //
    // ------------------------------------------------------------------ //

    @Test
    fun fixtureState_isOn_detectsNonZero() {
        val fixture = makeFixture3D("test", 0f, 0f, 0f)

        val on = FixtureState(fixture, Color.RED)
        val off = FixtureState(fixture, Color.BLACK)
        val dim = FixtureState(fixture, Color(0.01f, 0f, 0f))
        val veryDim = FixtureState(fixture, Color(0.0001f, 0f, 0f))

        assertTrue(on.isOn)
        assertTrue(!off.isOn)
        assertTrue(dim.isOn)
        assertTrue(!veryDim.isOn) // Below 0.001 threshold
    }

    @Test
    fun fixtureState_brightness_whiteis255() {
        val fixture = makeFixture3D("test", 0f, 0f, 0f)
        val state = FixtureState(fixture, Color.WHITE)
        assertEquals(255, state.brightness)
    }

    @Test
    fun fixtureState_brightness_blackIs0() {
        val fixture = makeFixture3D("test", 0f, 0f, 0f)
        val state = FixtureState(fixture, Color.BLACK)
        assertEquals(0, state.brightness)
    }

    @Test
    fun fixtureState_brightness_redIsLuminance() {
        val fixture = makeFixture3D("test", 0f, 0f, 0f)
        val state = FixtureState(fixture, Color.RED)
        // Luminance of pure red: 0.299 * 1.0 = 0.299, 0.299 * 255 = 76.245 -> 76
        val expected = (0.299f * 255f).roundToInt()
        assertEquals(expected, state.brightness)
    }

    // ------------------------------------------------------------------ //
    //  Pixel accessor                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun getPixel_returnsCorrectValue() {
        val camera = SimulatedCamera(width = 10, height = 10, noiseLevel = 0f, ambientLevel = 42)
        val frame = camera.generateFrame(emptyList())

        assertEquals(42, camera.getPixel(frame, 0, 0))
        assertEquals(42, camera.getPixel(frame, 5, 5))
        assertEquals(42, camera.getPixel(frame, 9, 9))
    }

    @Test
    fun peakBrightness_findsMaxInRegion() {
        val camera = SimulatedCamera(width = 100, height = 100, noiseLevel = 0f, ambientLevel = 0)
        val frame = ByteArray(100 * 100)
        // Set a bright pixel at (50, 50)
        frame[50 * 100 + 50] = 200.toByte()

        val peak = camera.peakBrightness(frame, 50, 50, searchRadius = 5)
        assertEquals(200, peak)
    }

    // ------------------------------------------------------------------ //
    //  Integration with fixture rig                                       //
    // ------------------------------------------------------------------ //

    @Test
    fun generateFrame_djRig_allOn_hasBrightSpots() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 0,
            blobRadius = 12f,
            cameraPosition = Vec3(0f, -6f, 1.5f),
            cameraTarget = Vec3(0f, 1f, 2.5f)
        )

        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val allOnIds = rig.fixtures.map { it.fixture.fixtureId }.toSet()

        val frame = camera.generateFrame(rig.fixtures, allOnIds)

        // Should have bright pixels
        var maxBrightness = 0
        var brightPixelCount = 0
        for (b in frame) {
            val v = b.toInt() and 0xFF
            if (v > maxBrightness) maxBrightness = v
            if (v > 50) brightPixelCount++
        }

        assertTrue(maxBrightness > 100, "Expected bright blobs, got max $maxBrightness")
        assertTrue(brightPixelCount > 20, "Expected multiple bright pixels, got $brightPixelCount")
    }
}

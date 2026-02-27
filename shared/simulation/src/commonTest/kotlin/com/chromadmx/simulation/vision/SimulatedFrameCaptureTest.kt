package com.chromadmx.simulation.vision

import com.chromadmx.core.model.Vec3
import com.chromadmx.vision.detection.BlobDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SimulatedFrameCaptureTest {

    private val pixelPositions = mapOf(
        "bar-1" to (0 until 24).map { px ->
            Vec3(x = -2.0f, y = 3.0f, z = 0.5f + px * 0.052f)
        },
        "bar-2" to (0 until 24).map { px ->
            Vec3(x = 1.0f, y = 1.5f, z = 0.5f + px * 0.052f)
        },
    )

    private fun createCapture(controller: SimulatedDmxController): SimulatedFrameCapture {
        return SimulatedFrameCapture(
            pixelPositions = pixelPositions,
            dmxController = controller,
            frameWidth = 320,
            frameHeight = 240,
        )
    }

    @Test
    fun blackoutProducesNoBlobs() = runTest {
        val controller = SimulatedDmxController()
        val capture = createCapture(controller)
        controller.blackout()

        val frame = capture.captureFrame()
        val blobs = BlobDetector(brightnessThreshold = 0.2f).detect(frame)
        assertEquals(0, blobs.size)
    }

    @Test
    fun singleFiredFixtureProducesOneBlob() = runTest {
        val controller = SimulatedDmxController()
        val capture = createCapture(controller)
        controller.fireFixture("bar-1")

        val frame = capture.captureFrame()
        val blobs = BlobDetector(brightnessThreshold = 0.2f).detect(frame)
        assertTrue(blobs.isNotEmpty(), "Should detect at least 1 blob")
    }

    @Test
    fun twoFiredFixturesProduceTwoBlobs() = runTest {
        val controller = SimulatedDmxController()
        val capture = createCapture(controller)
        controller.fireFixture("bar-1")
        controller.fireFixture("bar-2")

        val frame = capture.captureFrame()
        val blobs = BlobDetector(brightnessThreshold = 0.2f).detect(frame)
        assertTrue(blobs.size >= 2, "Should detect at least 2 blobs, got ${blobs.size}")
    }

    @Test
    fun blobPositionMatchesPhysicalPosition() = runTest {
        val controller = SimulatedDmxController()
        val capture = createCapture(controller)
        controller.fireFixture("bar-2")

        val frame = capture.captureFrame()
        val blobs = BlobDetector(brightnessThreshold = 0.2f).detect(frame)
        assertTrue(blobs.isNotEmpty())

        val centroid = blobs[0].centroid
        assertTrue(centroid.x > 160f, "Bar at x=1.0 should be in right half of frame, got ${centroid.x}")
    }
}

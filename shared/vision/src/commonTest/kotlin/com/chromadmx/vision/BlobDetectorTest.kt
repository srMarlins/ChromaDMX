package com.chromadmx.vision

import com.chromadmx.vision.detection.BlobDetector
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlobDetectorTest {

    private val detector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1)

    // -----------------------------------------------------------------------
    // Basic detection
    // -----------------------------------------------------------------------

    @Test
    fun detects_nothing_in_blank_frame() {
        val frame = SyntheticFrameHelper.blankFrame(100, 100)
        val blobs = detector.detect(frame)
        assertTrue(blobs.isEmpty(), "No blobs expected in blank frame")
    }

    @Test
    fun detects_nothing_when_all_pixels_below_threshold() {
        val frame = SyntheticFrameHelper.uniformFrame(50, 50, 0.29f)
        val blobs = detector.detect(frame)
        assertTrue(blobs.isEmpty(), "No blobs expected when all pixels below threshold")
    }

    @Test
    fun detects_single_pixel_blob() {
        val frame = SyntheticFrameHelper.pixelFrame(
            10, 10,
            mapOf((5 to 5) to 0.8f)
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "Should detect exactly one blob")
        assertEquals(5f, blobs[0].centroid.x, 0.01f)
        assertEquals(5f, blobs[0].centroid.y, 0.01f)
        assertEquals(1, blobs[0].pixelCount)
    }

    @Test
    fun detects_single_bright_spot() {
        val frame = SyntheticFrameHelper.singleSpot(
            width = 100, height = 100,
            cx = 50f, cy = 50f,
            radius = 5f,
            brightness = 1.0f
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "Should detect exactly one blob")

        val blob = blobs[0]
        assertTrue(abs(blob.centroid.x - 50f) < 1.5f, "Centroid X should be near 50, got ${blob.centroid.x}")
        assertTrue(abs(blob.centroid.y - 50f) < 1.5f, "Centroid Y should be near 50, got ${blob.centroid.y}")
        assertTrue(blob.pixelCount > 1, "Blob should have multiple pixels")
    }

    // -----------------------------------------------------------------------
    // Multiple blobs
    // -----------------------------------------------------------------------

    @Test
    fun detects_two_separate_blobs() {
        val frame = SyntheticFrameHelper.multipleSpots(
            width = 200, height = 100,
            spots = listOf(
                SyntheticFrameHelper.SpotSpec(cx = 30f, cy = 50f, radius = 8f),
                SyntheticFrameHelper.SpotSpec(cx = 170f, cy = 50f, radius = 8f)
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(2, blobs.size, "Should detect two blobs, got ${blobs.size}")

        // Both centroids should be near their spot centers
        val centroids = blobs.map { it.centroid }
        val nearFirst = centroids.any { abs(it.x - 30f) < 2f && abs(it.y - 50f) < 2f }
        val nearSecond = centroids.any { abs(it.x - 170f) < 2f && abs(it.y - 50f) < 2f }
        assertTrue(nearFirst, "One centroid should be near (30, 50)")
        assertTrue(nearSecond, "One centroid should be near (170, 50)")
    }

    @Test
    fun detects_three_blobs_at_different_positions() {
        val frame = SyntheticFrameHelper.multipleSpots(
            width = 200, height = 200,
            spots = listOf(
                SyntheticFrameHelper.SpotSpec(cx = 30f, cy = 30f, radius = 6f),
                SyntheticFrameHelper.SpotSpec(cx = 100f, cy = 170f, radius = 6f),
                SyntheticFrameHelper.SpotSpec(cx = 170f, cy = 80f, radius = 6f)
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(3, blobs.size, "Should detect three blobs, got ${blobs.size}")
    }

    // -----------------------------------------------------------------------
    // 4-connectivity
    // -----------------------------------------------------------------------

    @Test
    fun four_connectivity_does_not_connect_diagonal_pixels() {
        // Place two pixels diagonally adjacent — they should be separate blobs
        val frame = SyntheticFrameHelper.pixelFrame(
            10, 10,
            mapOf(
                (3 to 3) to 0.8f,
                (4 to 4) to 0.8f
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(2, blobs.size, "Diagonal pixels should be separate blobs with 4-connectivity")
    }

    @Test
    fun four_connectivity_connects_horizontal_neighbors() {
        val frame = SyntheticFrameHelper.pixelFrame(
            10, 10,
            mapOf(
                (3 to 5) to 0.8f,
                (4 to 5) to 0.8f,
                (5 to 5) to 0.8f
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "Horizontal neighbors should form one blob")
        assertEquals(3, blobs[0].pixelCount)
    }

    @Test
    fun four_connectivity_connects_vertical_neighbors() {
        val frame = SyntheticFrameHelper.pixelFrame(
            10, 10,
            mapOf(
                (5 to 3) to 0.8f,
                (5 to 4) to 0.8f,
                (5 to 5) to 0.8f
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "Vertical neighbors should form one blob")
        assertEquals(3, blobs[0].pixelCount)
    }

    @Test
    fun four_connectivity_connects_l_shape() {
        // L-shaped region:
        //   X .
        //   X .
        //   X X
        val frame = SyntheticFrameHelper.pixelFrame(
            10, 10,
            mapOf(
                (2 to 2) to 0.5f,
                (2 to 3) to 0.5f,
                (2 to 4) to 0.5f,
                (3 to 4) to 0.5f
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "L-shape should form one connected blob")
        assertEquals(4, blobs[0].pixelCount)
    }

    // -----------------------------------------------------------------------
    // Centroid accuracy
    // -----------------------------------------------------------------------

    @Test
    fun centroid_is_brightness_weighted() {
        // Two pixels with different brightness — centroid should be pulled
        // toward the brighter pixel
        val frame = SyntheticFrameHelper.pixelFrame(
            10, 10,
            mapOf(
                (2 to 5) to 0.4f,  // dimmer
                (3 to 5) to 0.8f   // brighter
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size)
        val cx = blobs[0].centroid.x
        // Weighted: (2*0.4 + 3*0.8) / (0.4 + 0.8) = (0.8 + 2.4) / 1.2 = 2.667
        assertTrue(cx > 2.5f, "Centroid should be pulled toward brighter pixel, got $cx")
        assertTrue(abs(cx - 2.667f) < 0.01f, "Centroid X should be ~2.667, got $cx")
    }

    @Test
    fun centroid_of_uniform_rectangle_is_at_center() {
        // 3x3 block of uniform brightness at position (4,4)-(6,6)
        val pixels = mutableMapOf<Pair<Int, Int>, Float>()
        for (x in 4..6) {
            for (y in 4..6) {
                pixels[x to y] = 0.5f
            }
        }
        val frame = SyntheticFrameHelper.pixelFrame(10, 10, pixels)
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size)
        // Centroid of uniform block is at geometric center (5, 5)
        assertEquals(5f, blobs[0].centroid.x, 0.01f)
        assertEquals(5f, blobs[0].centroid.y, 0.01f)
    }

    // -----------------------------------------------------------------------
    // Minimum blob size filtering
    // -----------------------------------------------------------------------

    @Test
    fun filters_blobs_smaller_than_minBlobSize() {
        val bigMinDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 5)
        val frame = SyntheticFrameHelper.pixelFrame(
            20, 20,
            mapOf(
                // Small blob (2 pixels) — should be filtered
                (3 to 3) to 0.8f,
                (4 to 3) to 0.8f,
                // Large blob (6 pixels) — should be kept
                (15 to 15) to 0.8f,
                (16 to 15) to 0.8f,
                (17 to 15) to 0.8f,
                (15 to 16) to 0.8f,
                (16 to 16) to 0.8f,
                (17 to 16) to 0.8f
            )
        )
        val blobs = bigMinDetector.detect(frame)
        assertEquals(1, blobs.size, "Only the large blob should pass min-size filter")
        assertEquals(6, blobs[0].pixelCount)
    }

    // -----------------------------------------------------------------------
    // Sorting by brightness
    // -----------------------------------------------------------------------

    @Test
    fun blobs_are_sorted_by_total_brightness_descending() {
        val frame = SyntheticFrameHelper.multipleSpots(
            width = 200, height = 100,
            spots = listOf(
                SyntheticFrameHelper.SpotSpec(cx = 30f, cy = 50f, radius = 3f, brightness = 0.4f),
                SyntheticFrameHelper.SpotSpec(cx = 100f, cy = 50f, radius = 3f, brightness = 1.0f),
                SyntheticFrameHelper.SpotSpec(cx = 170f, cy = 50f, radius = 3f, brightness = 0.6f)
            )
        )
        val blobs = detector.detect(frame)
        assertEquals(3, blobs.size)
        // Sorted descending by total brightness
        assertTrue(blobs[0].totalBrightness >= blobs[1].totalBrightness)
        assertTrue(blobs[1].totalBrightness >= blobs[2].totalBrightness)
    }

    // -----------------------------------------------------------------------
    // Ambient subtraction integration
    // -----------------------------------------------------------------------

    @Test
    fun ambient_subtraction_removes_background_noise() {
        // Ambient has uniform low brightness
        val ambient = SyntheticFrameHelper.uniformFrame(100, 100, 0.2f)
        // Captured has the same background plus a bright spot
        val captured = SyntheticFrameHelper.singleSpot(
            width = 100, height = 100,
            cx = 50f, cy = 50f,
            radius = 5f,
            brightness = 0.9f
        )
        // Add ambient to the captured frame
        val capturedWithAmbient = com.chromadmx.vision.camera.GrayscaleFrame(
            FloatArray(100 * 100) { i ->
                (captured.pixels[i] + ambient.pixels[i]).coerceIn(0f, 1f)
            },
            100, 100
        )

        // Without subtraction, the uniform background exceeds threshold
        val blobsWithoutSub = BlobDetector(brightnessThreshold = 0.15f).detect(capturedWithAmbient)
        // The entire image might be one giant blob because background >= 0.2
        assertTrue(blobsWithoutSub.isNotEmpty())

        // With subtraction, only the spot remains
        val diff = capturedWithAmbient.subtract(ambient)
        val blobsWithSub = BlobDetector(brightnessThreshold = 0.3f).detect(diff)
        assertEquals(1, blobsWithSub.size, "After subtraction, only the spot should remain")
        assertTrue(abs(blobsWithSub[0].centroid.x - 50f) < 2f)
        assertTrue(abs(blobsWithSub[0].centroid.y - 50f) < 2f)
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    fun handles_blob_at_frame_edge() {
        // Spot partially outside frame (at corner)
        val frame = SyntheticFrameHelper.singleSpot(
            width = 50, height = 50,
            cx = 0f, cy = 0f,
            radius = 5f,
            brightness = 1.0f
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "Should detect blob at frame edge")
        // Centroid should be in the visible part
        assertTrue(blobs[0].centroid.x >= 0f)
        assertTrue(blobs[0].centroid.y >= 0f)
    }

    @Test
    fun handles_entire_frame_above_threshold() {
        val frame = SyntheticFrameHelper.uniformFrame(20, 20, 0.5f)
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "Entire frame should be one blob")
        assertEquals(400, blobs[0].pixelCount)
        // Centroid should be at frame center
        assertEquals(9.5f, blobs[0].centroid.x, 0.01f)
        assertEquals(9.5f, blobs[0].centroid.y, 0.01f)
    }

    @Test
    fun handles_1x1_frame() {
        val frame = com.chromadmx.vision.camera.GrayscaleFrame(floatArrayOf(0.5f), 1, 1)
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size)
        assertEquals(0f, blobs[0].centroid.x, 0.01f)
        assertEquals(0f, blobs[0].centroid.y, 0.01f)
    }

    @Test
    fun threshold_boundary_pixel_exactly_at_threshold_is_included() {
        val frame = SyntheticFrameHelper.pixelFrame(
            5, 5,
            mapOf((2 to 2) to 0.3f)  // exactly at threshold
        )
        val blobs = detector.detect(frame)
        assertEquals(1, blobs.size, "Pixel exactly at threshold should be included")
    }

    @Test
    fun threshold_boundary_pixel_just_below_threshold_is_excluded() {
        val frame = SyntheticFrameHelper.pixelFrame(
            5, 5,
            mapOf((2 to 2) to 0.2999f)
        )
        val blobs = detector.detect(frame)
        assertTrue(blobs.isEmpty(), "Pixel just below threshold should be excluded")
    }

    // -----------------------------------------------------------------------
    // Configuration validation
    // -----------------------------------------------------------------------

    @Test
    fun rejects_invalid_threshold() {
        assertFailsWith<IllegalArgumentException> { BlobDetector(brightnessThreshold = -0.1f) }
        assertFailsWith<IllegalArgumentException> { BlobDetector(brightnessThreshold = 1.1f) }
    }

    @Test
    fun rejects_invalid_minBlobSize() {
        assertFailsWith<IllegalArgumentException> { BlobDetector(minBlobSize = 0) }
    }

    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName} but no exception was thrown")
        } catch (e: Throwable) {
            if (e !is T) throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}", e)
        }
    }
}

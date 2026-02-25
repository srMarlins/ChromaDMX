package com.chromadmx.vision

import com.chromadmx.vision.detection.Coord2D
import com.chromadmx.vision.mapping.PixelInterpolator
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelInterpolatorTest {

    // -----------------------------------------------------------------------
    // Basic interpolation
    // -----------------------------------------------------------------------

    @Test
    fun interpolate_two_pixels_returns_start_and_end() {
        val start = Coord2D(10f, 20f)
        val end = Coord2D(30f, 40f)
        val positions = PixelInterpolator.interpolate(start, end, 2)

        assertEquals(2, positions.size)
        assertEquals(10f, positions[0].x, 0.01f)
        assertEquals(20f, positions[0].y, 0.01f)
        assertEquals(30f, positions[1].x, 0.01f)
        assertEquals(40f, positions[1].y, 0.01f)
    }

    @Test
    fun interpolate_three_pixels_includes_midpoint() {
        val start = Coord2D(0f, 0f)
        val end = Coord2D(100f, 50f)
        val positions = PixelInterpolator.interpolate(start, end, 3)

        assertEquals(3, positions.size)
        assertEquals(0f, positions[0].x, 0.01f)
        assertEquals(0f, positions[0].y, 0.01f)
        assertEquals(50f, positions[1].x, 0.01f)
        assertEquals(25f, positions[1].y, 0.01f)
        assertEquals(100f, positions[2].x, 0.01f)
        assertEquals(50f, positions[2].y, 0.01f)
    }

    @Test
    fun interpolate_five_pixels_on_horizontal_line() {
        val start = Coord2D(10f, 50f)
        val end = Coord2D(90f, 50f)
        val positions = PixelInterpolator.interpolate(start, end, 5)

        assertEquals(5, positions.size)
        assertEquals(10f, positions[0].x, 0.01f)
        assertEquals(30f, positions[1].x, 0.01f)
        assertEquals(50f, positions[2].x, 0.01f)
        assertEquals(70f, positions[3].x, 0.01f)
        assertEquals(90f, positions[4].x, 0.01f)
        // All Y values should be 50
        positions.forEach { assertEquals(50f, it.y, 0.01f) }
    }

    @Test
    fun interpolate_on_vertical_line() {
        val start = Coord2D(25f, 10f)
        val end = Coord2D(25f, 90f)
        val positions = PixelInterpolator.interpolate(start, end, 5)

        assertEquals(5, positions.size)
        positions.forEach { assertEquals(25f, it.x, 0.01f) }
        assertEquals(10f, positions[0].y, 0.01f)
        assertEquals(30f, positions[1].y, 0.01f)
        assertEquals(50f, positions[2].y, 0.01f)
        assertEquals(70f, positions[3].y, 0.01f)
        assertEquals(90f, positions[4].y, 0.01f)
    }

    @Test
    fun interpolate_on_diagonal_line() {
        val start = Coord2D(0f, 0f)
        val end = Coord2D(80f, 60f)
        val positions = PixelInterpolator.interpolate(start, end, 5)

        assertEquals(5, positions.size)
        assertEquals(0f, positions[0].x, 0.01f)
        assertEquals(0f, positions[0].y, 0.01f)
        assertEquals(20f, positions[1].x, 0.01f)
        assertEquals(15f, positions[1].y, 0.01f)
        assertEquals(40f, positions[2].x, 0.01f)
        assertEquals(30f, positions[2].y, 0.01f)
        assertEquals(60f, positions[3].x, 0.01f)
        assertEquals(45f, positions[3].y, 0.01f)
        assertEquals(80f, positions[4].x, 0.01f)
        assertEquals(60f, positions[4].y, 0.01f)
    }

    // -----------------------------------------------------------------------
    // interpolateAt
    // -----------------------------------------------------------------------

    @Test
    fun interpolateAt_matches_full_interpolation() {
        val start = Coord2D(10f, 20f)
        val end = Coord2D(50f, 80f)
        val pixelCount = 7
        val fullResult = PixelInterpolator.interpolate(start, end, pixelCount)

        for (i in 0 until pixelCount) {
            val single = PixelInterpolator.interpolateAt(start, end, pixelCount, i)
            assertEquals(fullResult[i].x, single.x, 0.001f)
            assertEquals(fullResult[i].y, single.y, 0.001f)
        }
    }

    // -----------------------------------------------------------------------
    // Spacing uniformity
    // -----------------------------------------------------------------------

    @Test
    fun pixels_are_evenly_spaced() {
        val start = Coord2D(10f, 10f)
        val end = Coord2D(110f, 110f)
        val positions = PixelInterpolator.interpolate(start, end, 11)

        // Compute distances between consecutive pixels
        val distances = (0 until positions.size - 1).map { i ->
            val dx = positions[i + 1].x - positions[i].x
            val dy = positions[i + 1].y - positions[i].y
            kotlin.math.sqrt(dx * dx + dy * dy)
        }

        // All distances should be equal
        val expectedDist = distances[0]
        distances.forEach { dist ->
            assertTrue(abs(dist - expectedDist) < 0.01f,
                "Expected uniform spacing of $expectedDist but got $dist")
        }
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    @Test
    fun rejects_pixelCount_less_than_two() {
        val start = Coord2D(0f, 0f)
        val end = Coord2D(10f, 10f)
        try {
            PixelInterpolator.interpolate(start, end, 1)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun interpolateAt_rejects_out_of_range_index() {
        val start = Coord2D(0f, 0f)
        val end = Coord2D(10f, 10f)
        try {
            PixelInterpolator.interpolateAt(start, end, 5, 5)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}

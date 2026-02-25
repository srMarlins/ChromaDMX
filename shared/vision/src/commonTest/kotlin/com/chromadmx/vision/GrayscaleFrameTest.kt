package com.chromadmx.vision

import com.chromadmx.vision.camera.GrayscaleFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GrayscaleFrameTest {

    @Test
    fun pixelAt_returns_correct_value() {
        val frame = GrayscaleFrame(
            pixels = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f),
            width = 3,
            height = 2
        )
        assertEquals(0.1f, frame.pixelAt(0, 0))
        assertEquals(0.2f, frame.pixelAt(1, 0))
        assertEquals(0.3f, frame.pixelAt(2, 0))
        assertEquals(0.4f, frame.pixelAt(0, 1))
        assertEquals(0.6f, frame.pixelAt(2, 1))
    }

    @Test
    fun pixelAt_throws_on_out_of_bounds() {
        val frame = GrayscaleFrame(FloatArray(4), 2, 2)
        assertFailsWith<IllegalArgumentException> { frame.pixelAt(-1, 0) }
        assertFailsWith<IllegalArgumentException> { frame.pixelAt(0, 2) }
        assertFailsWith<IllegalArgumentException> { frame.pixelAt(2, 0) }
    }

    @Test
    fun constructor_rejects_wrong_pixel_count() {
        assertFailsWith<IllegalArgumentException> {
            GrayscaleFrame(FloatArray(5), 2, 2)
        }
    }

    @Test
    fun subtract_clamps_negative_values_to_zero() {
        val captured = GrayscaleFrame(floatArrayOf(0.5f, 0.2f, 0.8f, 0.1f), 2, 2)
        val baseline = GrayscaleFrame(floatArrayOf(0.3f, 0.4f, 0.1f, 0.1f), 2, 2)
        val diff = captured.subtract(baseline)

        // 0.5 - 0.3 = 0.2
        assertEquals(0.2f, diff.pixelAt(0, 0), 0.001f)
        // 0.2 - 0.4 = -0.2 -> clamped to 0
        assertEquals(0.0f, diff.pixelAt(1, 0), 0.001f)
        // 0.8 - 0.1 = 0.7
        assertEquals(0.7f, diff.pixelAt(0, 1), 0.001f)
        // 0.1 - 0.1 = 0.0
        assertEquals(0.0f, diff.pixelAt(1, 1), 0.001f)
    }

    @Test
    fun subtract_rejects_mismatched_dimensions() {
        val a = GrayscaleFrame(FloatArray(6), 3, 2)
        val b = GrayscaleFrame(FloatArray(6), 2, 3)
        assertFailsWith<IllegalArgumentException> { a.subtract(b) }
    }

    @Test
    fun equality_is_based_on_content() {
        val a = GrayscaleFrame(floatArrayOf(0.1f, 0.2f), 2, 1)
        val b = GrayscaleFrame(floatArrayOf(0.1f, 0.2f), 2, 1)
        assertTrue(a == b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}

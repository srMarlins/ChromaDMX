package com.chromadmx.core.util

import com.chromadmx.core.model.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DmxExtensionsTest {

    /* ---- packColor / unpackColor ---- */

    @Test
    fun packAndUnpackColorRoundTrip() {
        val buf = ByteArray(6)
        val color = Color(1f, 0.5f, 0f)
        buf.packColor(color, 0)

        val restored = buf.unpackColor(0)
        assertApprox(1f, restored.r)
        assertApprox(0.502f, restored.g, 0.005f)
        assertApprox(0f, restored.b)
    }

    @Test
    fun packColorAtOffset() {
        val buf = ByteArray(6)
        buf.packColor(Color.RED, 3)

        assertEquals(255.toByte(), buf[3])
        assertEquals(0.toByte(), buf[4])
        assertEquals(0.toByte(), buf[5])

        // First 3 bytes untouched
        assertEquals(0.toByte(), buf[0])
    }

    @Test
    fun packColorRejectsInvalidOffset() {
        val buf = ByteArray(3)
        assertFailsWith<IllegalArgumentException> {
            buf.packColor(Color.RED, 1)
        }
    }

    @Test
    fun unpackColorRejectsInvalidOffset() {
        val buf = ByteArray(3)
        assertFailsWith<IllegalArgumentException> {
            buf.unpackColor(1)
        }
    }

    /* ---- packColors / unpackColors ---- */

    @Test
    fun packAndUnpackMultipleColors() {
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE)
        val buf = ByteArray(9)
        buf.packColors(colors)

        val restored = buf.unpackColors(3)
        assertEquals(3, restored.size)
        assertColorApprox(Color.RED, restored[0])
        assertColorApprox(Color.GREEN, restored[1])
        assertColorApprox(Color.BLUE, restored[2])
    }

    @Test
    fun packColorsWithOffset() {
        val colors = listOf(Color.WHITE)
        val buf = ByteArray(6)
        buf.packColors(colors, offset = 3)

        val restored = buf.unpackColors(1, offset = 3)
        assertColorApprox(Color.WHITE, restored[0])

        // First 3 bytes should still be zero
        assertEquals(0.toByte(), buf[0])
        assertEquals(0.toByte(), buf[1])
        assertEquals(0.toByte(), buf[2])
    }

    @Test
    fun packColorsRejectsOverflow() {
        val buf = ByteArray(5)
        assertFailsWith<IllegalArgumentException> {
            buf.packColors(listOf(Color.RED, Color.GREEN))
        }
    }

    /* ---- setChannel / getChannel ---- */

    @Test
    fun setAndGetChannel() {
        val buf = ByteArray(4)
        buf.setChannel(2, 200)
        assertEquals(200, buf.getChannel(2))
    }

    @Test
    fun setChannelClampsValue() {
        val buf = ByteArray(2)
        buf.setChannel(0, 300)
        assertEquals(255, buf.getChannel(0))

        buf.setChannel(1, -5)
        assertEquals(0, buf.getChannel(1))
    }

    @Test
    fun getChannelUnsigned() {
        // 0xFF as signed byte = -1, but as unsigned channel = 255
        val buf = byteArrayOf((-1).toByte())
        assertEquals(255, buf.getChannel(0))
    }

    @Test
    fun setChannelRejectsOutOfRange() {
        val buf = ByteArray(3)
        assertFailsWith<IllegalArgumentException> {
            buf.setChannel(3, 0)
        }
    }

    @Test
    fun getChannelRejectsOutOfRange() {
        val buf = ByteArray(3)
        assertFailsWith<IllegalArgumentException> {
            buf.getChannel(3)
        }
    }

    /* ---- helpers ---- */

    private fun assertApprox(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(
            kotlin.math.abs(expected - actual) < eps,
            "Expected ~$expected but got $actual"
        )
    }

    private fun assertColorApprox(expected: Color, actual: Color, eps: Float = 0.01f) {
        assertApprox(expected.r, actual.r, eps)
        assertApprox(expected.g, actual.g, eps)
        assertApprox(expected.b, actual.b, eps)
    }
}

package com.chromadmx.engine.util

import com.chromadmx.core.model.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorUtilsTest {

    /* ------------------------------------------------------------------ */
    /*  hsvToRgb                                                           */
    /* ------------------------------------------------------------------ */

    @Test
    fun hsvRedAtZeroDegrees() {
        val c = ColorUtils.hsvToRgb(0f, 1f, 1f)
        assertEquals(1f, c.r, 0.01f)
        assertEquals(0f, c.g, 0.01f)
        assertEquals(0f, c.b, 0.01f)
    }

    @Test
    fun hsvGreenAt120Degrees() {
        val c = ColorUtils.hsvToRgb(120f, 1f, 1f)
        assertEquals(0f, c.r, 0.01f)
        assertEquals(1f, c.g, 0.01f)
        assertEquals(0f, c.b, 0.01f)
    }

    @Test
    fun hsvBlueAt240Degrees() {
        val c = ColorUtils.hsvToRgb(240f, 1f, 1f)
        assertEquals(0f, c.r, 0.01f)
        assertEquals(0f, c.g, 0.01f)
        assertEquals(1f, c.b, 0.01f)
    }

    @Test
    fun hsvWhiteAtZeroSaturation() {
        val c = ColorUtils.hsvToRgb(0f, 0f, 1f)
        assertEquals(1f, c.r, 0.01f)
        assertEquals(1f, c.g, 0.01f)
        assertEquals(1f, c.b, 0.01f)
    }

    @Test
    fun hsvBlackAtZeroValue() {
        val c = ColorUtils.hsvToRgb(0f, 1f, 0f)
        assertEquals(0f, c.r, 0.01f)
        assertEquals(0f, c.g, 0.01f)
        assertEquals(0f, c.b, 0.01f)
    }

    @Test
    fun hsvWrapsNegativeHue() {
        val c = ColorUtils.hsvToRgb(-120f, 1f, 1f)
        // -120 + 360 = 240 = blue
        assertEquals(0f, c.r, 0.01f)
        assertEquals(0f, c.g, 0.01f)
        assertEquals(1f, c.b, 0.01f)
    }

    @Test
    fun hsvWrapsHueOver360() {
        val c = ColorUtils.hsvToRgb(480f, 1f, 1f)
        // 480 % 360 = 120 = green
        assertEquals(0f, c.r, 0.01f)
        assertEquals(1f, c.g, 0.01f)
        assertEquals(0f, c.b, 0.01f)
    }

    /* ------------------------------------------------------------------ */
    /*  samplePalette                                                      */
    /* ------------------------------------------------------------------ */

    @Test
    fun samplePaletteEmptyReturnsBlack() {
        val c = ColorUtils.samplePalette(emptyList(), 0.5f)
        assertEquals(Color.BLACK, c)
    }

    @Test
    fun samplePaletteSingleColorAlwaysReturns() {
        val c = ColorUtils.samplePalette(listOf(Color.RED), 0.5f)
        assertEquals(Color.RED, c)
    }

    @Test
    fun samplePaletteStartAndEnd() {
        val palette = listOf(Color.RED, Color.BLUE)
        val start = ColorUtils.samplePalette(palette, 0f)
        val end = ColorUtils.samplePalette(palette, 1f)
        assertEquals(Color.RED, start)
        assertEquals(Color.BLUE, end)
    }

    @Test
    fun samplePaletteMidpoint() {
        val palette = listOf(Color.RED, Color.BLUE)
        val mid = ColorUtils.samplePalette(palette, 0.5f)
        assertEquals(0.5f, mid.r, 0.01f)
        assertEquals(0f, mid.g, 0.01f)
        assertEquals(0.5f, mid.b, 0.01f)
    }

    @Test
    fun samplePaletteClampsOutOfRange() {
        val palette = listOf(Color.RED, Color.BLUE)
        val under = ColorUtils.samplePalette(palette, -1f)
        val over = ColorUtils.samplePalette(palette, 2f)
        assertEquals(Color.RED, under)
        assertEquals(Color.BLUE, over)
    }
}

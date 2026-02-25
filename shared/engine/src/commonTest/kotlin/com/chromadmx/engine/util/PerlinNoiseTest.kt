package com.chromadmx.engine.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerlinNoiseTest {

    @Test
    fun noiseIsDeterministic() {
        val a = PerlinNoise.noise(1.5f, 2.3f, 0.7f)
        val b = PerlinNoise.noise(1.5f, 2.3f, 0.7f)
        assertEquals(a, b)
    }

    @Test
    fun noiseAtIntegerPointsIsZero() {
        // Perlin noise at integer coordinates should be 0 (gradient dot product with zero offset)
        val v = PerlinNoise.noise(1f, 2f, 3f)
        assertEquals(0f, v, 0.001f)
    }

    @Test
    fun noiseValuesInExpectedRange() {
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        for (i in 0 until 1000) {
            val x = (i * 0.137f) % 10f
            val y = (i * 0.251f) % 10f
            val z = (i * 0.419f) % 10f
            val v = PerlinNoise.noise(x, y, z)
            if (v < min) min = v
            if (v > max) max = v
        }
        // Perlin 3D noise should stay within approximately -1..1
        assertTrue(min >= -1.1f, "Min $min out of range")
        assertTrue(max <= 1.1f, "Max $max out of range")
    }

    @Test
    fun noise01InZeroToOneRange() {
        for (i in 0 until 500) {
            val x = (i * 0.137f) % 10f
            val y = (i * 0.251f) % 10f
            val z = (i * 0.419f) % 10f
            val v = PerlinNoise.noise01(x, y, z)
            assertTrue(v >= -0.05f, "noise01 value $v below 0 at ($x,$y,$z)")
            assertTrue(v <= 1.05f, "noise01 value $v above 1 at ($x,$y,$z)")
        }
    }

    @Test
    fun noiseVariesSpatially() {
        val v1 = PerlinNoise.noise(0.5f, 0.5f, 0.5f)
        val v2 = PerlinNoise.noise(5.5f, 5.5f, 5.5f)
        // Very unlikely to be exactly the same
        assertTrue(v1 != v2 || v1 == 0f, "Noise should vary spatially")
    }
}

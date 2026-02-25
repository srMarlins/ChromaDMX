package com.chromadmx.core

import com.chromadmx.core.model.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EffectParamsTest {

    @Test
    fun getFloatReturnsStoredValue() {
        val p = EffectParams(mapOf("speed" to 2.5f))
        assertEquals(2.5f, p.getFloat("speed"))
    }

    @Test
    fun getFloatReturnsDefaultWhenMissing() {
        assertEquals(0f, EffectParams.EMPTY.getFloat("speed"))
        assertEquals(1f, EffectParams.EMPTY.getFloat("speed", 1f))
    }

    @Test
    fun getFloatConvertsFromInt() {
        val p = EffectParams(mapOf("speed" to 3))
        assertEquals(3f, p.getFloat("speed"))
    }

    @Test
    fun getIntReturnsStoredValue() {
        val p = EffectParams(mapOf("count" to 5))
        assertEquals(5, p.getInt("count"))
    }

    @Test
    fun getIntConvertsFromFloat() {
        val p = EffectParams(mapOf("count" to 5.9f))
        assertEquals(5, p.getInt("count"))
    }

    @Test
    fun getStringReturnsStoredValue() {
        val p = EffectParams(mapOf("name" to "rainbow"))
        assertEquals("rainbow", p.getString("name"))
    }

    @Test
    fun getStringReturnsDefaultWhenMissing() {
        assertEquals("", EffectParams.EMPTY.getString("name"))
    }

    @Test
    fun getBooleanReturnsStoredValue() {
        val p = EffectParams(mapOf("enabled" to true))
        assertTrue(p.getBoolean("enabled"))
    }

    @Test
    fun getBooleanReturnsDefaultWhenMissing() {
        assertFalse(EffectParams.EMPTY.getBoolean("enabled"))
    }

    @Test
    fun getColorReturnsStoredValue() {
        val p = EffectParams(mapOf("primary" to Color.RED))
        assertEquals(Color.RED, p.getColor("primary"))
    }

    @Test
    fun getColorReturnsDefaultWhenMissing() {
        assertEquals(Color.WHITE, EffectParams.EMPTY.getColor("primary"))
    }

    @Test
    fun getColorListReturnsStoredList() {
        val palette = listOf(Color.RED, Color.GREEN, Color.BLUE)
        val p = EffectParams(mapOf("palette" to palette))
        assertEquals(palette, p.getColorList("palette"))
    }

    @Test
    fun containsDetectsPresence() {
        val p = EffectParams(mapOf("a" to 1))
        assertTrue(p.contains("a"))
        assertFalse(p.contains("b"))
    }

    @Test
    fun withAddsEntryImmutably() {
        val p1 = EffectParams.EMPTY
        val p2 = p1.with("speed", 5f)
        assertFalse(p1.contains("speed"))
        assertEquals(5f, p2.getFloat("speed"))
    }

    @Test
    fun mergesCombineEntries() {
        val a = EffectParams(mapOf("x" to 1f))
        val b = EffectParams(mapOf("y" to 2f))
        val merged = a.merge(b)
        assertEquals(1f, merged.getFloat("x"))
        assertEquals(2f, merged.getFloat("y"))
    }

    @Test
    fun toMapReturnsContents() {
        val map = mapOf("a" to 1, "b" to "two")
        val p = EffectParams(map)
        assertEquals(map, p.toMap())
    }

    @Test
    fun equalityAndHashCode() {
        val a = EffectParams(mapOf("x" to 1f))
        val b = EffectParams(mapOf("x" to 1f))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun emptyConstant() {
        assertTrue(EffectParams.EMPTY.toMap().isEmpty())
    }
}

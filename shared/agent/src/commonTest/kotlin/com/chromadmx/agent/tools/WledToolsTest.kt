package com.chromadmx.agent.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WledToolsTest {

    @Test
    fun parseHexColor() {
        val result = parseColor("#FF0000")
        assertNotNull(result)
        assertEquals(Triple(255, 0, 0), result)
    }

    @Test
    fun parseNamedColor() {
        val result = parseColor("warm white")
        assertNotNull(result)
        assertEquals(Triple(255, 180, 100), result)
    }

    @Test
    fun parseInvalidColorReturnsNull() {
        assertNull(parseColor("not-a-color"))
        assertNull(parseColor("#ZZZZZZ"))
        assertNull(parseColor("#FF"))
    }

    @Test
    fun parseColorCaseInsensitive() {
        val upper = parseColor("RED")
        val lower = parseColor("red")
        val mixed = parseColor("Warm White")
        assertNotNull(upper)
        assertNotNull(lower)
        assertNotNull(mixed)
        assertEquals(upper, lower)
        assertEquals(Triple(255, 180, 100), mixed)
    }
}

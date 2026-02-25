package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BlendModeTest {

    @Test
    fun enumValuesExist() {
        val modes = BlendMode.entries
        assertEquals(4, modes.size)
        assertEquals(BlendMode.NORMAL, modes[0])
        assertEquals(BlendMode.ADDITIVE, modes[1])
        assertEquals(BlendMode.MULTIPLY, modes[2])
        assertEquals(BlendMode.OVERLAY, modes[3])
    }

    @Test
    fun valueOfRoundTrips() {
        for (mode in BlendMode.entries) {
            assertEquals(mode, BlendMode.valueOf(mode.name))
        }
    }
}

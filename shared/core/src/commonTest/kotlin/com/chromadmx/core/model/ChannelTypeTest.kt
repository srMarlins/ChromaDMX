package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChannelTypeTest {
    @Test
    fun allExpectedChannelTypesExist() {
        val expected = setOf(
            "DIMMER", "RED", "GREEN", "BLUE", "WHITE", "AMBER", "UV",
            "PAN", "TILT", "PAN_FINE", "TILT_FINE",
            "GOBO", "COLOR_WHEEL", "FOCUS", "ZOOM", "PRISM",
            "STROBE", "SHUTTER",
            "GENERIC"
        )
        val actual = ChannelType.entries.map { it.name }.toSet()
        for (name in expected) {
            assertTrue(name in actual, "Missing ChannelType: $name")
        }
    }

    @Test
    fun channelTypeIsColor() {
        assertTrue(ChannelType.RED.isColor)
        assertTrue(ChannelType.GREEN.isColor)
        assertTrue(ChannelType.BLUE.isColor)
        assertTrue(ChannelType.WHITE.isColor)
        assertTrue(ChannelType.AMBER.isColor)
        assertTrue(ChannelType.UV.isColor)
        assertTrue(!ChannelType.PAN.isColor)
        assertTrue(!ChannelType.DIMMER.isColor)
    }

    @Test
    fun channelTypeIsMovement() {
        assertTrue(ChannelType.PAN.isMovement)
        assertTrue(ChannelType.TILT.isMovement)
        assertTrue(ChannelType.PAN_FINE.isMovement)
        assertTrue(ChannelType.TILT_FINE.isMovement)
        assertTrue(!ChannelType.RED.isMovement)
    }
}

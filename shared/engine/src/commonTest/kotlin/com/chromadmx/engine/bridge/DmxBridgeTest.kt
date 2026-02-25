package com.chromadmx.engine.bridge

import com.chromadmx.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DmxBridgeTest {
    private val profiles = mapOf(
        "generic-rgb-par" to BuiltInProfiles.GENERIC_RGB_PAR
    )

    @Test
    fun convertsColorsToUniverseBytes() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("f1", "Par 1", channelStart = 0, channelCount = 3, universeId = 0),
                position = Vec3.ZERO
            ),
            Fixture3D(
                fixture = Fixture("f2", "Par 2", channelStart = 3, channelCount = 3, universeId = 0),
                position = Vec3.ZERO
            )
        )
        val colors = arrayOf(Color.RED, Color.GREEN)
        val bridge = DmxBridge(fixtures, profiles)

        val result = bridge.convert(colors)

        assertTrue(result.containsKey(0))
        val data = result[0]!!
        // Par 1 at channels 0-2: RED = (255, 0, 0)
        assertEquals(255, data[0].toInt() and 0xFF)
        assertEquals(0, data[1].toInt() and 0xFF)
        assertEquals(0, data[2].toInt() and 0xFF)
        // Par 2 at channels 3-5: GREEN = (0, 255, 0)
        assertEquals(0, data[3].toInt() and 0xFF)
        assertEquals(255, data[4].toInt() and 0xFF)
        assertEquals(0, data[5].toInt() and 0xFF)
    }

    @Test
    fun multipleUniverses() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("f1", "Par 1", channelStart = 0, channelCount = 3, universeId = 0),
                position = Vec3.ZERO
            ),
            Fixture3D(
                fixture = Fixture("f2", "Par 2", channelStart = 0, channelCount = 3, universeId = 1),
                position = Vec3.ZERO
            )
        )
        val colors = arrayOf(Color.RED, Color.BLUE)
        val bridge = DmxBridge(fixtures, profiles)

        val result = bridge.convert(colors)

        assertEquals(2, result.size)
        // Universe 0: RED
        assertEquals(255, result[0]!![0].toInt() and 0xFF)
        // Universe 1: BLUE
        assertEquals(255, result[1]!![2].toInt() and 0xFF)
    }

    @Test
    fun profileWithDimmerChannel() {
        val washProfiles = mapOf(
            "generic-wash" to BuiltInProfiles.GENERIC_WASH
        )
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("w1", "Wash 1", channelStart = 0, channelCount = 5,
                    universeId = 0, profileId = "generic-wash"),
                position = Vec3.ZERO
            )
        )
        val colors = arrayOf(Color(0.5f, 0.5f, 0.5f))
        val bridge = DmxBridge(fixtures, washProfiles)

        val result = bridge.convert(colors)
        val data = result[0]!!
        // Dimmer at offset 0 should be 128 (brightness = max of 0.5, 0.5, 0.5 = 0.5 -> 128)
        assertEquals(128, data[0].toInt() and 0xFF)
        // RGB at offsets 1-3
        assertEquals(128, data[1].toInt() and 0xFF, "Red should be ~128")
    }

    @Test
    fun emptyFixturesProducesEmptyResult() {
        val bridge = DmxBridge(emptyList(), profiles)
        val result = bridge.convert(emptyArray())
        assertTrue(result.isEmpty())
    }
}

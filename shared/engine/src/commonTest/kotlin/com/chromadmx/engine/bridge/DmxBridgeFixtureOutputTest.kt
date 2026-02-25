package com.chromadmx.engine.bridge

import com.chromadmx.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [DmxBridge.convertOutputs] — FixtureOutput to DMX channel mapping.
 */
class DmxBridgeFixtureOutputTest {

    /* ------------------------------------------------------------------ */
    /*  Moving head: full FixtureOutput → DMX mapping                     */
    /* ------------------------------------------------------------------ */

    @Test
    fun movingHeadMapsAllChannels() {
        val profile = BuiltInProfiles.GENERIC_MOVING_HEAD
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "mh1",
                    name = "Moving Head 1",
                    channelStart = 0,
                    channelCount = 10,
                    universeId = 0,
                    profileId = "generic-moving-head"
                ),
                position = Vec3.ZERO
            )
        )
        val bridge = DmxBridge(fixtures, mapOf("generic-moving-head" to profile))

        val output = FixtureOutput(
            color = Color(1.0f, 0.5f, 0.0f), // orange
            pan = 0.5f,
            tilt = 0.75f,
            gobo = 3,
            strobeRate = 0.0f
        )

        val result = bridge.convertOutputs(arrayOf(output))
        val data = result[0]!!

        // Channel layout: Pan(0), PanFine(1), Tilt(2), TiltFine(3),
        //                 Dimmer(4), R(5), G(6), B(7), Gobo(8), Strobe(9)

        // Pan coarse: 0.5 * 255 = 128
        assertEquals(128, data[0].toInt() and 0xFF)

        // Pan fine: lower 8 bits of 0.5 * 65535 = 32768 → 32768 & 0xFF = 0
        assertEquals(0, data[1].toInt() and 0xFF)

        // Tilt coarse: 0.75 * 255 = 191
        assertEquals(191, data[2].toInt() and 0xFF)

        // Tilt fine: lower 8 bits of 0.75 * 65535 = 49151 → 49151 & 0xFF = 255
        assertEquals(255, data[3].toInt() and 0xFF)

        // Dimmer: brightness = max(1.0, 0.5, 0.0) = 1.0 → 255
        assertEquals(255, data[4].toInt() and 0xFF)

        // Red: normalized = 1.0/1.0 = 1.0 → 255
        assertEquals(255, data[5].toInt() and 0xFF)

        // Green: normalized = 0.5/1.0 = 0.5 → 128
        assertEquals(128, data[6].toInt() and 0xFF)

        // Blue: normalized = 0.0/1.0 = 0.0 → 0
        assertEquals(0, data[7].toInt() and 0xFF)

        // Gobo: slot 3
        assertEquals(3, data[8].toInt() and 0xFF)

        // Strobe: 0.0 → 0
        assertEquals(0, data[9].toInt() and 0xFF)
    }

    @Test
    fun movingHeadNullFieldsUseDefaults() {
        val profile = BuiltInProfiles.GENERIC_MOVING_HEAD
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "mh1",
                    name = "Moving Head 1",
                    channelStart = 0,
                    channelCount = 10,
                    universeId = 0,
                    profileId = "generic-moving-head"
                ),
                position = Vec3.ZERO
            )
        )
        val bridge = DmxBridge(fixtures, mapOf("generic-moving-head" to profile))

        // Only color, all movement fields null
        val output = FixtureOutput(color = Color.WHITE)

        val result = bridge.convertOutputs(arrayOf(output))
        val data = result[0]!!

        // Pan: null → use default (0 / 255f = 0.0)
        assertEquals(0, data[0].toInt() and 0xFF)

        // Tilt: null → use default (0 / 255f = 0.0)
        assertEquals(0, data[2].toInt() and 0xFF)

        // Gobo: null → use default (0)
        assertEquals(0, data[8].toInt() and 0xFF)

        // Strobe: null → use default (0 / 255f = 0.0)
        assertEquals(0, data[9].toInt() and 0xFF)
    }

    /* ------------------------------------------------------------------ */
    /*  RGB-only fixture ignores movement channels                         */
    /* ------------------------------------------------------------------ */

    @Test
    fun rgbParIgnoresMovementFields() {
        val profile = BuiltInProfiles.GENERIC_RGB_PAR
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "par1",
                    name = "Par 1",
                    channelStart = 0,
                    channelCount = 3,
                    universeId = 0,
                    profileId = "generic-rgb-par"
                ),
                position = Vec3.ZERO
            )
        )
        val bridge = DmxBridge(fixtures, mapOf("generic-rgb-par" to profile))

        // Movement fields set but should be ignored for RGB par
        val output = FixtureOutput(
            color = Color.RED,
            pan = 0.5f,
            tilt = 0.75f,
            gobo = 3
        )

        val result = bridge.convertOutputs(arrayOf(output))
        val data = result[0]!!

        // Only RGB channels should be written
        assertEquals(255, data[0].toInt() and 0xFF) // Red
        assertEquals(0, data[1].toInt() and 0xFF)   // Green
        assertEquals(0, data[2].toInt() and 0xFF)   // Blue
    }

    /* ------------------------------------------------------------------ */
    /*  Wash fixture with zoom channel                                     */
    /* ------------------------------------------------------------------ */

    @Test
    fun washFixtureMapsFocusAndZoom() {
        val profile = BuiltInProfiles.GENERIC_WASH
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "w1",
                    name = "Wash 1",
                    channelStart = 0,
                    channelCount = 5,
                    universeId = 0,
                    profileId = "generic-wash"
                ),
                position = Vec3.ZERO
            )
        )
        val bridge = DmxBridge(fixtures, mapOf("generic-wash" to profile))

        val output = FixtureOutput(
            color = Color.WHITE,
            zoom = 0.5f
        )

        val result = bridge.convertOutputs(arrayOf(output))
        val data = result[0]!!

        // Wash layout: Dimmer(0), R(1), G(2), B(3), Zoom(4)
        // Zoom: 0.5 → 128
        assertEquals(128, data[4].toInt() and 0xFF)
    }

    /* ------------------------------------------------------------------ */
    /*  Fallback for unknown profile                                       */
    /* ------------------------------------------------------------------ */

    @Test
    fun unknownProfileFallsBackToSimpleRgb() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "f1",
                    name = "Unknown",
                    channelStart = 0,
                    channelCount = 3,
                    universeId = 0,
                    profileId = "unknown-profile-xyz"
                ),
                position = Vec3.ZERO
            )
        )
        val bridge = DmxBridge(fixtures, emptyMap())

        val output = FixtureOutput(
            color = Color.GREEN,
            pan = 0.5f
        )

        val result = bridge.convertOutputs(arrayOf(output))
        val data = result[0]!!

        // Fallback: simple 3-channel RGB
        assertEquals(0, data[0].toInt() and 0xFF)   // Red
        assertEquals(255, data[1].toInt() and 0xFF)  // Green
        assertEquals(0, data[2].toInt() and 0xFF)    // Blue
    }

    /* ------------------------------------------------------------------ */
    /*  Empty fixtures                                                      */
    /* ------------------------------------------------------------------ */

    @Test
    fun emptyFixturesProducesEmptyResult() {
        val bridge = DmxBridge(emptyList(), emptyMap())
        val result = bridge.convertOutputs(emptyArray())
        assertTrue(result.isEmpty())
    }

    /* ------------------------------------------------------------------ */
    /*  Multiple universes                                                  */
    /* ------------------------------------------------------------------ */

    @Test
    fun multipleUniversesWithFixtureOutput() {
        val profile = BuiltInProfiles.GENERIC_RGB_PAR
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("f1", "Par 1", channelStart = 0, channelCount = 3,
                    universeId = 0, profileId = "generic-rgb-par"),
                position = Vec3.ZERO
            ),
            Fixture3D(
                fixture = Fixture("f2", "Par 2", channelStart = 0, channelCount = 3,
                    universeId = 1, profileId = "generic-rgb-par"),
                position = Vec3.ZERO
            )
        )
        val bridge = DmxBridge(fixtures, mapOf("generic-rgb-par" to profile))

        val outputs = arrayOf(
            FixtureOutput(color = Color.RED),
            FixtureOutput(color = Color.BLUE)
        )

        val result = bridge.convertOutputs(outputs)
        assertEquals(2, result.size)

        // Universe 0: RED
        assertEquals(255, result[0]!![0].toInt() and 0xFF)
        assertEquals(0, result[0]!![1].toInt() and 0xFF)

        // Universe 1: BLUE
        assertEquals(0, result[1]!![0].toInt() and 0xFF)
        assertEquals(0, result[1]!![1].toInt() and 0xFF)
        assertEquals(255, result[1]!![2].toInt() and 0xFF)
    }

    /* ------------------------------------------------------------------ */
    /*  Pan/tilt fine channels (16-bit resolution)                          */
    /* ------------------------------------------------------------------ */

    @Test
    fun panTiltFineChannelsProvide16BitResolution() {
        val profile = BuiltInProfiles.GENERIC_MOVING_HEAD
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "mh1",
                    name = "Moving Head 1",
                    channelStart = 0,
                    channelCount = 10,
                    universeId = 0,
                    profileId = "generic-moving-head"
                ),
                position = Vec3.ZERO
            )
        )
        val bridge = DmxBridge(fixtures, mapOf("generic-moving-head" to profile))

        // Pan = 1.0 → coarse = 255, fine = 65535 & 0xFF = 255
        val output = FixtureOutput(color = Color.BLACK, pan = 1.0f, tilt = 0.0f)
        val result = bridge.convertOutputs(arrayOf(output))
        val data = result[0]!!

        // Pan coarse at offset 0: 255
        assertEquals(255, data[0].toInt() and 0xFF)
        // Pan fine at offset 1: 65535 & 0xFF = 255
        assertEquals(255, data[1].toInt() and 0xFF)

        // Tilt coarse at offset 2: 0
        assertEquals(0, data[2].toInt() and 0xFF)
        // Tilt fine at offset 3: 0
        assertEquals(0, data[3].toInt() and 0xFF)
    }
}

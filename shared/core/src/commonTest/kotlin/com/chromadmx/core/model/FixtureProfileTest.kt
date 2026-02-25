package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FixtureProfileTest {
    @Test
    fun channelDataClass() {
        val ch = Channel(name = "Red", type = ChannelType.RED, offset = 0)
        assertEquals("Red", ch.name)
        assertEquals(ChannelType.RED, ch.type)
        assertEquals(0, ch.offset)
        assertEquals(0, ch.defaultValue)
    }

    @Test
    fun fixtureProfileWithChannels() {
        val profile = FixtureProfile(
            profileId = "generic-rgb-par",
            name = "Generic RGB Par",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("Red", ChannelType.RED, 0),
                Channel("Green", ChannelType.GREEN, 1),
                Channel("Blue", ChannelType.BLUE, 2),
            )
        )
        assertEquals(3, profile.channelCount)
        assertEquals("generic-rgb-par", profile.profileId)
    }

    @Test
    fun findChannelByType() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("Dimmer", ChannelType.DIMMER, 0),
                Channel("Red", ChannelType.RED, 1),
                Channel("Green", ChannelType.GREEN, 2),
                Channel("Blue", ChannelType.BLUE, 3),
            )
        )
        val red = profile.channelByType(ChannelType.RED)
        assertNotNull(red)
        assertEquals(1, red.offset)
        assertNull(profile.channelByType(ChannelType.PAN))
    }

    @Test
    fun colorMixingCapability() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("R", ChannelType.RED, 0),
                Channel("G", ChannelType.GREEN, 1),
                Channel("B", ChannelType.BLUE, 2),
            ),
            capabilities = Capabilities(colorMixing = ColorMixing.RGB)
        )
        assertEquals(ColorMixing.RGB, profile.capabilities.colorMixing)
    }

    @Test
    fun physicalProperties() {
        val profile = FixtureProfile(
            profileId = "test-mh",
            name = "Test Moving Head",
            type = FixtureType.MOVING_HEAD,
            channels = emptyList(),
            physical = PhysicalProperties(
                beamAngle = 15f,
                panRange = 540f,
                tiltRange = 270f
            )
        )
        assertEquals(540f, profile.physical.panRange)
    }

    @Test
    fun renderingHint() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PIXEL_BAR,
            channels = emptyList(),
            renderHint = RenderHint.BAR
        )
        assertEquals(RenderHint.BAR, profile.renderHint)
    }

    @Test
    fun channelCountDerivedFromChannelsList() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("R", ChannelType.RED, 0),
                Channel("G", ChannelType.GREEN, 1),
                Channel("B", ChannelType.BLUE, 2),
            )
        )
        assertEquals(3, profile.channelCount)
    }
}

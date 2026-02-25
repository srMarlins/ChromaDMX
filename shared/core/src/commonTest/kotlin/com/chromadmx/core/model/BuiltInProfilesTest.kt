package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuiltInProfilesTest {
    @Test
    fun allSixProfilesExist() {
        assertEquals(6, BuiltInProfiles.all().size)
    }

    @Test
    fun genericRgbParIs3Channel() {
        val par = BuiltInProfiles.GENERIC_RGB_PAR
        assertEquals(FixtureType.PAR, par.type)
        assertEquals(3, par.channelCount)
        assertTrue(par.hasRgb)
        assertEquals(RenderHint.POINT, par.renderHint)
    }

    @Test
    fun movingHeadHasMovementChannels() {
        val mh = BuiltInProfiles.GENERIC_MOVING_HEAD
        assertEquals(FixtureType.MOVING_HEAD, mh.type)
        assertTrue(mh.capabilities.hasMovement)
        assertTrue(mh.channelByType(ChannelType.PAN) != null)
        assertTrue(mh.channelByType(ChannelType.TILT) != null)
        assertEquals(RenderHint.BEAM_CONE, mh.renderHint)
        assertEquals(540f, mh.physical.panRange)
        assertEquals(270f, mh.physical.tiltRange)
    }

    @Test
    fun pixelBar8Has24Channels() {
        val bar = BuiltInProfiles.PIXEL_BAR_8
        assertEquals(FixtureType.PIXEL_BAR, bar.type)
        assertEquals(24, bar.channelCount) // 8 pixels * 3 channels
        assertEquals(8, bar.physical.pixelCount)
        assertEquals(RenderHint.BAR, bar.renderHint)
    }

    @Test
    fun strobeProfile() {
        val strobe = BuiltInProfiles.GENERIC_STROBE
        assertEquals(FixtureType.STROBE, strobe.type)
        assertTrue(strobe.channelByType(ChannelType.STROBE) != null ||
                   strobe.channelByType(ChannelType.DIMMER) != null)
    }

    @Test
    fun lookupByIdWorks() {
        val found = BuiltInProfiles.findById("generic-rgb-par")
        assertEquals(BuiltInProfiles.GENERIC_RGB_PAR, found)
    }

    @Test
    fun lookupByIdReturnsNullForUnknown() {
        val found = BuiltInProfiles.findById("nonexistent")
        assertEquals(null, found)
    }
}

package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FixtureTest {
    @Test
    fun fixtureHasProfileId() {
        val fixture = Fixture(
            fixtureId = "par-1",
            name = "Par 1",
            channelStart = 0,
            channelCount = 3,
            universeId = 0,
            profileId = "generic-rgb-par"
        )
        assertEquals("generic-rgb-par", fixture.profileId)
    }

    @Test
    fun fixtureDefaultsToRgbParProfile() {
        val fixture = Fixture(
            fixtureId = "par-1",
            name = "Par 1",
            channelStart = 0,
            channelCount = 3,
            universeId = 0
        )
        assertEquals("generic-rgb-par", fixture.profileId)
    }
}

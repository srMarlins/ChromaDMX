package com.chromadmx.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileSerializationTest {
    private val json = Json { prettyPrint = false }

    @Test
    fun roundTripBuiltInProfile() {
        val original = BuiltInProfiles.GENERIC_RGB_PAR
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<FixtureProfile>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTripMovingHeadProfile() {
        val original = BuiltInProfiles.GENERIC_MOVING_HEAD
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<FixtureProfile>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTripCustomProfile() {
        val custom = FixtureProfile(
            profileId = "custom-rgbw",
            name = "Custom RGBW Fixture",
            type = FixtureType.OTHER,
            channels = listOf(
                Channel("Red", ChannelType.RED, 0),
                Channel("Green", ChannelType.GREEN, 1),
                Channel("Blue", ChannelType.BLUE, 2),
                Channel("White", ChannelType.WHITE, 3),
            ),
            capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        )
        val encoded = json.encodeToString(custom)
        val decoded = json.decodeFromString<FixtureProfile>(encoded)
        assertEquals(custom, decoded)
    }
}

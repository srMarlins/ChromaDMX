package com.chromadmx.core.model

/**
 * Factory for built-in fixture profiles covering common fixture types.
 *
 * These profiles serve as defaults until user-imported profiles are available.
 * Each profile defines the complete channel map, capabilities, physical
 * properties, and rendering hint for a fixture model.
 */
object BuiltInProfiles {
    val GENERIC_RGB_PAR = FixtureProfile(
        profileId = "generic-rgb-par",
        name = "Generic RGB Par",
        type = FixtureType.PAR,
        channels = listOf(
            Channel("Red", ChannelType.RED, 0),
            Channel("Green", ChannelType.GREEN, 1),
            Channel("Blue", ChannelType.BLUE, 2),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        renderHint = RenderHint.POINT
    )

    val GENERIC_MOVING_HEAD = FixtureProfile(
        profileId = "generic-moving-head",
        name = "Generic Moving Head",
        type = FixtureType.MOVING_HEAD,
        channels = listOf(
            Channel("Pan", ChannelType.PAN, 0),
            Channel("Pan Fine", ChannelType.PAN_FINE, 1),
            Channel("Tilt", ChannelType.TILT, 2),
            Channel("Tilt Fine", ChannelType.TILT_FINE, 3),
            Channel("Dimmer", ChannelType.DIMMER, 4),
            Channel("Red", ChannelType.RED, 5),
            Channel("Green", ChannelType.GREEN, 6),
            Channel("Blue", ChannelType.BLUE, 7),
            Channel("Gobo", ChannelType.GOBO, 8),
            Channel("Strobe", ChannelType.STROBE, 9),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.RGB, hasMovement = true, goboSlots = 8),
        physical = PhysicalProperties(beamAngle = 15f, panRange = 540f, tiltRange = 270f),
        renderHint = RenderHint.BEAM_CONE
    )

    val PIXEL_BAR_8 = FixtureProfile(
        profileId = "pixel-bar-8",
        name = "Pixel Bar 8-Segment",
        type = FixtureType.PIXEL_BAR,
        channels = (0 until 8).flatMap { pixel ->
            listOf(
                Channel("Pixel${pixel}_R", ChannelType.RED, pixel * 3),
                Channel("Pixel${pixel}_G", ChannelType.GREEN, pixel * 3 + 1),
                Channel("Pixel${pixel}_B", ChannelType.BLUE, pixel * 3 + 2),
            )
        },
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        physical = PhysicalProperties(pixelCount = 8),
        renderHint = RenderHint.BAR
    )

    val PIXEL_BAR_16 = FixtureProfile(
        profileId = "pixel-bar-16",
        name = "Pixel Bar 16-Segment",
        type = FixtureType.PIXEL_BAR,
        channels = (0 until 16).flatMap { pixel ->
            listOf(
                Channel("Pixel${pixel}_R", ChannelType.RED, pixel * 3),
                Channel("Pixel${pixel}_G", ChannelType.GREEN, pixel * 3 + 1),
                Channel("Pixel${pixel}_B", ChannelType.BLUE, pixel * 3 + 2),
            )
        },
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        physical = PhysicalProperties(pixelCount = 16),
        renderHint = RenderHint.BAR
    )

    val GENERIC_STROBE = FixtureProfile(
        profileId = "generic-strobe",
        name = "Generic Strobe",
        type = FixtureType.STROBE,
        channels = listOf(
            Channel("Dimmer", ChannelType.DIMMER, 0),
            Channel("Strobe", ChannelType.STROBE, 1),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.SINGLE),
        renderHint = RenderHint.POINT
    )

    val GENERIC_WASH = FixtureProfile(
        profileId = "generic-wash",
        name = "Generic Wash",
        type = FixtureType.WASH,
        channels = listOf(
            Channel("Dimmer", ChannelType.DIMMER, 0),
            Channel("Red", ChannelType.RED, 1),
            Channel("Green", ChannelType.GREEN, 2),
            Channel("Blue", ChannelType.BLUE, 3),
            Channel("Zoom", ChannelType.ZOOM, 4),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        physical = PhysicalProperties(beamAngle = 60f),
        renderHint = RenderHint.POINT
    )

    private val allProfiles = listOf(
        GENERIC_RGB_PAR, GENERIC_MOVING_HEAD,
        PIXEL_BAR_8, PIXEL_BAR_16,
        GENERIC_STROBE, GENERIC_WASH
    )

    fun all(): List<FixtureProfile> = allProfiles

    fun findById(profileId: String): FixtureProfile? = allProfiles.find { it.profileId == profileId }
}

package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/** A single DMX channel within a fixture profile. */
@Serializable
data class Channel(
    val name: String,
    val type: ChannelType,
    val offset: Int,
    val defaultValue: Int = 0
)

/** How the fixture mixes colors. */
@Serializable
enum class ColorMixing { RGB, CMY, COLOR_WHEEL, SINGLE }

/** Fixture capabilities. */
@Serializable
data class Capabilities(
    val colorMixing: ColorMixing = ColorMixing.RGB,
    val hasMovement: Boolean = false,
    val goboSlots: Int = 0
)

/** Physical properties of the fixture. */
@Serializable
data class PhysicalProperties(
    val beamAngle: Float = 25f,
    val panRange: Float = 0f,
    val tiltRange: Float = 0f,
    val pixelCount: Int = 1
)

/** How to render this fixture in the stage preview. */
@Serializable
enum class RenderHint { POINT, BAR, BEAM_CONE }

/**
 * Describes the type and channel layout of a fixture model.
 *
 * The [channels] list defines every DMX channel the fixture uses,
 * with typed offsets relative to the fixture's start address.
 */
@Serializable
data class FixtureProfile(
    val profileId: String,
    val name: String,
    val type: FixtureType,
    val channels: List<Channel>,
    val capabilities: Capabilities = Capabilities(),
    val physical: PhysicalProperties = PhysicalProperties(),
    val renderHint: RenderHint = RenderHint.POINT
) {
    /** Total number of DMX channels this fixture occupies. */
    val channelCount: Int get() = (channels.maxOfOrNull { it.offset } ?: -1) + 1

    /** Find the first channel of a given type, or null. */
    fun channelByType(type: ChannelType): Channel? = channels.firstOrNull { it.type == type }

    /** Find all channels of a given type. */
    fun channelsByType(type: ChannelType): List<Channel> = channels.filter { it.type == type }

    /** Whether this profile has RGB color channels. */
    val hasRgb: Boolean get() =
        channelByType(ChannelType.RED) != null &&
        channelByType(ChannelType.GREEN) != null &&
        channelByType(ChannelType.BLUE) != null
}

/** Well-known fixture types. */
@Serializable
enum class FixtureType {
    PAR, PIXEL_BAR, MOVING_HEAD, STROBE, WASH, SPOT, LASER, OTHER
}

/**
 * Typed DMX channel classification.
 *
 * Each entry describes the semantic role of a single DMX channel.
 * [isColor] and [isMovement] flags allow quick capability queries.
 */
@Serializable
enum class ChannelType(val isColor: Boolean = false, val isMovement: Boolean = false) {
    DIMMER,
    RED(isColor = true), GREEN(isColor = true), BLUE(isColor = true),
    WHITE(isColor = true), AMBER(isColor = true), UV(isColor = true),
    PAN(isMovement = true), TILT(isMovement = true),
    PAN_FINE(isMovement = true), TILT_FINE(isMovement = true),
    GOBO, COLOR_WHEEL, FOCUS, ZOOM, PRISM,
    STROBE, SHUTTER,
    GENERIC
}

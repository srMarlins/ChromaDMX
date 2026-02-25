package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/**
 * Describes the type and channel layout of a fixture model.
 *
 * [channelLayout] maps logical function names (e.g. "red", "green",
 * "blue", "dimmer", "strobe") to their zero-based offset within the
 * fixture's channel range.
 */
@Serializable
data class FixtureProfile(
    val profileId: String,
    val name: String,
    val type: FixtureType,
    val channelCount: Int,
    val channelLayout: Map<String, Int> = emptyMap()
) {
    /** Convenience: offset of the red channel, or null if not mapped. */
    val redOffset: Int? get() = channelLayout["red"]
    /** Convenience: offset of the green channel, or null if not mapped. */
    val greenOffset: Int? get() = channelLayout["green"]
    /** Convenience: offset of the blue channel, or null if not mapped. */
    val blueOffset: Int? get() = channelLayout["blue"]
}

/**
 * Well-known fixture types.
 */
@Serializable
enum class FixtureType {
    PAR,
    PIXEL_BAR,
    MOVING_HEAD,
    STROBE,
    WASH,
    SPOT,
    LASER,
    OTHER
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

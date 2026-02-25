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
    LASER,
    OTHER
}

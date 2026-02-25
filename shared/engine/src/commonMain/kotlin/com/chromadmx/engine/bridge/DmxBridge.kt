package com.chromadmx.engine.bridge

import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.ChannelType
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureProfile

/**
 * Converts per-fixture RGB colors from the effect engine into
 * per-universe DMX byte arrays for the output service.
 *
 * Uses [FixtureProfile] channel mappings to write color values
 * to the correct DMX addresses. Fixtures without a matching profile
 * fall back to simple 3-channel RGB mapping.
 */
class DmxBridge(
    private val fixtures: List<Fixture3D>,
    private val profiles: Map<String, FixtureProfile> = emptyMap()
) {
    /**
     * Convert an array of per-fixture colors into per-universe DMX data.
     *
     * @param colors One [Color] per fixture, parallel to the [fixtures] list.
     * @return Map of universe ID to 512-byte DMX channel data.
     */
    fun convert(colors: Array<Color>): Map<Int, ByteArray> {
        if (fixtures.isEmpty()) return emptyMap()

        val universes = mutableMapOf<Int, ByteArray>()

        for (i in fixtures.indices) {
            val fixture = fixtures[i].fixture
            val color = colors.getOrElse(i) { Color.BLACK }
            val data = universes.getOrPut(fixture.universeId) { ByteArray(512) }
            val profile = profiles[fixture.profileId]
                ?: BuiltInProfiles.findById(fixture.profileId)

            if (profile != null && profile.hasRgb) {
                writeProfileChannels(data, fixture.channelStart, profile, color)
            } else {
                // Fallback: write as simple 3-channel RGB
                writeSimpleRgb(data, fixture.channelStart, color)
            }
        }

        return universes
    }

    private fun writeProfileChannels(
        data: ByteArray,
        channelStart: Int,
        profile: FixtureProfile,
        color: Color
    ) {
        val clamped = color.clamped()

        for (channel in profile.channels) {
            val addr = channelStart + channel.offset
            if (addr < 0 || addr >= 512) continue

            data[addr] = when (channel.type) {
                ChannelType.RED -> (clamped.r * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                ChannelType.GREEN -> (clamped.g * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                ChannelType.BLUE -> (clamped.b * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                ChannelType.DIMMER -> {
                    // Set dimmer based on the brightest color component
                    val brightness = maxOf(clamped.r, clamped.g, clamped.b)
                    (brightness * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.WHITE -> {
                    // White = minimum of RGB (conservative approach)
                    val white = minOf(clamped.r, clamped.g, clamped.b)
                    (white * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.STROBE -> channel.defaultValue.toByte()
                else -> channel.defaultValue.toByte()
            }
        }
    }

    private fun writeSimpleRgb(data: ByteArray, channelStart: Int, color: Color) {
        val bytes = color.toDmxBytes()
        for (i in bytes.indices) {
            val addr = channelStart + i
            if (addr in 0 until 512) {
                data[addr] = bytes[i]
            }
        }
    }
}

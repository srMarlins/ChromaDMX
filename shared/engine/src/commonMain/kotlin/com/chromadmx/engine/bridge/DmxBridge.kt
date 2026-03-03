package com.chromadmx.engine.bridge

import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.ChannelType
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.FixtureProfile

/**
 * Converts per-fixture RGB colors from the effect engine into
 * per-universe DMX byte arrays for the output service.
 *
 * Uses [FixtureProfile] channel mappings to write color values
 * to the correct DMX addresses. Fixtures without a matching profile
 * fall back to simple 3-channel RGB mapping.
 *
 * Also supports [FixtureOutput] for multi-channel fixtures with
 * pan, tilt, gobo, focus, zoom, and strobe channels.
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

    /**
     * Convert an array of per-fixture [FixtureOutput] into per-universe DMX data.
     *
     * This overload handles multi-channel fixtures with movement, gobo, etc.
     *
     * @param outputs One [FixtureOutput] per fixture, parallel to the [fixtures] list.
     * @return Map of universe ID to 512-byte DMX channel data.
     */
    fun convertOutputs(outputs: Array<FixtureOutput>): Map<Int, ByteArray> {
        if (fixtures.isEmpty()) return emptyMap()

        val universes = mutableMapOf<Int, ByteArray>()

        for (i in fixtures.indices) {
            val fixture = fixtures[i].fixture
            val output = outputs.getOrElse(i) { FixtureOutput.DEFAULT }
            val data = universes.getOrPut(fixture.universeId) { ByteArray(512) }
            val profile = profiles[fixture.profileId]
                ?: BuiltInProfiles.findById(fixture.profileId)

            if (profile != null) {
                writeProfileChannelsWithOutput(data, fixture.channelStart, profile, output)
            } else {
                // Fallback: write color as simple 3-channel RGB
                writeSimpleRgb(data, fixture.channelStart, output.color)
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
        // Optimization: bypass clamped() to prevent allocating a Color object per fixture per frame
        val cr = color.r.coerceIn(0f, 1f)
        val cg = color.g.coerceIn(0f, 1f)
        val cb = color.b.coerceIn(0f, 1f)

        val hasDimmer = profile.channelByType(ChannelType.DIMMER) != null
        val brightness = maxOf(cr, cg, cb)

        for (channel in profile.channels) {
            val addr = channelStart + channel.offset
            if (addr < 0 || addr >= 512) continue

            data[addr] = when (channel.type) {
                ChannelType.RED -> {
                    val value = if (hasDimmer && brightness > 0f) cr / brightness else cr
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.GREEN -> {
                    val value = if (hasDimmer && brightness > 0f) cg / brightness else cg
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.BLUE -> {
                    val value = if (hasDimmer && brightness > 0f) cb / brightness else cb
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.DIMMER -> {
                    (brightness * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.WHITE -> {
                    // White = minimum of RGB (conservative approach)
                    val white = minOf(cr, cg, cb)
                    (white * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.STROBE -> channel.defaultValue.toByte()
                else -> channel.defaultValue.toByte()
            }
        }
    }

    private fun writeProfileChannelsWithOutput(
        data: ByteArray,
        channelStart: Int,
        profile: FixtureProfile,
        output: FixtureOutput
    ) {
        // Optimization: bypass clamped() to prevent allocating a Color object per fixture per frame
        val cr = output.color.r.coerceIn(0f, 1f)
        val cg = output.color.g.coerceIn(0f, 1f)
        val cb = output.color.b.coerceIn(0f, 1f)

        val hasDimmer = profile.channelByType(ChannelType.DIMMER) != null
        val brightness = maxOf(cr, cg, cb)

        for (channel in profile.channels) {
            val addr = channelStart + channel.offset
            if (addr < 0 || addr >= 512) continue

            data[addr] = when (channel.type) {
                ChannelType.RED -> {
                    val value = if (hasDimmer && brightness > 0f) cr / brightness else cr
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.GREEN -> {
                    val value = if (hasDimmer && brightness > 0f) cg / brightness else cg
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.BLUE -> {
                    val value = if (hasDimmer && brightness > 0f) cb / brightness else cb
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.DIMMER -> {
                    (brightness * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.WHITE -> {
                    val white = minOf(cr, cg, cb)
                    (white * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.PAN -> {
                    // Compute full 16-bit value; coarse channel = MSB
                    val coarseDefault = channel.defaultValue / 255f
                    val value = output.pan ?: coarseDefault
                    val full16 = (value * 65535f + 0.5f).toInt().coerceIn(0, 65535)
                    (full16 shr 8 and 0xFF).toByte()
                }
                ChannelType.TILT -> {
                    // Compute full 16-bit value; coarse channel = MSB
                    val coarseDefault = channel.defaultValue / 255f
                    val value = output.tilt ?: coarseDefault
                    val full16 = (value * 65535f + 0.5f).toInt().coerceIn(0, 65535)
                    (full16 shr 8 and 0xFF).toByte()
                }
                ChannelType.PAN_FINE -> {
                    // Fine channel = LSB of 16-bit value; derive default from coarse PAN channel
                    val coarseDefault = profile.channelByType(ChannelType.PAN)?.defaultValue?.let { it / 255f } ?: 0f
                    val value = output.pan ?: coarseDefault
                    val full16 = (value * 65535f + 0.5f).toInt().coerceIn(0, 65535)
                    (full16 and 0xFF).toByte()
                }
                ChannelType.TILT_FINE -> {
                    // Fine channel = LSB of 16-bit value; derive default from coarse TILT channel
                    val coarseDefault = profile.channelByType(ChannelType.TILT)?.defaultValue?.let { it / 255f } ?: 0f
                    val value = output.tilt ?: coarseDefault
                    val full16 = (value * 65535f + 0.5f).toInt().coerceIn(0, 65535)
                    (full16 and 0xFF).toByte()
                }
                ChannelType.GOBO -> {
                    output.gobo?.coerceIn(0, 255)?.toByte() ?: channel.defaultValue.toByte()
                }
                ChannelType.FOCUS -> {
                    val value = output.focus ?: (channel.defaultValue / 255f)
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.ZOOM -> {
                    val value = output.zoom ?: (channel.defaultValue / 255f)
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.STROBE, ChannelType.SHUTTER -> {
                    val value = output.strobeRate ?: (channel.defaultValue / 255f)
                    (value * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                else -> channel.defaultValue.toByte()
            }
        }
    }

    private fun writeSimpleRgb(data: ByteArray, channelStart: Int, color: Color) {
        // Optimization: prevent allocating a ByteArray and Color per fixture per frame by manually coercing
        val cr = color.r.coerceIn(0f, 1f)
        val cg = color.g.coerceIn(0f, 1f)
        val cb = color.b.coerceIn(0f, 1f)

        if (channelStart in 0 until 512) {
            data[channelStart] = (cr * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        }
        if (channelStart + 1 in 0 until 512) {
            data[channelStart + 1] = (cg * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        }
        if (channelStart + 2 in 0 until 512) {
            data[channelStart + 2] = (cb * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        }
    }
}

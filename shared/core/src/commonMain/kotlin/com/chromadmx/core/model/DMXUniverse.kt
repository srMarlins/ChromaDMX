package com.chromadmx.core.model

/**
 * A single DMX-512 universe: 512 channels addressed 0-511.
 *
 * Channel data is mutable so the engine can write directly into it each
 * frame, but the [universeId] is fixed for the lifetime of the object.
 */
data class DMXUniverse(
    val universeId: Int,
    val channels: ByteArray = ByteArray(DMX_CHANNEL_COUNT)
) {
    init {
        require(channels.size == DMX_CHANNEL_COUNT) {
            "DMX universe must have exactly $DMX_CHANNEL_COUNT channels, got ${channels.size}"
        }
    }

    /** Set a single channel (0-511) to a byte value. */
    fun setChannel(channel: Int, value: Byte) {
        require(channel in 0 until DMX_CHANNEL_COUNT) {
            "Channel $channel out of range 0..${ DMX_CHANNEL_COUNT - 1 }"
        }
        channels[channel] = value
    }

    /** Get a single channel value. */
    fun getChannel(channel: Int): Byte {
        require(channel in 0 until DMX_CHANNEL_COUNT) {
            "Channel $channel out of range 0..${ DMX_CHANNEL_COUNT - 1 }"
        }
        return channels[channel]
    }

    /** Zero out every channel. */
    fun clear() {
        channels.fill(0)
    }

    /** Write an RGB [Color] starting at [startChannel] (3 consecutive channels). */
    fun setColor(startChannel: Int, color: Color) {
        require(startChannel in 0..(DMX_CHANNEL_COUNT - 3)) {
            "Cannot write 3-byte color at channel $startChannel"
        }
        val bytes = color.toDmxBytes()
        bytes.copyInto(channels, startChannel)
    }

    /** Read an RGB [Color] from [startChannel] (3 consecutive channels). */
    fun getColor(startChannel: Int): Color {
        require(startChannel in 0..(DMX_CHANNEL_COUNT - 3)) {
            "Cannot read 3-byte color from channel $startChannel"
        }
        return Color.fromDmxBytes(channels, startChannel)
    }

    /* -- equals / hashCode must account for ByteArray -- */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DMXUniverse) return false
        return universeId == other.universeId && channels.contentEquals(other.channels)
    }

    override fun hashCode(): Int = 31 * universeId + channels.contentHashCode()

    companion object {
        /** Standard DMX-512 channel count. */
        const val DMX_CHANNEL_COUNT = 512
    }
}

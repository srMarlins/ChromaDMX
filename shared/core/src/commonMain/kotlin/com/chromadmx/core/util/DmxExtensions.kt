package com.chromadmx.core.util

import com.chromadmx.core.model.Color

/**
 * ByteArray extension functions for DMX channel packing / unpacking.
 */

/**
 * Write an RGB [Color] into this ByteArray at [offset] (3 consecutive bytes).
 *
 * @throws IllegalArgumentException if there isn't room for 3 bytes at [offset].
 */
fun ByteArray.packColor(color: Color, offset: Int) {
    require(offset >= 0 && offset + 3 <= size) {
        "Cannot pack 3-byte color at offset $offset in array of size $size"
    }
    val bytes = color.toDmxBytes()
    bytes.copyInto(this, offset)
}

/**
 * Read an RGB [Color] from 3 consecutive bytes starting at [offset].
 */
fun ByteArray.unpackColor(offset: Int): Color {
    require(offset >= 0 && offset + 3 <= size) {
        "Cannot unpack 3-byte color at offset $offset in array of size $size"
    }
    return Color.fromDmxBytes(this, offset)
}

/**
 * Pack a list of [Color]s into consecutive 3-byte RGB triples starting
 * at [offset].  Useful for multi-pixel fixtures (e.g. pixel bars).
 *
 * @throws IllegalArgumentException if the array is too small.
 */
fun ByteArray.packColors(colors: List<Color>, offset: Int = 0) {
    val needed = colors.size * 3
    require(offset >= 0 && offset + needed <= size) {
        "Cannot pack ${colors.size} colors (${needed} bytes) at offset $offset in array of size $size"
    }
    colors.forEachIndexed { i, color ->
        packColor(color, offset + i * 3)
    }
}

/**
 * Unpack [count] RGB colors from consecutive 3-byte triples starting at [offset].
 */
fun ByteArray.unpackColors(count: Int, offset: Int = 0): List<Color> {
    val needed = count * 3
    require(offset >= 0 && offset + needed <= size) {
        "Cannot unpack $count colors ($needed bytes) at offset $offset in array of size $size"
    }
    return List(count) { i -> unpackColor(offset + i * 3) }
}

/**
 * Set a single channel byte by index.
 */
fun ByteArray.setChannel(index: Int, value: Int) {
    require(index in indices) { "Channel index $index out of range 0..${size - 1}" }
    this[index] = value.coerceIn(0, 255).toByte()
}

/**
 * Get a single channel as an unsigned Int (0-255).
 */
fun ByteArray.getChannel(index: Int): Int {
    require(index in indices) { "Channel index $index out of range 0..${size - 1}" }
    return this[index].toInt() and 0xFF
}

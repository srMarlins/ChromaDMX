package com.chromadmx.vision.camera

/**
 * A single grayscale camera frame.
 *
 * [pixels] is a row-major array of brightness values in the range 0.0 (black)
 * to 1.0 (white). The array length must equal [width] * [height].
 */
data class GrayscaleFrame(
    val pixels: FloatArray,
    val width: Int,
    val height: Int
) {
    init {
        require(pixels.size == width * height) {
            "Pixel array size (${pixels.size}) must equal width*height ($width*$height=${width * height})"
        }
        require(width > 0 && height > 0) {
            "Frame dimensions must be positive: ${width}x$height"
        }
    }

    /** Returns the brightness of the pixel at ([x], [y]). */
    fun pixelAt(x: Int, y: Int): Float {
        require(x in 0 until width && y in 0 until height) {
            "Pixel ($x, $y) out of bounds for ${width}x$height frame"
        }
        return pixels[y * width + x]
    }

    /**
     * Subtracts [baseline] brightness from this frame, clamping to 0.0.
     * Used for ambient baseline subtraction.
     */
    fun subtract(baseline: GrayscaleFrame): GrayscaleFrame {
        require(width == baseline.width && height == baseline.height) {
            "Frame dimensions must match for subtraction: ${width}x$height vs ${baseline.width}x${baseline.height}"
        }
        val result = FloatArray(pixels.size) { i ->
            (pixels[i] - baseline.pixels[i]).coerceAtLeast(0f)
        }
        return GrayscaleFrame(result, width, height)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GrayscaleFrame) return false
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = pixels.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

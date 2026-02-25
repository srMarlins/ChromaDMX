package com.chromadmx.vision

import com.chromadmx.vision.camera.GrayscaleFrame

/**
 * Utilities for generating synthetic grayscale frames for testing.
 *
 * These helpers create frames with bright spots at known positions so that
 * blob detection results can be verified against expected centroids.
 */
object SyntheticFrameHelper {

    /**
     * Create a blank (black) frame.
     */
    fun blankFrame(width: Int, height: Int): GrayscaleFrame =
        GrayscaleFrame(FloatArray(width * height), width, height)

    /**
     * Create a frame with a single circular bright spot.
     *
     * @param width frame width
     * @param height frame height
     * @param cx center X of the spot
     * @param cy center Y of the spot
     * @param radius radius of the spot in pixels
     * @param brightness peak brightness (0.0-1.0) at the center
     * @param falloff if true, brightness decreases linearly from center to edge
     */
    fun singleSpot(
        width: Int,
        height: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        brightness: Float = 1.0f,
        falloff: Boolean = false
    ): GrayscaleFrame {
        val pixels = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - cx
                val dy = y - cy
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist <= radius) {
                    val value = if (falloff) {
                        brightness * (1f - dist / radius)
                    } else {
                        brightness
                    }
                    pixels[y * width + x] = value.coerceIn(0f, 1f)
                }
            }
        }
        return GrayscaleFrame(pixels, width, height)
    }

    /**
     * Create a frame with multiple circular bright spots.
     */
    fun multipleSpots(
        width: Int,
        height: Int,
        spots: List<SpotSpec>
    ): GrayscaleFrame {
        val pixels = FloatArray(width * height)
        for (spot in spots) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val dx = x - spot.cx
                    val dy = y - spot.cy
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist <= spot.radius) {
                        val value = if (spot.falloff) {
                            spot.brightness * (1f - dist / spot.radius)
                        } else {
                            spot.brightness
                        }
                        // Additive blending, clamped
                        val idx = y * width + x
                        pixels[idx] = (pixels[idx] + value).coerceIn(0f, 1f)
                    }
                }
            }
        }
        return GrayscaleFrame(pixels, width, height)
    }

    /**
     * Create a uniform frame (every pixel has the same brightness).
     */
    fun uniformFrame(width: Int, height: Int, brightness: Float): GrayscaleFrame =
        GrayscaleFrame(FloatArray(width * height) { brightness }, width, height)

    /**
     * Set specific pixels to given brightness values on a blank frame.
     */
    fun pixelFrame(width: Int, height: Int, pixels: Map<Pair<Int, Int>, Float>): GrayscaleFrame {
        val arr = FloatArray(width * height)
        for ((pos, brightness) in pixels) {
            val (x, y) = pos
            arr[y * width + x] = brightness
        }
        return GrayscaleFrame(arr, width, height)
    }

    data class SpotSpec(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val brightness: Float = 1.0f,
        val falloff: Boolean = false
    )
}

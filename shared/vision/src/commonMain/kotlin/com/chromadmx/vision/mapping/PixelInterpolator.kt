package com.chromadmx.vision.mapping

import com.chromadmx.vision.detection.Coord2D

/**
 * Interpolates pixel positions along a line segment between two endpoints.
 *
 * When a multi-cell LED bar has its end-pixels fired, blob detection produces
 * two endpoint centroids. This class distributes [pixelCount] pixel positions
 * evenly along the line connecting those endpoints.
 *
 * For a bar with N pixels, position i is at parameter t = i / (N - 1),
 * giving linear interpolation from [start] to [end].
 */
object PixelInterpolator {

    /**
     * Interpolate [pixelCount] evenly spaced positions along the line from
     * [start] to [end].
     *
     * @param start centroid of the first endpoint blob
     * @param end centroid of the second endpoint blob
     * @param pixelCount total number of addressable pixels on the bar (must be >= 2)
     * @return list of [pixelCount] coordinates, from [start] to [end]
     */
    fun interpolate(start: Coord2D, end: Coord2D, pixelCount: Int): List<Coord2D> {
        require(pixelCount >= 2) {
            "pixelCount must be >= 2 for endpoint interpolation, got $pixelCount"
        }
        val steps = pixelCount - 1
        return (0 until pixelCount).map { i ->
            val t = i.toFloat() / steps
            Coord2D(
                x = start.x + (end.x - start.x) * t,
                y = start.y + (end.y - start.y) * t
            )
        }
    }

    /**
     * Interpolate a single pixel's position at a specific index along the bar.
     *
     * @param start centroid of the first endpoint blob
     * @param end centroid of the second endpoint blob
     * @param pixelCount total number of addressable pixels on the bar
     * @param pixelIndex 0-based index of the pixel to locate
     * @return the interpolated position
     */
    fun interpolateAt(start: Coord2D, end: Coord2D, pixelCount: Int, pixelIndex: Int): Coord2D {
        require(pixelCount >= 2) { "pixelCount must be >= 2" }
        require(pixelIndex in 0 until pixelCount) {
            "pixelIndex ($pixelIndex) must be in [0, $pixelCount)"
        }
        val t = pixelIndex.toFloat() / (pixelCount - 1)
        return Coord2D(
            x = start.x + (end.x - start.x) * t,
            y = start.y + (end.y - start.y) * t
        )
    }
}

package com.chromadmx.vision.detection

/**
 * A 2-D coordinate in camera-frame pixel space.
 *
 * Sub-pixel precision is used because centroids are computed as weighted
 * averages and rarely land on exact pixel boundaries.
 */
data class Coord2D(
    val x: Float,
    val y: Float
)

/**
 * Result of blob detection on a single difference frame.
 *
 * [centroid] is the brightness-weighted center of the blob.
 * [pixelCount] is the number of above-threshold pixels in the connected component.
 * [totalBrightness] is the sum of all pixel brightness values in the blob,
 * useful for comparing relative intensity across blobs.
 */
data class DetectedBlob(
    val centroid: Coord2D,
    val pixelCount: Int,
    val totalBrightness: Float
)

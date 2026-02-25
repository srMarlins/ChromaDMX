package com.chromadmx.vision.detection

import com.chromadmx.vision.camera.GrayscaleFrame

/**
 * Detects bright blobs in a grayscale difference frame using brightness
 * thresholding and connected-component labeling (4-connectivity).
 *
 * Algorithm:
 * 1. Threshold the frame to produce a binary mask of "bright" pixels.
 * 2. Run single-pass flood-fill CCL with 4-connectivity (up/down/left/right).
 * 3. For each connected component, compute the brightness-weighted centroid.
 * 4. Filter components smaller than [minBlobSize].
 * 5. Return detected blobs sorted by total brightness (brightest first).
 */
class BlobDetector(
    /** Minimum brightness (0.0-1.0) for a pixel to be considered part of a blob. */
    val brightnessThreshold: Float = 0.3f,
    /** Minimum number of pixels for a blob to be reported. */
    val minBlobSize: Int = 3
) {
    init {
        require(brightnessThreshold in 0f..1f) {
            "brightnessThreshold must be in [0, 1], got $brightnessThreshold"
        }
        require(minBlobSize >= 1) {
            "minBlobSize must be >= 1, got $minBlobSize"
        }
    }

    /**
     * Detect blobs in the given difference frame.
     *
     * The frame is typically the result of subtracting an ambient baseline
     * from a captured frame: `captured.subtract(ambient)`.
     *
     * @return list of [DetectedBlob] sorted by total brightness descending
     */
    fun detect(frame: GrayscaleFrame): List<DetectedBlob> {
        val w = frame.width
        val h = frame.height
        val pixels = frame.pixels

        // Label array: 0 = unlabeled, >0 = component label
        val labels = IntArray(w * h)
        var nextLabel = 1

        // Accumulator per label: (sumX, sumY, sumBrightness, count)
        val components = mutableMapOf<Int, BlobAccumulator>()

        // Single-pass flood-fill CCL with 4-connectivity
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (pixels[idx] >= brightnessThreshold && labels[idx] == 0) {
                    val label = nextLabel++
                    val acc = BlobAccumulator()
                    floodFill(pixels, labels, w, h, x, y, label, acc)
                    components[label] = acc
                }
            }
        }

        // Convert accumulators to DetectedBlob, filter by min size
        return components.values
            .filter { it.count >= minBlobSize }
            .map { acc ->
                DetectedBlob(
                    centroid = Coord2D(
                        x = acc.weightedSumX / acc.totalBrightness,
                        y = acc.weightedSumY / acc.totalBrightness
                    ),
                    pixelCount = acc.count,
                    totalBrightness = acc.totalBrightness
                )
            }
            .sortedByDescending { it.totalBrightness }
    }

    /**
     * Iterative flood-fill using an explicit stack to avoid stack overflow
     * on large blobs. Uses 4-connectivity (up/down/left/right).
     */
    private fun floodFill(
        pixels: FloatArray,
        labels: IntArray,
        w: Int,
        h: Int,
        startX: Int,
        startY: Int,
        label: Int,
        acc: BlobAccumulator
    ) {
        val stack = ArrayDeque<Int>()
        val startIdx = startY * w + startX
        stack.addLast(startIdx)
        labels[startIdx] = label

        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            val x = idx % w
            val y = idx / w
            val brightness = pixels[idx]

            acc.weightedSumX += x * brightness
            acc.weightedSumY += y * brightness
            acc.totalBrightness += brightness
            acc.count++

            // 4-connectivity neighbors: right, left, down, up
            if (x + 1 < w) {
                val nIdx = idx + 1
                if (labels[nIdx] == 0 && pixels[nIdx] >= brightnessThreshold) {
                    labels[nIdx] = label
                    stack.addLast(nIdx)
                }
            }
            if (x - 1 >= 0) {
                val nIdx = idx - 1
                if (labels[nIdx] == 0 && pixels[nIdx] >= brightnessThreshold) {
                    labels[nIdx] = label
                    stack.addLast(nIdx)
                }
            }
            if (y + 1 < h) {
                val nIdx = idx + w
                if (labels[nIdx] == 0 && pixels[nIdx] >= brightnessThreshold) {
                    labels[nIdx] = label
                    stack.addLast(nIdx)
                }
            }
            if (y - 1 >= 0) {
                val nIdx = idx - w
                if (labels[nIdx] == 0 && pixels[nIdx] >= brightnessThreshold) {
                    labels[nIdx] = label
                    stack.addLast(nIdx)
                }
            }
        }
    }

    /** Mutable accumulator used during flood-fill to compute centroid stats. */
    private class BlobAccumulator {
        var weightedSumX: Float = 0f
        var weightedSumY: Float = 0f
        var totalBrightness: Float = 0f
        var count: Int = 0
    }
}

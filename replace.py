import sys

with open('./shared/vision/src/commonMain/kotlin/com/chromadmx/vision/detection/BlobDetector.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""        // Single-pass flood-fill CCL with 4-connectivity
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
            .sortedByDescending { it.totalBrightness }""",
"""        // Single-pass flood-fill CCL with 4-connectivity
        // Pre-allocate a single stack to avoid auto-boxing and allocations in the hot path
        val stack = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (pixels[idx] >= brightnessThreshold && labels[idx] == 0) {
                    val label = nextLabel++
                    val acc = BlobAccumulator()
                    floodFill(pixels, labels, w, h, x, y, label, acc, stack)
                    components[label] = acc
                }
            }
        }

        // Convert accumulators to DetectedBlob, filter by min size
        return components.values
            .mapNotNull { acc ->
                if (acc.count >= minBlobSize) {
                    DetectedBlob(
                        centroid = Coord2D(
                            x = acc.weightedSumX / acc.totalBrightness,
                            y = acc.weightedSumY / acc.totalBrightness
                        ),
                        pixelCount = acc.count,
                        totalBrightness = acc.totalBrightness
                    )
                } else null
            }
            .sortedByDescending { it.totalBrightness }"""
)

content = content.replace(
"""    private fun floodFill(
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
    }""",
"""    private fun floodFill(
        pixels: FloatArray,
        labels: IntArray,
        w: Int,
        h: Int,
        startX: Int,
        startY: Int,
        label: Int,
        acc: BlobAccumulator,
        stack: IntArray
    ) {
        var stackSize = 0
        val startIdx = startY * w + startX
        stack[stackSize++] = startIdx
        labels[startIdx] = label

        while (stackSize > 0) {
            val idx = stack[--stackSize]
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
                    stack[stackSize++] = nIdx
                }
            }
            if (x - 1 >= 0) {
                val nIdx = idx - 1
                if (labels[nIdx] == 0 && pixels[nIdx] >= brightnessThreshold) {
                    labels[nIdx] = label
                    stack[stackSize++] = nIdx
                }
            }
            if (y + 1 < h) {
                val nIdx = idx + w
                if (labels[nIdx] == 0 && pixels[nIdx] >= brightnessThreshold) {
                    labels[nIdx] = label
                    stack[stackSize++] = nIdx
                }
            }
            if (y - 1 >= 0) {
                val nIdx = idx - w
                if (labels[nIdx] == 0 && pixels[nIdx] >= brightnessThreshold) {
                    labels[nIdx] = label
                    stack[stackSize++] = nIdx
                }
            }
        }
    }"""
)

with open('./shared/vision/src/commonMain/kotlin/com/chromadmx/vision/detection/BlobDetector.kt', 'w') as f:
    f.write(content)

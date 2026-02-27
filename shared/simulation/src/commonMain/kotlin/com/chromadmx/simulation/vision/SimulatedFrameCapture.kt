package com.chromadmx.simulation.vision

import com.chromadmx.core.model.Vec3
import com.chromadmx.vision.calibration.FrameCapture
import com.chromadmx.vision.camera.GrayscaleFrame
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Simulated camera that renders synthetic grayscale frames showing
 * bright Gaussian blobs at the physical positions of active fixtures.
 *
 * Projects 3D bar positions onto a 2D frame using (x, z) — the camera
 * faces the V-formation from the front, so x maps to horizontal and
 * z (height) maps to vertical. Depth (y) is ignored for 2D projection.
 */
class SimulatedFrameCapture(
    private val pixelPositions: Map<String, List<Vec3>>,
    private val dmxController: SimulatedDmxController,
    private val frameWidth: Int = 640,
    private val frameHeight: Int = 480,
    private val blobRadius: Float = 6f,
    private val xRange: ClosedFloatingPointRange<Float> = -3f..3f,
    private val zRange: ClosedFloatingPointRange<Float> = 0f..2.5f,
) : FrameCapture {

    override suspend fun captureFrame(): GrayscaleFrame {
        val pixels = FloatArray(frameWidth * frameHeight)
        val activePixelMap = dmxController.activePixels.value

        for ((fixtureId, activePixelIndices) in activePixelMap) {
            val positions = pixelPositions[fixtureId] ?: continue
            for (pixelIndex in activePixelIndices) {
                if (pixelIndex !in positions.indices) continue
                val pos = positions[pixelIndex]
                renderBlob(pixels, pos)
            }
        }

        return GrayscaleFrame(pixels, frameWidth, frameHeight)
    }

    private fun renderBlob(pixels: FloatArray, worldPos: Vec3) {
        val xFrac = (worldPos.x - xRange.start) / (xRange.endInclusive - xRange.start)
        val zFrac = (worldPos.z - zRange.start) / (zRange.endInclusive - zRange.start)

        val cx = (xFrac * frameWidth).roundToInt()
        val cy = ((1f - zFrac) * frameHeight).roundToInt()

        val r = blobRadius.roundToInt()
        val sigma = blobRadius / 2.5f

        for (dy in -r..r) {
            for (dx in -r..r) {
                val px = cx + dx
                val py = cy + dy
                if (px < 0 || px >= frameWidth || py < 0 || py >= frameHeight) continue

                val dist2 = (dx * dx + dy * dy).toFloat()
                val brightness = exp(-dist2 / (2f * sigma * sigma))
                val idx = py * frameWidth + px
                pixels[idx] = (pixels[idx] + brightness).coerceAtMost(1f)
            }
        }
    }
}

package com.chromadmx.simulation.camera

import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Synthetic camera frame generator for testing the vision module's blob detection.
 *
 * Projects 3D fixture positions into 2D image space using a simple pinhole
 * camera model, then renders Gaussian blobs at fixture locations where the
 * fixture is "on" (has non-zero color).
 *
 * The output is a grayscale [ByteArray] frame (one byte per pixel) compatible
 * with the vision module's BlobDetector input format.
 *
 * @param width         Frame width in pixels
 * @param height        Frame height in pixels
 * @param noiseLevel    Noise amplitude (0.0 = none, 1.0 = full range random)
 * @param blobRadius    Radius of the Gaussian blob in pixels
 * @param ambientLevel  Base ambient light level (0-255)
 * @param cameraPosition 3D position of the simulated camera
 * @param cameraTarget   3D point the camera is looking at
 * @param fovDegrees    Horizontal field of view in degrees
 * @param random        Random source for noise (injectable for deterministic tests)
 */
class SimulatedCamera(
    val width: Int = 640,
    val height: Int = 480,
    val noiseLevel: Float = 0.02f,
    val blobRadius: Float = 15.0f,
    val ambientLevel: Int = 10,
    val cameraPosition: Vec3 = Vec3(0f, -5f, 1.5f),
    val cameraTarget: Vec3 = Vec3(0f, 2f, 3f),
    val fovDegrees: Float = 90f,
    private val random: Random = Random.Default
) {
    /**
     * A fixture and its current brightness/color for frame generation.
     */
    data class FixtureState(
        val fixture3D: Fixture3D,
        val color: Color = Color.BLACK
    ) {
        /** Whether this fixture is contributing light (any non-zero channel). */
        val isOn: Boolean get() = color.r > 0.001f || color.g > 0.001f || color.b > 0.001f

        /** Grayscale brightness (0-255) using standard luminance weights. */
        val brightness: Int get() {
            val luminance = 0.299f * color.r + 0.587f * color.g + 0.114f * color.b
            return (luminance.coerceIn(0f, 1f) * 255f).roundToInt()
        }
    }

    /**
     * Result of projecting a 3D fixture position to 2D image space.
     */
    data class ProjectedPoint(
        val x: Float,
        val y: Float,
        val depth: Float,
        val isVisible: Boolean
    )

    /**
     * Generate a synthetic grayscale frame.
     *
     * @param fixtureStates List of fixtures and their current on/off + color state
     * @return Grayscale byte array of size [width] * [height]
     */
    fun generateFrame(fixtureStates: List<FixtureState>): ByteArray {
        val frame = ByteArray(width * height)

        // Fill with ambient light
        if (ambientLevel > 0) {
            frame.fill(ambientLevel.coerceIn(0, 255).toByte())
        }

        // Project each "on" fixture and draw a blob
        for (state in fixtureStates) {
            if (!state.isOn) continue

            val projected = projectToImage(state.fixture3D.position)
            if (!projected.isVisible) continue

            drawGaussianBlob(
                frame = frame,
                cx = projected.x,
                cy = projected.y,
                intensity = state.brightness,
                radius = blobRadius
            )
        }

        // Add noise
        if (noiseLevel > 0f) {
            addNoise(frame)
        }

        return frame
    }

    /**
     * Generate a frame from fixture positions with a simple on/off list.
     *
     * @param fixtures     All fixtures in the rig
     * @param onFixtureIds Set of fixture IDs that are currently "on" (white at full brightness)
     * @return Grayscale byte array
     */
    fun generateFrame(fixtures: List<Fixture3D>, onFixtureIds: Set<String>): ByteArray {
        val states = fixtures.map { fixture ->
            FixtureState(
                fixture3D = fixture,
                color = if (fixture.fixture.fixtureId in onFixtureIds) Color.WHITE else Color.BLACK
            )
        }
        return generateFrame(states)
    }

    /**
     * Project a 3D world position to 2D image coordinates.
     *
     * Uses a simplified pinhole camera model:
     * 1. Transform world coordinates to camera space
     * 2. Perspective divide
     * 3. Map to image pixel coordinates
     */
    fun projectToImage(worldPos: Vec3): ProjectedPoint {
        // Camera basis vectors
        val forward = (cameraTarget - cameraPosition).normalized()
        val worldUp = Vec3.UP
        // In our coordinate system z is up, so worldUp = (0, 0, 1) would be better
        val up = Vec3(0f, 0f, 1f)
        val right = (forward cross up).normalized()
        val cameraUp = (right cross forward).normalized()

        // Transform to camera space
        val relative = worldPos - cameraPosition
        val cx = relative dot right
        val cy = relative dot cameraUp
        val cz = relative dot forward // depth

        // Not visible if behind camera
        if (cz <= 0.01f) {
            return ProjectedPoint(0f, 0f, cz, isVisible = false)
        }

        // Perspective projection
        val fovRad = fovDegrees * (kotlin.math.PI.toFloat() / 180f)
        val focalLength = (width / 2f) / kotlin.math.tan(fovRad / 2f).toFloat()

        val screenX = (cx / cz) * focalLength + width / 2f
        val screenY = -(cy / cz) * focalLength + height / 2f

        val isVisible = screenX >= -blobRadius && screenX < width + blobRadius &&
            screenY >= -blobRadius && screenY < height + blobRadius

        return ProjectedPoint(screenX, screenY, cz, isVisible)
    }

    /**
     * Draw a Gaussian blob at the specified position.
     */
    private fun drawGaussianBlob(
        frame: ByteArray,
        cx: Float,
        cy: Float,
        intensity: Int,
        radius: Float
    ) {
        val r = radius.roundToInt()
        val sigma = radius / 2.5f
        val sigmaSquared = sigma * sigma

        val startX = max(0, (cx - r * 2).roundToInt())
        val endX = min(width - 1, (cx + r * 2).roundToInt())
        val startY = max(0, (cy - r * 2).roundToInt())
        val endY = min(height - 1, (cy + r * 2).roundToInt())

        for (py in startY..endY) {
            for (px in startX..endX) {
                val dx = px - cx
                val dy = py - cy
                val distSquared = dx * dx + dy * dy

                // Gaussian falloff
                val gaussValue = exp(-distSquared / (2f * sigmaSquared))
                val addValue = (intensity * gaussValue).roundToInt()

                if (addValue > 0) {
                    val idx = py * width + px
                    val current = frame[idx].toInt() and 0xFF
                    frame[idx] = min(255, current + addValue).toByte()
                }
            }
        }
    }

    /**
     * Add random noise to the frame.
     */
    private fun addNoise(frame: ByteArray) {
        val noiseRange = (noiseLevel * 255f).roundToInt()
        if (noiseRange <= 0) return

        for (i in frame.indices) {
            val noise = random.nextInt(-noiseRange, noiseRange + 1)
            val current = frame[i].toInt() and 0xFF
            frame[i] = (current + noise).coerceIn(0, 255).toByte()
        }
    }

    /**
     * Get the pixel value at a specific coordinate from a frame.
     */
    fun getPixel(frame: ByteArray, x: Int, y: Int): Int {
        require(x in 0 until width && y in 0 until height) {
            "Pixel ($x, $y) out of bounds ($width x $height)"
        }
        return frame[y * width + x].toInt() and 0xFF
    }

    /**
     * Find the brightest pixel value in a region around a point.
     */
    fun peakBrightness(frame: ByteArray, cx: Int, cy: Int, searchRadius: Int = 10): Int {
        var peak = 0
        val startX = max(0, cx - searchRadius)
        val endX = min(width - 1, cx + searchRadius)
        val startY = max(0, cy - searchRadius)
        val endY = min(height - 1, cy + searchRadius)

        for (py in startY..endY) {
            for (px in startX..endX) {
                val value = frame[py * width + px].toInt() and 0xFF
                if (value > peak) peak = value
            }
        }
        return peak
    }
}

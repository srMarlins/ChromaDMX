package com.chromadmx.vision.calibration

import com.chromadmx.vision.camera.GrayscaleFrame
import com.chromadmx.vision.detection.BlobDetector
import com.chromadmx.vision.detection.Coord2D
import com.chromadmx.vision.mapping.SpatialMap
import com.chromadmx.vision.mapping.SpatialMapBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Describes a fixture to be scanned.
 *
 * @param fixtureId unique identifier
 * @param isMultiCell true if this is a multi-cell LED bar requiring endpoint detection
 * @param pixelCount number of addressable pixels (only relevant if [isMultiCell] is true)
 */
data class ScanFixture(
    val fixtureId: String,
    val isMultiCell: Boolean = false,
    val pixelCount: Int = 1
)

/**
 * Interface for capturing camera frames. Abstracts the platform camera so
 * the orchestrator can be tested with synthetic frames.
 */
interface FrameCapture {
    suspend fun captureFrame(): GrayscaleFrame
}

/**
 * Orchestrates the full scan pipeline: ambient capture, per-fixture fire-and-detect,
 * endpoint detection for multi-cell bars, and spatial map assembly.
 *
 * Emits scan progress via [state] as a [StateFlow].
 *
 * ## Pipeline
 * 1. Capture ambient baseline
 * 2. For each fixture: fire at full white, capture, subtract ambient, detect blob centroid
 * 3. For each multi-cell fixture: fire end-pixels, detect two endpoint blobs
 * 4. Build SpatialMap from all detected positions
 *
 * ## Timing
 * - [fireSettleMs]: delay after firing before capture (default 80ms)
 * - [decayMs]: delay after turning off before next fixture (default 50ms)
 */
class ScanOrchestrator(
    private val dmxController: DmxController,
    private val frameCapture: FrameCapture,
    private val blobDetector: BlobDetector = BlobDetector(),
    private val fireSettleMs: Long = 80L,
    private val decayMs: Long = 50L
) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)

    /** Observable scan state. */
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /**
     * Run the full scan pipeline.
     *
     * @param fixtures list of fixtures to scan, in order
     * @return the completed [SpatialMap], or null if the scan failed
     */
    suspend fun scan(fixtures: List<ScanFixture>): SpatialMap? {
        if (fixtures.isEmpty()) {
            _state.value = ScanState.Error("No fixtures to scan")
            return null
        }

        try {
            // Step 1: Capture ambient baseline
            _state.value = ScanState.CapturingBaseline
            dmxController.blackout()
            delay(decayMs)
            val ambient = frameCapture.captureFrame()

            val mapBuilder = SpatialMapBuilder()

            // Step 2: Scan each fixture individually
            for ((index, scanFixture) in fixtures.withIndex()) {
                _state.value = ScanState.Scanning(
                    currentFixtureIndex = index,
                    totalFixtures = fixtures.size,
                    currentFixtureId = scanFixture.fixtureId
                )

                // Fire fixture at full white
                dmxController.fireFixture(scanFixture.fixtureId)
                delay(fireSettleMs)

                // Capture and subtract ambient
                val captured = frameCapture.captureFrame()
                val diff = captured.subtract(ambient)

                // Detect the brightest blob
                val blobs = blobDetector.detect(diff)
                if (blobs.isEmpty()) {
                    _state.value = ScanState.Error(
                        "No blob detected for fixture '${scanFixture.fixtureId}'"
                    )
                    dmxController.blackout()
                    return null
                }

                // Use the brightest (first) blob
                val centroid = blobs[0].centroid

                if (!scanFixture.isMultiCell) {
                    mapBuilder.addSingleCell(scanFixture.fixtureId, centroid)
                }
                // Multi-cell centroids stored temporarily; endpoints detected in step 3

                // Turn off and wait for decay
                dmxController.turnOffFixture(scanFixture.fixtureId)
                delay(decayMs)
            }

            // Step 3: Endpoint detection for multi-cell fixtures
            val multiCellFixtures = fixtures.filter { it.isMultiCell }
            for ((index, scanFixture) in multiCellFixtures.withIndex()) {
                _state.value = ScanState.DetectingEndpoints(
                    currentFixtureIndex = index,
                    totalMultiCellFixtures = multiCellFixtures.size,
                    currentFixtureId = scanFixture.fixtureId
                )

                // Fire end-pixels only
                dmxController.fireEndPixels(scanFixture.fixtureId)
                delay(fireSettleMs)

                // Capture and subtract ambient
                val captured = frameCapture.captureFrame()
                val diff = captured.subtract(ambient)

                // Detect two endpoint blobs
                val blobs = blobDetector.detect(diff)
                if (blobs.size < 2) {
                    _state.value = ScanState.Error(
                        "Expected 2 endpoint blobs for fixture '${scanFixture.fixtureId}', " +
                            "found ${blobs.size}"
                    )
                    dmxController.blackout()
                    return null
                }

                // Use the two brightest blobs as endpoints
                // Sort by X to ensure consistent left-to-right ordering
                val endpoints = blobs.take(2).sortedBy { it.centroid.x }
                mapBuilder.addMultiCell(
                    fixtureId = scanFixture.fixtureId,
                    startEndpoint = endpoints[0].centroid,
                    endEndpoint = endpoints[1].centroid,
                    pixelCount = scanFixture.pixelCount
                )

                dmxController.turnOffFixture(scanFixture.fixtureId)
                delay(decayMs)
            }

            // Step 4: Build spatial map
            dmxController.blackout()
            val spatialMap = mapBuilder.build()
            _state.value = ScanState.Complete(spatialMap)
            return spatialMap

        } catch (e: Exception) {
            _state.value = ScanState.Error("Scan failed: ${e.message}", e)
            try { dmxController.blackout() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * Reset the orchestrator to idle state.
     */
    fun reset() {
        _state.value = ScanState.Idle
    }
}

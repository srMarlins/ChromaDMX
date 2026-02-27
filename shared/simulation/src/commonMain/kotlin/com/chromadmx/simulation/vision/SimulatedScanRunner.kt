package com.chromadmx.simulation.vision

import com.chromadmx.simulation.rigs.PixelBarVRig
import com.chromadmx.vision.calibration.ScanFixture
import com.chromadmx.vision.calibration.ScanOrchestrator
import com.chromadmx.vision.calibration.ScanState
import com.chromadmx.vision.detection.BlobDetector
import com.chromadmx.vision.mapping.SpatialMap
import kotlinx.coroutines.flow.StateFlow

/**
 * Runs the full vision scan pipeline using simulated DMX + camera
 * against the [PixelBarVRig] V-formation rig.
 *
 * Wires together [SimulatedDmxController], [SimulatedFrameCapture], and
 * [ScanOrchestrator] using the real [BlobDetector]. The UI observes
 * [scanState] for progress and [activeFixtures] for flash animation.
 *
 * This class is single-use: create a new instance for each scan.
 */
class SimulatedScanRunner {
    private val dmxController = SimulatedDmxController(
        pixelsPerFixture = PixelBarVRig.PIXELS_PER_BAR,
    )

    private val frameCapture = SimulatedFrameCapture(
        pixelPositions = PixelBarVRig.pixelPositions(),
        dmxController = dmxController,
    )

    private val orchestrator = ScanOrchestrator(
        dmxController = dmxController,
        frameCapture = frameCapture,
        blobDetector = BlobDetector(brightnessThreshold = 0.2f, minBlobSize = 2),
        fireSettleMs = 0L,
        decayMs = 0L,
    )

    /** Observable scan state — progress, errors, completion. */
    val scanState: StateFlow<ScanState> get() = orchestrator.state

    /** Set of fixture IDs currently being flashed by the DMX controller. */
    val activeFixtures: StateFlow<Set<String>> get() = dmxController.activeFixtures

    /**
     * Run the full scan pipeline against the rig.
     *
     * @return the completed [SpatialMap], or null if the scan failed
     */
    suspend fun runScan(): SpatialMap? {
        val fixtures = PixelBarVRig.createFixtures()
        val scanFixtures = fixtures.map { f3d ->
            ScanFixture(
                fixtureId = f3d.fixture.fixtureId,
                isMultiCell = true,
                pixelCount = PixelBarVRig.PIXELS_PER_BAR,
            )
        }
        return orchestrator.scan(scanFixtures)
    }
}

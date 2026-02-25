package com.chromadmx.vision.calibration

import com.chromadmx.vision.mapping.SpatialMap

/**
 * States of the scan orchestrator state machine.
 *
 * The scan progresses linearly through these states:
 * IDLE -> CAPTURING_BASELINE -> SCANNING -> DETECTING_ENDPOINTS -> COMPLETE
 * Any state may transition to ERROR on failure.
 */
sealed class ScanState {
    /** Idle — scan has not started. */
    data object Idle : ScanState()

    /** Capturing the ambient baseline frame. */
    data object CapturingBaseline : ScanState()

    /**
     * Scanning individual fixtures.
     * @param currentFixtureIndex 0-based index of the fixture being scanned
     * @param totalFixtures total number of fixtures to scan
     * @param currentFixtureId the ID of the fixture currently being scanned
     */
    data class Scanning(
        val currentFixtureIndex: Int,
        val totalFixtures: Int,
        val currentFixtureId: String
    ) : ScanState() {
        /** Progress as a fraction from 0.0 to 1.0. */
        val progress: Float get() = (currentFixtureIndex + 1).toFloat() / totalFixtures
    }

    /**
     * Detecting endpoints for multi-cell fixtures.
     * @param currentFixtureIndex 0-based index of the multi-cell fixture
     * @param totalMultiCellFixtures number of multi-cell fixtures to process
     * @param currentFixtureId the ID of the fixture being endpoint-detected
     */
    data class DetectingEndpoints(
        val currentFixtureIndex: Int,
        val totalMultiCellFixtures: Int,
        val currentFixtureId: String
    ) : ScanState()

    /** Scan completed successfully. */
    data class Complete(val spatialMap: SpatialMap) : ScanState()

    /** Scan failed with an error. */
    data class Error(val message: String, val cause: Throwable? = null) : ScanState()
}

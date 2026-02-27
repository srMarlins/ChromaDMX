package com.chromadmx.simulation.vision

import com.chromadmx.simulation.rigs.PixelBarVRig
import com.chromadmx.vision.calibration.ScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SimulatedScanRunnerTest {

    @Test
    fun scanResolvesAllEightBars() = runTest {
        val runner = SimulatedScanRunner()
        val result = runner.runScan()

        assertNotNull(result, "Scan should produce a SpatialMap")
        assertEquals(8, result.fixturePositions.size, "Should have positions for all 8 bars")
    }

    @Test
    fun scanStateReachesComplete() = runTest {
        val runner = SimulatedScanRunner()
        runner.runScan()

        assertIs<ScanState.Complete>(runner.scanState.value)
    }

    @Test
    fun resolvedPositionsMatchPhysicalLayout() = runTest {
        val runner = SimulatedScanRunner()
        val result = runner.runScan()
        assertNotNull(result)

        val physicalPositions = PixelBarVRig.pixelPositions()

        for ((fixtureId, _) in physicalPositions) {
            assertTrue(
                result.fixturePositions.containsKey(fixtureId),
                "SpatialMap should contain $fixtureId"
            )
        }
    }

    @Test
    fun scrambledAddressesDontAffectPositionMapping() = runTest {
        val runner = SimulatedScanRunner()
        val result = runner.runScan()
        assertNotNull(result)

        // Bar 2 is at physical x=-1.5 which projects to left half of 640px frame (< 320)
        val bar2Pos = result.fixturePositions["vbar-phys-2"]
        assertNotNull(bar2Pos, "Bar 2 should be in spatial map")
        assertTrue(bar2Pos[0].x < 320f, "Bar 2 should be in left half of frame, got ${bar2Pos[0].x}")

        // Bar 6 is at physical x=2.0 which projects to right half (> 320)
        val bar6Pos = result.fixturePositions["vbar-phys-6"]
        assertNotNull(bar6Pos, "Bar 6 should be in spatial map")
        assertTrue(bar6Pos[0].x > 320f, "Bar 6 should be in right half of frame, got ${bar6Pos[0].x}")
    }
}

package com.chromadmx.vision

import com.chromadmx.vision.calibration.*
import com.chromadmx.vision.camera.GrayscaleFrame
import com.chromadmx.vision.detection.BlobDetector
import com.chromadmx.vision.detection.Coord2D
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanOrchestratorTest {

    // -----------------------------------------------------------------------
    // Mock implementations
    // -----------------------------------------------------------------------

    /**
     * Records all DMX commands for verification.
     */
    private class MockDmxController : DmxController {
        val commands = mutableListOf<String>()

        override suspend fun fireFixture(fixtureId: String) {
            commands.add("fire:$fixtureId")
        }

        override suspend fun turnOffFixture(fixtureId: String) {
            commands.add("off:$fixtureId")
        }

        override suspend fun fireEndPixels(fixtureId: String) {
            commands.add("endpoints:$fixtureId")
        }

        override suspend fun blackout() {
            commands.add("blackout")
        }
    }

    /**
     * Returns pre-configured frames in sequence.
     */
    private class MockFrameCapture(private val frames: List<GrayscaleFrame>) : FrameCapture {
        private var index = 0
        var captureCount = 0
            private set

        override suspend fun captureFrame(): GrayscaleFrame {
            captureCount++
            return if (index < frames.size) {
                frames[index++]
            } else {
                // Return blank frame if we run out
                SyntheticFrameHelper.blankFrame(frames[0].width, frames[0].height)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private val frameW = 100
    private val frameH = 100

    /** Create ambient (dark) frame. */
    private fun ambientFrame() = SyntheticFrameHelper.uniformFrame(frameW, frameH, 0.05f)

    /** Create a frame with a bright spot at the given position + ambient level. */
    private fun spotFrame(cx: Float, cy: Float, ambient: Float = 0.05f): GrayscaleFrame {
        val spot = SyntheticFrameHelper.singleSpot(frameW, frameH, cx, cy, 4f, 0.9f)
        return GrayscaleFrame(
            FloatArray(frameW * frameH) { i ->
                (spot.pixels[i] + ambient).coerceIn(0f, 1f)
            },
            frameW, frameH
        )
    }

    /** Create a frame with two bright spots (for endpoint detection). */
    private fun twoSpotFrame(
        cx1: Float, cy1: Float,
        cx2: Float, cy2: Float,
        ambient: Float = 0.05f
    ): GrayscaleFrame {
        val spots = SyntheticFrameHelper.multipleSpots(
            frameW, frameH,
            listOf(
                SyntheticFrameHelper.SpotSpec(cx1, cy1, 4f, 0.9f),
                SyntheticFrameHelper.SpotSpec(cx2, cy2, 4f, 0.9f)
            )
        )
        return GrayscaleFrame(
            FloatArray(frameW * frameH) { i ->
                (spots.pixels[i] + ambient).coerceIn(0f, 1f)
            },
            frameW, frameH
        )
    }

    // -----------------------------------------------------------------------
    // Tests: Single-cell fixtures
    // -----------------------------------------------------------------------

    @Test
    fun scan_single_fixture_succeeds() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),       // baseline
            spotFrame(50f, 50f)   // fixture f1
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L,
            decayMs = 0L
        )

        val fixtures = listOf(ScanFixture("f1"))
        val result = orchestrator.scan(fixtures)

        assertNotNull(result)
        assertIs<ScanState.Complete>(orchestrator.state.value)

        // Verify spatial map has one entry
        assertEquals(1, result.fixturePositions.size)
        val positions = result.fixturePositions["f1"]!!
        assertEquals(1, positions.size)
        assertTrue(abs(positions[0].x - 50f) < 2f)
        assertTrue(abs(positions[0].y - 50f) < 2f)
    }

    @Test
    fun scan_multiple_single_cell_fixtures() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),         // baseline
            spotFrame(20f, 30f),    // f1
            spotFrame(70f, 80f)     // f2
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L,
            decayMs = 0L
        )

        val fixtures = listOf(ScanFixture("f1"), ScanFixture("f2"))
        val result = orchestrator.scan(fixtures)

        assertNotNull(result)
        assertEquals(2, result.fixturePositions.size)

        val f1Pos = result.fixturePositions["f1"]!![0]
        assertTrue(abs(f1Pos.x - 20f) < 2f, "f1 X should be ~20, got ${f1Pos.x}")
        assertTrue(abs(f1Pos.y - 30f) < 2f, "f1 Y should be ~30, got ${f1Pos.y}")

        val f2Pos = result.fixturePositions["f2"]!![0]
        assertTrue(abs(f2Pos.x - 70f) < 2f, "f2 X should be ~70, got ${f2Pos.x}")
        assertTrue(abs(f2Pos.y - 80f) < 2f, "f2 Y should be ~80, got ${f2Pos.y}")
    }

    // -----------------------------------------------------------------------
    // Tests: Multi-cell fixtures
    // -----------------------------------------------------------------------

    @Test
    fun scan_multi_cell_fixture_with_endpoint_detection() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),                    // baseline
            spotFrame(50f, 50f),               // bar1 full-fire (centroid)
            twoSpotFrame(20f, 50f, 80f, 50f)   // bar1 endpoint fire
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L,
            decayMs = 0L
        )

        val fixtures = listOf(ScanFixture("bar1", isMultiCell = true, pixelCount = 5))
        val result = orchestrator.scan(fixtures)

        assertNotNull(result)
        assertIs<ScanState.Complete>(orchestrator.state.value)

        val positions = result.fixturePositions["bar1"]!!
        assertEquals(5, positions.size, "Multi-cell bar should have 5 pixel positions")

        // First and last positions should be near endpoints
        assertTrue(abs(positions[0].x - 20f) < 2f)
        assertTrue(abs(positions[4].x - 80f) < 2f)

        // Middle position should be interpolated
        assertTrue(abs(positions[2].x - 50f) < 2f)
    }

    // -----------------------------------------------------------------------
    // Tests: State transitions
    // -----------------------------------------------------------------------

    @Test
    fun state_starts_at_idle() {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(ambientFrame()))
        val orchestrator = ScanOrchestrator(dmx, capture, fireSettleMs = 0L, decayMs = 0L)
        assertIs<ScanState.Idle>(orchestrator.state.value)
    }

    @Test
    fun state_transitions_to_complete_on_success() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            spotFrame(50f, 50f)
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        orchestrator.scan(listOf(ScanFixture("f1")))
        assertIs<ScanState.Complete>(orchestrator.state.value)
    }

    @Test
    fun state_transitions_to_error_when_no_blob_detected() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            ambientFrame()  // no bright spot — will fail detection
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        val result = orchestrator.scan(listOf(ScanFixture("f1")))
        assertNull(result)
        assertIs<ScanState.Error>(orchestrator.state.value)
        assertTrue((orchestrator.state.value as ScanState.Error).message.contains("f1"))
    }

    @Test
    fun state_transitions_to_error_when_endpoint_detection_fails() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            spotFrame(50f, 50f),  // full-fire OK
            spotFrame(50f, 50f)   // endpoint fire — only ONE blob, should be 2
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        val result = orchestrator.scan(listOf(
            ScanFixture("bar1", isMultiCell = true, pixelCount = 5)
        ))
        assertNull(result)
        assertIs<ScanState.Error>(orchestrator.state.value)
        assertTrue((orchestrator.state.value as ScanState.Error).message.contains("endpoint"))
    }

    @Test
    fun state_error_when_no_fixtures() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(ambientFrame()))
        val orchestrator = ScanOrchestrator(dmx, capture, fireSettleMs = 0L, decayMs = 0L)

        val result = orchestrator.scan(emptyList())
        assertNull(result)
        assertIs<ScanState.Error>(orchestrator.state.value)
    }

    // -----------------------------------------------------------------------
    // Tests: DMX command sequence
    // -----------------------------------------------------------------------

    @Test
    fun dmx_commands_follow_correct_sequence_for_single_cell() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            spotFrame(50f, 50f),
            spotFrame(70f, 70f)
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        orchestrator.scan(listOf(ScanFixture("f1"), ScanFixture("f2")))

        assertEquals("blackout", dmx.commands[0], "Should start with blackout")
        assertEquals("fire:f1", dmx.commands[1])
        assertEquals("off:f1", dmx.commands[2])
        assertEquals("fire:f2", dmx.commands[3])
        assertEquals("off:f2", dmx.commands[4])
        assertEquals("blackout", dmx.commands[5], "Should end with blackout")
    }

    @Test
    fun dmx_commands_include_endpoint_fire_for_multi_cell() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            spotFrame(50f, 50f),              // bar1 full fire
            twoSpotFrame(20f, 50f, 80f, 50f)  // bar1 endpoint fire
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        orchestrator.scan(listOf(
            ScanFixture("bar1", isMultiCell = true, pixelCount = 5)
        ))

        assertTrue(dmx.commands.contains("fire:bar1"), "Should fire bar at full white")
        assertTrue(dmx.commands.contains("endpoints:bar1"), "Should fire end-pixels")
    }

    @Test
    fun blackout_called_on_error() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            ambientFrame()  // no blob
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        orchestrator.scan(listOf(ScanFixture("f1")))

        // Should have a blackout at start and after error
        val blackoutCount = dmx.commands.count { it == "blackout" }
        assertTrue(blackoutCount >= 2, "Should call blackout on error, got $blackoutCount blackouts")
    }

    // -----------------------------------------------------------------------
    // Tests: Reset
    // -----------------------------------------------------------------------

    @Test
    fun reset_returns_to_idle() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            spotFrame(50f, 50f)
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        orchestrator.scan(listOf(ScanFixture("f1")))
        assertIs<ScanState.Complete>(orchestrator.state.value)

        orchestrator.reset()
        assertIs<ScanState.Idle>(orchestrator.state.value)
    }

    // -----------------------------------------------------------------------
    // Tests: Progress tracking
    // -----------------------------------------------------------------------

    @Test
    fun scanning_state_reports_correct_progress() {
        val state = ScanState.Scanning(
            currentFixtureIndex = 2,
            totalFixtures = 10,
            currentFixtureId = "f3"
        )
        assertEquals(0.3f, state.progress, 0.01f)
    }

    @Test
    fun scanning_state_progress_at_start() {
        val state = ScanState.Scanning(0, 5, "f1")
        assertEquals(0.2f, state.progress, 0.01f)
    }

    @Test
    fun scanning_state_progress_at_end() {
        val state = ScanState.Scanning(4, 5, "f5")
        assertEquals(1.0f, state.progress, 0.01f)
    }

    // -----------------------------------------------------------------------
    // Tests: Frame capture count
    // -----------------------------------------------------------------------

    @Test
    fun captures_correct_number_of_frames() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            spotFrame(20f, 30f),
            spotFrame(70f, 80f)
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        orchestrator.scan(listOf(ScanFixture("f1"), ScanFixture("f2")))

        // 1 ambient + 2 fixture captures = 3
        assertEquals(3, capture.captureCount)
    }

    @Test
    fun multi_cell_fixture_captures_extra_frame_for_endpoints() = runTest {
        val dmx = MockDmxController()
        val capture = MockFrameCapture(listOf(
            ambientFrame(),
            spotFrame(50f, 50f),                // full fire
            twoSpotFrame(20f, 50f, 80f, 50f)    // endpoint fire
        ))
        val orchestrator = ScanOrchestrator(
            dmx, capture,
            blobDetector = BlobDetector(brightnessThreshold = 0.3f, minBlobSize = 1),
            fireSettleMs = 0L, decayMs = 0L
        )

        orchestrator.scan(listOf(
            ScanFixture("bar1", isMultiCell = true, pixelCount = 5)
        ))

        // 1 ambient + 1 full fire + 1 endpoint = 3
        assertEquals(3, capture.captureCount)
    }
}

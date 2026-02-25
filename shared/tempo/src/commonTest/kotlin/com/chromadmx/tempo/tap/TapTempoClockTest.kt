package com.chromadmx.tempo.tap

import com.chromadmx.tempo.clock.BeatClockUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TapTempoClockTest {

    // ---- Helpers ----

    /**
     * A controllable time source for deterministic tests.
     * Advances in nanoseconds.
     */
    private class FakeTimeSource(startNanos: Long = 0L) {
        var nowNanos: Long = startNanos
            private set

        fun advance(nanos: Long) {
            nowNanos += nanos
        }

        fun advanceMs(ms: Long) = advance(ms * 1_000_000L)

        fun advanceSec(sec: Double) = advance((sec * 1_000_000_000.0).toLong())

        val provider: () -> Long = { nowNanos }
    }

    private fun assertApprox(expected: Float, actual: Float, eps: Float = 1.0f, message: String = "") {
        assertTrue(
            abs(expected - actual) < eps,
            "Expected ~$expected but got $actual (eps=$eps)${if (message.isNotEmpty()) " — $message" else ""}"
        )
    }

    // ---- BPM Calculation Tests ----

    @Test
    fun fourTapsAt120BpmYields120Bpm() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // 120 BPM = 0.5 sec per beat
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        assertApprox(120f, clock.bpm.value, eps = 2f, message = "4 taps at 500ms intervals")
    }

    @Test
    fun fourTapsAt140BpmYields140Bpm() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // 140 BPM = 60/140 ~ 0.4286 sec per beat
        val interval = 60.0 / 140.0
        clock.tap()
        time.advanceSec(interval)
        clock.tap()
        time.advanceSec(interval)
        clock.tap()
        time.advanceSec(interval)
        clock.tap()

        assertApprox(140f, clock.bpm.value, eps = 2f, message = "4 taps at 140 BPM intervals")
    }

    @Test
    fun tapsAt90BpmYields90Bpm() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // 90 BPM = 60/90 ~ 0.6667 sec per beat
        val interval = 60.0 / 90.0
        clock.tap()
        time.advanceSec(interval)
        clock.tap()
        time.advanceSec(interval)
        clock.tap()
        time.advanceSec(interval)
        clock.tap()

        assertApprox(90f, clock.bpm.value, eps = 2f, message = "4 taps at 90 BPM intervals")
    }

    @Test
    fun outlierTapIsFilteredByMedian() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // 5 taps at 120 BPM, with one outlier
        val normalInterval = 0.5 // 120 BPM
        clock.tap()
        time.advanceSec(normalInterval)
        clock.tap()
        time.advanceSec(normalInterval)
        clock.tap()
        // Outlier: 2.5x the interval (would be filtered by > 2.0x median rule)
        time.advanceSec(normalInterval * 2.5)
        clock.tap()
        time.advanceSec(normalInterval)
        clock.tap()

        // After outlier filtering, the median should still be close to 120 BPM
        assertApprox(120f, clock.bpm.value, eps = 5f, message = "Outlier should be filtered")
    }

    @Test
    fun bpmClampedToMinMax() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Tap very slowly — would be ~20.7 BPM without clamping
        clock.tap()
        time.advanceSec(2.9) // Just under the 3-second timeout
        clock.tap()

        val bpm = clock.bpm.value
        assertTrue(bpm >= BeatClockUtils.MIN_BPM, "BPM $bpm should be >= ${BeatClockUtils.MIN_BPM}")
        assertTrue(bpm <= BeatClockUtils.MAX_BPM, "BPM $bpm should be <= ${BeatClockUtils.MAX_BPM}")
    }

    @Test
    fun tapTimeoutResetsHistory() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Establish 120 BPM
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        assertApprox(120f, clock.bpm.value, eps = 2f, message = "Before timeout")

        // Wait > 3 seconds (tap timeout)
        time.advanceSec(4.0)

        // New taps at 100 BPM — history should be reset so old 120 BPM taps don't interfere
        val interval100 = 60.0 / 100.0
        clock.tap() // This clears old history due to timeout
        time.advanceSec(interval100)
        clock.tap()
        time.advanceSec(interval100)
        clock.tap()
        time.advanceSec(interval100)
        clock.tap()

        assertApprox(100f, clock.bpm.value, eps = 2f, message = "After timeout + 4 taps at 100 BPM")
    }

    // ---- Phase Tests ----

    @Test
    fun phaseIsZeroOnTap() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        clock.tap()

        // Immediately after a tap, beat phase should be 0 (downbeat)
        assertApprox(0f, clock.beatPhase.value, eps = 0.01f, message = "Phase should be 0 on tap")
    }

    @Test
    fun phaseAdvancesToMidpointHalfwayThroughBeat() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Establish 120 BPM (0.5 sec per beat)
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        // Advance half a beat (0.25 sec at 120 BPM)
        time.advanceSec(0.25)
        clock.updatePhase()

        assertApprox(0.5f, clock.beatPhase.value, eps = 0.05f, message = "Phase should be ~0.5 halfway through beat")
    }

    @Test
    fun phaseWrapsAtBeatBoundary() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Establish 120 BPM
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        // Advance 1.1 beats (0.55 sec at 120 BPM) — should wrap
        time.advanceSec(0.55)
        clock.updatePhase()

        val phase = clock.beatPhase.value
        assertTrue(phase < 0.2f, "Phase $phase should have wrapped (expected ~0.1)")
    }

    @Test
    fun barPhaseAdvancesOverFourBeats() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Establish 120 BPM
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        // Advance 2 beats (1.0 sec at 120 BPM) — barPhase should be 0.5
        time.advanceSec(1.0)
        clock.updatePhase()

        assertApprox(0.5f, clock.barPhase.value, eps = 0.05f, message = "Bar phase should be ~0.5 after 2 of 4 beats")
    }

    @Test
    fun barPhaseWrapsAfterFourBeats() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Establish 120 BPM (bar = 4 beats = 2.0 sec)
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        // Advance 4.5 beats (2.25 sec) — barPhase should be ~0.125 (0.5/4)
        time.advanceSec(2.25)
        clock.updatePhase()

        val barPhase = clock.barPhase.value
        assertTrue(barPhase < 0.2f, "Bar phase $barPhase should have wrapped after 4 beats")
    }

    // ---- Phase Nudge Tests ----

    @Test
    fun nudgePhaseShiftsPhaseForward() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Establish 120 BPM and start at downbeat
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        // Phase should be 0 right after tap
        assertApprox(0f, clock.beatPhase.value, eps = 0.01f)

        // Nudge forward by 1/4 beat (0.125 sec at 120 BPM)
        clock.nudgePhase(0.125)

        assertApprox(0.25f, clock.beatPhase.value, eps = 0.05f, message = "Phase should be ~0.25 after nudge")
    }

    // ---- Lifecycle Tests ----

    @Test
    fun startsNotRunning() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        assertFalse(clock.isRunning.value, "Clock should not be running initially")
    }

    @Test
    fun tapAutoStarts() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        clock.tap()
        assertTrue(clock.isRunning.value, "Clock should auto-start on first tap")
    }

    @Test
    fun stopFreezesPhase() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        // Advance to a known phase
        time.advanceSec(0.25) // half a beat at 120 BPM
        clock.updatePhase()
        val phaseBeforeStop = clock.beatPhase.value

        clock.stop()

        // Phase should stay frozen
        assertApprox(phaseBeforeStop, clock.beatPhase.value, eps = 0.01f, message = "Phase should be frozen after stop")
    }

    @Test
    fun resetClearsState() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        clock.reset()

        assertFalse(clock.isRunning.value, "Should not be running after reset")
        assertEquals(BeatClockUtils.DEFAULT_BPM, clock.bpm.value, "BPM should be default after reset")
        assertEquals(0f, clock.beatPhase.value, "Beat phase should be 0 after reset")
        assertEquals(0f, clock.barPhase.value, "Bar phase should be 0 after reset")
    }

    // ---- BeatState Snapshot Tests ----

    @Test
    fun beatStateReflectsCurrentValues() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        // Establish 120 BPM
        clock.tap()
        time.advanceSec(0.5)
        clock.tap()

        val state = clock.beatState.value
        assertApprox(120f, state.bpm, eps = 2f, message = "BeatState.bpm")
        assertApprox(clock.beatPhase.value, state.beatPhase, eps = 0.01f, message = "BeatState.beatPhase")
        assertApprox(clock.barPhase.value, state.barPhase, eps = 0.01f, message = "BeatState.barPhase")
    }

    @Test
    fun defaultBpmIs120() = runTest {
        val time = FakeTimeSource()
        val clock = TapTempoClock(
            scope = backgroundScope,
            timeSource = time.provider,
            updateIntervalMs = 100
        )

        assertEquals(120f, clock.bpm.value, "Default BPM should be 120")
    }

    // ---- BeatClockUtils Tests ----

    @Test
    fun medianOfOddList() {
        val result = BeatClockUtils.median(listOf(3.0, 1.0, 2.0))
        assertEquals(2.0, result)
    }

    @Test
    fun medianOfEvenList() {
        val result = BeatClockUtils.median(listOf(1.0, 2.0, 3.0, 4.0))
        assertEquals(2.5, result)
    }

    @Test
    fun medianOfEmptyList() {
        val result = BeatClockUtils.median(emptyList())
        assertEquals(0.0, result)
    }

    @Test
    fun medianOfSingleValue() {
        val result = BeatClockUtils.median(listOf(42.0))
        assertEquals(42.0, result)
    }

    @Test
    fun beatDurationAt120Bpm() {
        val duration = BeatClockUtils.beatDurationSec(120f)
        assertTrue(abs(duration - 0.5) < 0.001, "Beat duration at 120 BPM should be 0.5 sec")
    }

    @Test
    fun barDurationAt120Bpm() {
        val duration = BeatClockUtils.barDurationSec(120f)
        assertTrue(abs(duration - 2.0) < 0.001, "Bar duration at 120 BPM should be 2.0 sec")
    }

    @Test
    fun computeBeatPhaseAtHalfBeat() {
        // At 120 BPM, half a beat = 0.25 sec
        val phase = BeatClockUtils.computeBeatPhase(0.25, 120f)
        assertTrue(abs(phase - 0.5f) < 0.01f, "Phase should be ~0.5 at half a beat")
    }

    @Test
    fun computeBarPhaseAtTwoBeats() {
        // At 120 BPM, 2 beats = 1.0 sec -> barPhase = 0.5
        val phase = BeatClockUtils.computeBarPhase(1.0, 120f)
        assertTrue(abs(phase - 0.5f) < 0.01f, "Bar phase should be ~0.5 at 2 of 4 beats")
    }

    @Test
    fun clampBpmRespectsRange() {
        assertEquals(BeatClockUtils.MIN_BPM, BeatClockUtils.clampBpm(1f))
        assertEquals(BeatClockUtils.MAX_BPM, BeatClockUtils.clampBpm(999f))
        assertEquals(120f, BeatClockUtils.clampBpm(120f))
    }
}

package com.chromadmx.tempo.link

import com.chromadmx.core.model.BeatState
import com.chromadmx.tempo.clock.BeatClockUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [AbletonLinkClock].
 *
 * These tests verify the Kotlin-level logic of the clock: state management,
 * polling behavior, link state transitions, and BeatClock contract compliance.
 * They use a [FakeLinkSession] to simulate the native Link SDK responses.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AbletonLinkClockTest {

    // ---- Test doubles ----

    private class FakeLinkSession(
        initialBpm: Double = 120.0,
        initialPeerCount: Int = 0
    ) : LinkSessionApi {
        private var _enabled = false
        private var _peerCount = initialPeerCount
        private var _bpm = initialBpm
        private var _beatPhase = 0.0
        private var _barPhase = 0.0

        override fun enable() { _enabled = true }
        override fun disable() { _enabled = false }
        override val isEnabled: Boolean get() = _enabled
        override val peerCount: Int get() = _peerCount
        override val bpm: Double get() = _bpm
        override val beatPhase: Double get() = _beatPhase
        override val barPhase: Double get() = _barPhase
        override fun requestBpm(bpm: Double) { _bpm = bpm }

        fun setPeerCount(count: Int) { _peerCount = count }
        fun setBpm(value: Double) { _bpm = value }
        fun setBeatPhase(value: Double) { _beatPhase = value }
        fun setBarPhase(value: Double) { _barPhase = value }
    }

    private class FakeTimeSource(startNanos: Long = 0L) {
        var nowNanos: Long = startNanos
            private set

        fun advance(nanos: Long) { nowNanos += nanos }
        fun advanceMs(ms: Long) = advance(ms * 1_000_000L)
        fun advanceSec(sec: Double) = advance((sec * 1_000_000_000.0).toLong())

        val provider: () -> Long = { nowNanos }
    }

    private fun assertApprox(
        expected: Float,
        actual: Float,
        eps: Float = 0.01f,
        message: String = ""
    ) {
        assertTrue(
            abs(expected - actual) < eps,
            "Expected ~$expected but got $actual (eps=$eps)" +
                if (message.isNotEmpty()) " - $message" else ""
        )
    }

    // ======================================================================
    // BeatClock contract compliance
    // ======================================================================

    @Test
    fun implementsBeatClockInterface() = runTest {
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = FakeTimeSource().provider
        )

        // Verify all BeatClock StateFlows are accessible
        assertEquals(BeatClockUtils.DEFAULT_BPM, clock.bpm.value)
        assertEquals(0f, clock.beatPhase.value)
        assertEquals(0f, clock.barPhase.value)
        assertFalse(clock.isRunning.value)
        assertEquals(BeatState.IDLE, clock.beatState.value)
    }

    @Test
    fun startsNotRunning() = runTest {
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = FakeTimeSource().provider
        )

        assertFalse(clock.isRunning.value)
        assertEquals(AbletonLinkClock.LinkState.DISABLED, clock.linkState.value)
    }

    @Test
    fun startSetsRunningTrue() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        assertTrue(clock.isRunning.value)
        clock.stop()
    }

    @Test
    fun stopSetsRunningFalse() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.stop()
        assertFalse(clock.isRunning.value)
    }

    @Test
    fun startIsIdempotent() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.start() // Second call should be a no-op
        assertTrue(clock.isRunning.value)
        clock.stop()
    }

    @Test
    fun stopIsIdempotent() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.stop()
        clock.stop() // Second call should be a no-op
        assertFalse(clock.isRunning.value)
    }

    // ======================================================================
    // Session enable/disable lifecycle
    // ======================================================================

    @Test
    fun startEnablesSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        assertFalse(session.isEnabled)
        clock.start()
        assertTrue(session.isEnabled)
        clock.stop()
    }

    @Test
    fun stopDisablesSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        assertTrue(session.isEnabled)
        clock.stop()
        assertFalse(session.isEnabled)
    }

    // ======================================================================
    // Polling — BPM, phase, peer count
    // ======================================================================

    @Test
    fun pollReadsBpmFromSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 138.0, initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertApprox(138f, clock.bpm.value, eps = 1f)
        clock.stop()
    }

    @Test
    fun pollReadsBeatPhaseFromSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        session.setBeatPhase(0.42)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertApprox(0.42f, clock.beatPhase.value)
        clock.stop()
    }

    @Test
    fun pollReadsBarPhaseFromSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        session.setBarPhase(0.87)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertApprox(0.87f, clock.barPhase.value)
        clock.stop()
    }

    @Test
    fun pollReadsPeerCountFromSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 3)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertEquals(3, clock.peerCount.value)
        clock.stop()
    }

    @Test
    fun pollUpdatesWhenSessionValuesChange() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 120.0, initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()
        assertApprox(120f, clock.bpm.value, eps = 1f)

        // Change session values
        session.setBpm(145.0)
        session.setBeatPhase(0.6)
        session.setBarPhase(0.3)
        session.setPeerCount(5)
        time.advanceMs(16)
        clock.pollLinkSession()

        assertApprox(145f, clock.bpm.value, eps = 1f)
        assertApprox(0.6f, clock.beatPhase.value)
        assertApprox(0.3f, clock.barPhase.value)
        assertEquals(5, clock.peerCount.value)

        clock.stop()
    }

    @Test
    fun bpmClampedToMin() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 5.0) // Below MIN_BPM
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertEquals(BeatClockUtils.MIN_BPM, clock.bpm.value)
        clock.stop()
    }

    @Test
    fun bpmClampedToMax() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 999.0) // Above MAX_BPM
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertEquals(BeatClockUtils.MAX_BPM, clock.bpm.value)
        clock.stop()
    }

    @Test
    fun phaseClampedToValidRange() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        // Set out-of-range phases
        session.setBeatPhase(1.5)
        session.setBarPhase(-0.1)

        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertTrue(clock.beatPhase.value <= 1f, "Beat phase should be clamped to <= 1.0")
        assertTrue(clock.barPhase.value >= 0f, "Bar phase should be clamped to >= 0.0")
        clock.stop()
    }

    // ======================================================================
    // Link state transitions
    // ======================================================================

    @Test
    fun disabledBeforeStart() = runTest {
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = FakeTimeSource().provider
        )

        assertEquals(AbletonLinkClock.LinkState.DISABLED, clock.linkState.value)
    }

    @Test
    fun searchingOnStartWithNoPeers() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 0)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            noLinkTimeoutMs = 5000,
            timeSource = time.provider
        )

        clock.start()
        // Just started, within timeout
        time.advanceMs(100)
        clock.pollLinkSession()

        assertEquals(AbletonLinkClock.LinkState.SEARCHING, clock.linkState.value)
        clock.stop()
    }

    @Test
    fun connectedWhenPeersPresent() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 2)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertEquals(AbletonLinkClock.LinkState.CONNECTED, clock.linkState.value)
        clock.stop()
    }

    @Test
    fun noLinkAfterTimeoutWithoutEverSeeingPeers() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 0)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            noLinkTimeoutMs = 3000,
            timeSource = time.provider
        )

        clock.start()
        time.advanceMs(4000) // Past timeout
        clock.pollLinkSession()

        assertEquals(AbletonLinkClock.LinkState.NO_LINK, clock.linkState.value)
        clock.stop()
    }

    @Test
    fun noLinkAfterTimeoutWhenPeersDisappear() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            noLinkTimeoutMs = 3000,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.CONNECTED, clock.linkState.value)

        // Peers disappear
        session.setPeerCount(0)
        time.advanceMs(1000) // Within timeout
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.SEARCHING, clock.linkState.value)

        time.advanceMs(3000) // Past timeout
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.NO_LINK, clock.linkState.value)

        clock.stop()
    }

    @Test
    fun reconnectsFromNoLinkWhenPeersAppear() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 0)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            noLinkTimeoutMs = 3000,
            timeSource = time.provider
        )

        clock.start()
        time.advanceMs(4000) // -> NO_LINK
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.NO_LINK, clock.linkState.value)

        // Peer appears
        session.setPeerCount(1)
        time.advanceMs(100)
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.CONNECTED, clock.linkState.value)

        clock.stop()
    }

    @Test
    fun disabledAfterStop() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.CONNECTED, clock.linkState.value)

        clock.stop()
        assertEquals(AbletonLinkClock.LinkState.DISABLED, clock.linkState.value)
    }

    // ======================================================================
    // BeatState snapshot
    // ======================================================================

    @Test
    fun beatStateReflectsPolledValues() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 132.0, initialPeerCount = 1)
        session.setBeatPhase(0.3)
        session.setBarPhase(0.7)

        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        time.advanceSec(1.5)
        clock.pollLinkSession()

        val state = clock.beatState.value
        assertApprox(132f, state.bpm, eps = 1f, message = "BeatState.bpm")
        assertApprox(0.3f, state.beatPhase, message = "BeatState.beatPhase")
        assertApprox(0.7f, state.barPhase, message = "BeatState.barPhase")
        assertApprox(1.5f, state.elapsed, eps = 0.1f, message = "BeatState.elapsed")

        clock.stop()
    }

    @Test
    fun elapsedTimeAccumulates() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()

        time.advanceSec(1.0)
        clock.pollLinkSession()
        assertApprox(1.0f, clock.beatState.value.elapsed, eps = 0.1f)

        time.advanceSec(2.0)
        clock.pollLinkSession()
        assertApprox(3.0f, clock.beatState.value.elapsed, eps = 0.1f)

        clock.stop()
    }

    // ======================================================================
    // Edge cases
    // ======================================================================

    @Test
    fun pollBeforeStartDoesNotCrash() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        // Polling before start should not throw
        clock.pollLinkSession()
    }

    @Test
    fun peerCountUpdatesReflectImmediately() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 0)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()
        assertEquals(0, clock.peerCount.value)

        session.setPeerCount(1)
        clock.pollLinkSession()
        assertEquals(1, clock.peerCount.value)

        session.setPeerCount(10)
        clock.pollLinkSession()
        assertEquals(10, clock.peerCount.value)

        session.setPeerCount(0)
        clock.pollLinkSession()
        assertEquals(0, clock.peerCount.value)

        clock.stop()
    }

    @Test
    fun rapidStartStopCycles() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            timeSource = time.provider
        )

        // Rapid start/stop should not throw or leave inconsistent state
        repeat(5) {
            clock.start()
            assertTrue(clock.isRunning.value)
            assertTrue(session.isEnabled)
            time.advanceMs(10)
            clock.stop()
            assertFalse(clock.isRunning.value)
            assertFalse(session.isEnabled)
        }
    }
}

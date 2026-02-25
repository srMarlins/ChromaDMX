package com.chromadmx.tempo.link

import com.chromadmx.core.model.BeatState
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.clock.BeatClockUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LinkFallbackClockTest {

    // ---- Test doubles ----

    /**
     * A fake [LinkSessionApi] that allows tests to control peerCount, BPM, and
     * phase values without requiring native Link SDK integration.
     */
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

    /**
     * A fake [BeatClock] (stands in for TapTempoClock) with controllable values.
     */
    private class FakeTapClock(
        initialBpm: Float = BeatClockUtils.DEFAULT_BPM
    ) : BeatClock {
        private val _bpm = MutableStateFlow(initialBpm)
        override val bpm: StateFlow<Float> = _bpm.asStateFlow()

        private val _beatPhase = MutableStateFlow(0f)
        override val beatPhase: StateFlow<Float> = _beatPhase.asStateFlow()

        private val _barPhase = MutableStateFlow(0f)
        override val barPhase: StateFlow<Float> = _barPhase.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _beatState = MutableStateFlow(BeatState.IDLE)
        override val beatState: StateFlow<BeatState> = _beatState.asStateFlow()

        override fun start() { _isRunning.value = true }
        override fun stop() { _isRunning.value = false }

        fun setBpm(value: Float) {
            _bpm.value = value
            _beatState.value = _beatState.value.copy(bpm = value)
        }

        fun setBeatPhase(value: Float) {
            _beatPhase.value = value
            _beatState.value = _beatState.value.copy(beatPhase = value)
        }

        fun setBarPhase(value: Float) {
            _barPhase.value = value
            _beatState.value = _beatState.value.copy(barPhase = value)
        }
    }

    /**
     * A controllable time source for deterministic tests (nanoseconds).
     */
    private class FakeTimeSource(startNanos: Long = 0L) {
        var nowNanos: Long = startNanos
            private set

        fun advance(nanos: Long) { nowNanos += nanos }
        fun advanceMs(ms: Long) = advance(ms * 1_000_000L)
        fun advanceSec(sec: Double) = advance((sec * 1_000_000_000.0).toLong())

        val provider: () -> Long = { nowNanos }
    }

    private fun assertApprox(expected: Float, actual: Float, eps: Float = 0.01f, message: String = "") {
        assertTrue(
            abs(expected - actual) < eps,
            "Expected ~$expected but got $actual (eps=$eps)${if (message.isNotEmpty()) " - $message" else ""}"
        )
    }

    // ======================================================================
    // AbletonLinkClock tests
    // ======================================================================

    @Test
    fun linkClockReadsPhaseFromSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 128.0, initialPeerCount = 2)
        session.setBeatPhase(0.75)
        session.setBarPhase(0.25)

        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertApprox(128f, clock.bpm.value, eps = 1f, message = "BPM from session")
        assertApprox(0.75f, clock.beatPhase.value, eps = 0.01f, message = "Beat phase from session")
        assertApprox(0.25f, clock.barPhase.value, eps = 0.01f, message = "Bar phase from session")
        assertEquals(2, clock.peerCount.value)

        clock.stop()
    }

    @Test
    fun linkClockReportsConnectedWhenPeersPresent() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertEquals(AbletonLinkClock.LinkState.CONNECTED, clock.linkState.value)
        clock.stop()
    }

    @Test
    fun linkClockReportsSearchingInitially() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 0)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            noLinkTimeoutMs = 5000,
            timeSource = time.provider
        )

        clock.start()
        // Within timeout, no peers -> SEARCHING
        time.advanceMs(1000)
        clock.pollLinkSession()

        assertEquals(AbletonLinkClock.LinkState.SEARCHING, clock.linkState.value)
        clock.stop()
    }

    @Test
    fun linkClockReportsNoLinkAfterTimeout() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 0)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            noLinkTimeoutMs = 5000,
            timeSource = time.provider
        )

        clock.start()
        // Advance past the 5-second timeout
        time.advanceMs(6000)
        clock.pollLinkSession()

        assertEquals(AbletonLinkClock.LinkState.NO_LINK, clock.linkState.value)
        clock.stop()
    }

    @Test
    fun linkClockTransitionsFromNoLinkToConnected() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 0)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            noLinkTimeoutMs = 5000,
            timeSource = time.provider
        )

        clock.start()

        // Go past timeout -> NO_LINK
        time.advanceMs(6000)
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.NO_LINK, clock.linkState.value)

        // Peer appears
        session.setPeerCount(1)
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.CONNECTED, clock.linkState.value)

        clock.stop()
    }

    @Test
    fun linkClockDisabledAfterStop() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )

        clock.start()
        clock.stop()
        assertEquals(AbletonLinkClock.LinkState.DISABLED, clock.linkState.value)
    }

    @Test
    fun linkClockClampsBpm() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 500.0) // Above MAX_BPM
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()

        assertEquals(BeatClockUtils.MAX_BPM, clock.bpm.value)
        clock.stop()
    }

    @Test
    fun linkClockBeatStateIncludesElapsed() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 120.0, initialPeerCount = 1)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )

        clock.start()
        time.advanceSec(2.5)
        clock.pollLinkSession()

        assertApprox(2.5f, clock.beatState.value.elapsed, eps = 0.1f, message = "Elapsed time in BeatState")
        clock.stop()
    }

    @Test
    fun linkClockEnablesSessionOnStart() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession()
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )

        assertFalse(session.isEnabled, "Session should not be enabled before start")
        clock.start()
        assertTrue(session.isEnabled, "Session should be enabled after start")
        clock.stop()
        assertFalse(session.isEnabled, "Session should be disabled after stop")
    }

    @Test
    fun linkClockNoLinkAfterPeersDisappear() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 2)
        val clock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            noLinkTimeoutMs = 5000,
            timeSource = time.provider
        )

        clock.start()
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.CONNECTED, clock.linkState.value)

        // Peers disappear
        session.setPeerCount(0)
        time.advanceMs(2000)
        clock.pollLinkSession()
        // Within timeout, should still be SEARCHING (had seen peers before)
        assertEquals(AbletonLinkClock.LinkState.SEARCHING, clock.linkState.value)

        // Past timeout
        time.advanceMs(4000)
        clock.pollLinkSession()
        assertEquals(AbletonLinkClock.LinkState.NO_LINK, clock.linkState.value)

        clock.stop()
    }

    // ======================================================================
    // LinkFallbackClock tests
    // ======================================================================

    @Test
    fun fallbackUsesLinkWhenPeersPresent() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 140.0, initialPeerCount = 2)
        session.setBeatPhase(0.6)

        val linkClock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )
        val tapClock = FakeTapClock(initialBpm = 90f)

        val fallback = LinkFallbackClock(
            scope = backgroundScope,
            linkClock = linkClock,
            tapClock = tapClock,
            pollIntervalMs = 100
        )

        fallback.start()
        // Force a poll of the link clock first, then the fallback
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()

        assertEquals(SyncSource.LINK, fallback.syncSource.value)
        assertApprox(140f, fallback.bpm.value, eps = 1f, message = "BPM should come from Link")
        assertApprox(0.6f, fallback.beatPhase.value, eps = 0.01f, message = "Phase should come from Link")

        fallback.stop()
    }

    @Test
    fun fallbackSwitchesToTapWhenNoPeers() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 140.0, initialPeerCount = 0)

        val linkClock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            noLinkTimeoutMs = 5000,
            timeSource = time.provider
        )
        val tapClock = FakeTapClock(initialBpm = 95f)
        tapClock.setBeatPhase(0.3f)

        val fallback = LinkFallbackClock(
            scope = backgroundScope,
            linkClock = linkClock,
            tapClock = tapClock,
            pollIntervalMs = 100
        )

        fallback.start()

        // Advance past no-link timeout
        time.advanceMs(6000)
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()

        assertEquals(SyncSource.TAP, fallback.syncSource.value)
        assertApprox(95f, fallback.bpm.value, eps = 1f, message = "BPM should come from tap")
        assertApprox(0.3f, fallback.beatPhase.value, eps = 0.01f, message = "Phase should come from tap")

        fallback.stop()
    }

    @Test
    fun fallbackSwitchesBackToLinkWhenPeersAppear() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 130.0, initialPeerCount = 0)

        val linkClock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            noLinkTimeoutMs = 5000,
            timeSource = time.provider
        )
        val tapClock = FakeTapClock(initialBpm = 100f)

        val fallback = LinkFallbackClock(
            scope = backgroundScope,
            linkClock = linkClock,
            tapClock = tapClock,
            pollIntervalMs = 100
        )

        fallback.start()

        // No peers for > timeout -> TAP
        time.advanceMs(6000)
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()
        assertEquals(SyncSource.TAP, fallback.syncSource.value)

        // Peers appear -> LINK
        session.setPeerCount(1)
        session.setBpm(130.0)
        session.setBeatPhase(0.8)
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()

        assertEquals(SyncSource.LINK, fallback.syncSource.value)
        assertApprox(130f, fallback.bpm.value, eps = 1f, message = "BPM should switch back to Link")
        assertApprox(0.8f, fallback.beatPhase.value, eps = 0.01f, message = "Phase should switch back to Link")

        fallback.stop()
    }

    @Test
    fun fallbackBpmPropagatesFromLinkSession() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialBpm = 120.0, initialPeerCount = 1)

        val linkClock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )
        val tapClock = FakeTapClock()

        val fallback = LinkFallbackClock(
            scope = backgroundScope,
            linkClock = linkClock,
            tapClock = tapClock,
            pollIntervalMs = 100
        )

        fallback.start()
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()
        assertApprox(120f, fallback.bpm.value, eps = 1f)

        // Change BPM in session
        session.setBpm(140.0)
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()
        assertApprox(140f, fallback.bpm.value, eps = 1f, message = "BPM should update when session changes")

        fallback.stop()
    }

    @Test
    fun fallbackIsNoneWhenStopped() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        val linkClock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )
        val tapClock = FakeTapClock()

        val fallback = LinkFallbackClock(
            scope = backgroundScope,
            linkClock = linkClock,
            tapClock = tapClock,
            pollIntervalMs = 100
        )

        assertEquals(SyncSource.NONE, fallback.syncSource.value, "Should be NONE before start")

        fallback.start()
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()
        assertEquals(SyncSource.LINK, fallback.syncSource.value)

        fallback.stop()
        assertEquals(SyncSource.NONE, fallback.syncSource.value, "Should be NONE after stop")
    }

    @Test
    fun fallbackPeerCountPassesThrough() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 3)
        val linkClock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )
        val tapClock = FakeTapClock()

        val fallback = LinkFallbackClock(
            scope = backgroundScope,
            linkClock = linkClock,
            tapClock = tapClock,
            pollIntervalMs = 100
        )

        fallback.start()
        linkClock.pollLinkSession()

        assertEquals(3, fallback.peerCount.value, "Peer count should pass through from link clock")

        session.setPeerCount(5)
        linkClock.pollLinkSession()
        assertEquals(5, fallback.peerCount.value)

        fallback.stop()
    }

    @Test
    fun fallbackBarPhaseFollowsActiveSource() = runTest {
        val time = FakeTimeSource()
        val session = FakeLinkSession(initialPeerCount = 1)
        session.setBarPhase(0.7)

        val linkClock = AbletonLinkClock(
            scope = backgroundScope,
            linkSession = session,
            updateIntervalMs = 100,
            timeSource = time.provider
        )
        val tapClock = FakeTapClock()
        tapClock.setBarPhase(0.2f)

        val fallback = LinkFallbackClock(
            scope = backgroundScope,
            linkClock = linkClock,
            tapClock = tapClock,
            pollIntervalMs = 100
        )

        fallback.start()
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()

        // Link active, bar phase from Link
        assertApprox(0.7f, fallback.barPhase.value, eps = 0.01f, message = "Bar phase from Link")

        // Drop peers, wait for timeout
        session.setPeerCount(0)
        time.advanceMs(6000)
        linkClock.pollLinkSession()
        fallback.updateFromActiveSource()

        // Now tap is active, bar phase from tap
        assertApprox(0.2f, fallback.barPhase.value, eps = 0.01f, message = "Bar phase from tap after fallback")

        fallback.stop()
    }

    // ======================================================================
    // LinkSessionApi fake validation tests
    // ======================================================================

    @Test
    fun fakeLinkSessionDefaults() {
        val session = FakeLinkSession()
        assertEquals(120.0, session.bpm)
        assertEquals(0.0, session.beatPhase)
        assertEquals(0.0, session.barPhase)
        assertEquals(0, session.peerCount)
        assertFalse(session.isEnabled)
    }

    @Test
    fun fakeLinkSessionEnableDisable() {
        val session = FakeLinkSession()
        session.enable()
        assertTrue(session.isEnabled)
        session.disable()
        assertFalse(session.isEnabled)
    }

    @Test
    fun fakeLinkSessionRequestBpm() {
        val session = FakeLinkSession(initialBpm = 120.0)
        session.requestBpm(145.0)
        assertEquals(145.0, session.bpm)
    }
}

package com.chromadmx.tempo.link

import com.chromadmx.core.model.BeatState
import com.chromadmx.tempo.clock.BeatClock
import com.chromadmx.tempo.clock.BeatClockUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * Ableton Link-based beat clock that synchronizes BPM and phase across
 * devices on the same network.
 *
 * This is a shared (common) implementation that delegates to [LinkSession]
 * for the actual native Link SDK communication. The class polls the session
 * at ~60fps and publishes updates via StateFlows.
 *
 * ## Automatic "no link" detection
 *
 * When [peerCount] remains 0 for longer than [noLinkTimeoutMs] (default 5 seconds),
 * this clock sets [linkState] to [LinkState.NO_LINK], signaling the UI to show
 * a "no peers" indicator and potentially fall back to tap tempo.
 *
 * ## Usage
 *
 * ```kotlin
 * val linkClock = AbletonLinkClock(scope, linkSession)
 * linkClock.start() // enables LinkSession and starts polling
 * // observe linkClock.bpm, linkClock.beatPhase, etc.
 * linkClock.stop()  // disables LinkSession
 * ```
 *
 * @param scope           Coroutine scope for the phase polling loop.
 * @param linkSession     Platform-specific Link session (implements [LinkSessionApi]).
 * @param updateIntervalMs  How often to poll Link and refresh StateFlows (default 16ms).
 * @param noLinkTimeoutMs  Duration in ms with 0 peers before emitting [LinkState.NO_LINK].
 * @param timeSource      Injectable time source for testing (returns nanoseconds).
 */
class AbletonLinkClock(
    private val scope: CoroutineScope,
    private val linkSession: LinkSessionApi,
    private val updateIntervalMs: Long = 16L,
    private val noLinkTimeoutMs: Long = 5_000L,
    private val timeSource: () -> Long = defaultTimeSource
) : BeatClock {

    /** Represents the state of the Link connection. */
    enum class LinkState {
        /** Link session is active and peers are connected. */
        CONNECTED,

        /** Link session is active but no peers are present (within grace period). */
        SEARCHING,

        /** Link session is active but no peers detected for longer than [noLinkTimeoutMs]. */
        NO_LINK,

        /** Link session is disabled / clock is stopped. */
        DISABLED
    }

    // ---- Internal state ----

    private var pollJob: Job? = null
    private var startTimeNanos: Long = 0L

    /** Nanotime when peers were last seen (peerCount > 0). */
    private var lastPeerSeenNanos: Long = 0L

    /** Whether we have ever seen a peer since the session was enabled. */
    private var hasSeenPeer: Boolean = false

    // ---- StateFlows (BeatClock) ----

    private val _bpm = MutableStateFlow(BeatClockUtils.DEFAULT_BPM)
    override val bpm: StateFlow<Float> = _bpm.asStateFlow()

    private val _beatPhase = MutableStateFlow(0f)
    override val beatPhase: StateFlow<Float> = _beatPhase.asStateFlow()

    private val _barPhase = MutableStateFlow(0f)
    override val barPhase: StateFlow<Float> = _barPhase.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _beatState = MutableStateFlow(BeatState.IDLE)
    override val beatState: StateFlow<BeatState> = _beatState.asStateFlow()

    // ---- Additional StateFlows for Link-specific info ----

    private val _peerCount = MutableStateFlow(0)

    /** Number of connected Link peers. UI can use this for color-coding (#26). */
    val peerCount: StateFlow<Int> = _peerCount.asStateFlow()

    private val _linkState = MutableStateFlow(LinkState.DISABLED)

    /** Current Link connection state. */
    val linkState: StateFlow<LinkState> = _linkState.asStateFlow()

    // ---- BeatClock lifecycle ----

    /**
     * Enable the Link session and begin polling for tempo and phase updates.
     */
    override fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        startTimeNanos = timeSource()
        lastPeerSeenNanos = startTimeNanos
        hasSeenPeer = false

        linkSession.enable()
        _linkState.value = LinkState.SEARCHING

        startPollLoop()
    }

    /**
     * Disable the Link session and stop polling.
     */
    override fun stop() {
        if (!_isRunning.value) return
        _isRunning.value = false
        pollJob?.cancel()
        pollJob = null

        linkSession.disable()
        _linkState.value = LinkState.DISABLED
    }

    // ---- Internal ----

    private fun startPollLoop() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                pollLinkSession()
                delay(updateIntervalMs)
            }
        }
    }

    /**
     * Read the current state from [linkSession] and update all StateFlows.
     * Visible for testing.
     */
    internal fun pollLinkSession() {
        val now = timeSource()
        val currentPeers = linkSession.peerCount
        _peerCount.value = currentPeers

        // Track peer visibility for no-link detection
        if (currentPeers > 0) {
            lastPeerSeenNanos = now
            hasSeenPeer = true
        }

        // Update link state.
        // Note: lastPeerSeenNanos is initialized to startTimeNanos, so when
        // !hasSeenPeer the expression (now - lastPeerSeenNanos) equals
        // (now - startTimeNanos). Both NO_LINK branches therefore reduce to
        // the same check on nanosSinceLastPeer.
        _linkState.value = when {
            !_isRunning.value -> LinkState.DISABLED
            currentPeers > 0 -> LinkState.CONNECTED
            nanosSinceLastPeer(now) > noLinkTimeoutMs * NANOS_PER_MS -> LinkState.NO_LINK
            else -> LinkState.SEARCHING
        }

        // Read phase and tempo from Link
        val linkBpm = linkSession.bpm.toFloat()
        val clampedBpm = BeatClockUtils.clampBpm(linkBpm)
        val linkBeatPhase = linkSession.beatPhase.toFloat().coerceIn(0f, 1f)
        val linkBarPhase = linkSession.barPhase.toFloat().coerceIn(0f, 1f)

        _bpm.value = clampedBpm
        _beatPhase.value = linkBeatPhase
        _barPhase.value = linkBarPhase

        // Elapsed time since start
        val elapsedSec = (now - startTimeNanos).toDouble() / NANOS_PER_SEC

        _beatState.value = BeatState(
            bpm = clampedBpm,
            beatPhase = linkBeatPhase,
            barPhase = linkBarPhase,
            elapsed = elapsedSec.toFloat()
        )
    }

    private fun nanosSinceLastPeer(now: Long): Long = now - lastPeerSeenNanos

    companion object {
        internal const val NANOS_PER_SEC: Double = 1_000_000_000.0
        internal const val NANOS_PER_MS: Long = 1_000_000L

        private val monotonicStart = TimeSource.Monotonic.markNow()
        private val defaultTimeSource: () -> Long = {
            monotonicStart.elapsedNow().inWholeNanoseconds
        }
    }
}

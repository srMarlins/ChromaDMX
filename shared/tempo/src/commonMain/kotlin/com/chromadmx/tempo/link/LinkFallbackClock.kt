package com.chromadmx.tempo.link

import com.chromadmx.core.model.BeatState
import com.chromadmx.tempo.clock.BeatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A composite [BeatClock] that wraps both an [AbletonLinkClock] and a
 * tap-tempo [BeatClock], automatically switching between them based on
 * Link peer availability.
 *
 * ## Behavior
 *
 * - When Link has connected peers ([AbletonLinkClock.linkState] == CONNECTED),
 *   this clock forwards all StateFlows from [linkClock].
 * - When Link has no peers for longer than its timeout (NO_LINK state),
 *   this clock switches to [tapClock] and exposes its StateFlows instead.
 * - The active source is reported via [syncSource].
 *
 * ## Switching rules
 *
 * - **LINK -> TAP**: When linkState transitions to NO_LINK and tapClock is running.
 * - **TAP -> LINK**: When linkState transitions to CONNECTED.
 * - **NONE**: When this clock is stopped.
 *
 * ## Thread safety
 *
 * This class is designed to be driven from a single coroutine scope. All
 * StateFlow updates happen on the polling coroutine.
 *
 * @param scope     Coroutine scope for the state-monitoring loop.
 * @param linkClock The Ableton Link clock instance.
 * @param tapClock  The tap-tempo clock instance (must also implement [BeatClock]).
 * @param pollIntervalMs How often to check link state and update outputs (default 16ms).
 */
class LinkFallbackClock(
    private val scope: CoroutineScope,
    private val linkClock: AbletonLinkClock,
    private val tapClock: BeatClock,
    private val pollIntervalMs: Long = 16L
) : BeatClock {

    // ---- StateFlows (BeatClock) ----

    private val _bpm = MutableStateFlow(tapClock.bpm.value)
    override val bpm: StateFlow<Float> = _bpm.asStateFlow()

    private val _beatPhase = MutableStateFlow(0f)
    override val beatPhase: StateFlow<Float> = _beatPhase.asStateFlow()

    private val _barPhase = MutableStateFlow(0f)
    override val barPhase: StateFlow<Float> = _barPhase.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _beatState = MutableStateFlow(BeatState.IDLE)
    override val beatState: StateFlow<BeatState> = _beatState.asStateFlow()

    // ---- Link-specific StateFlows ----

    private val _syncSource = MutableStateFlow(SyncSource.NONE)

    /** Which timing source is currently providing beat data. */
    val syncSource: StateFlow<SyncSource> = _syncSource.asStateFlow()

    /** Peer count passthrough from the Link clock. */
    val peerCount: StateFlow<Int> get() = linkClock.peerCount

    // ---- Internal ----

    private var monitorJob: Job? = null

    // ---- BeatClock lifecycle ----

    /**
     * Start both clocks and begin monitoring which source to use.
     *
     * The link clock is started first. If it finds peers, it takes priority.
     * The tap clock is also started so it's ready as a fallback.
     */
    override fun start() {
        if (_isRunning.value) return
        _isRunning.value = true

        linkClock.start()
        tapClock.start()

        startMonitorLoop()
    }

    /**
     * Stop both clocks and the monitor loop.
     */
    override fun stop() {
        if (!_isRunning.value) return
        _isRunning.value = false
        monitorJob?.cancel()
        monitorJob = null

        linkClock.stop()
        tapClock.stop()
        _syncSource.value = SyncSource.NONE
    }

    // ---- Internal ----

    private fun startMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                updateFromActiveSource()
                delay(pollIntervalMs)
            }
        }
    }

    /**
     * Determine the active source and copy its StateFlow values to our outputs.
     * Visible for testing.
     */
    internal fun updateFromActiveSource() {
        if (!_isRunning.value) {
            _syncSource.value = SyncSource.NONE
            return
        }

        val currentLinkState = linkClock.linkState.value
        val activeClock: BeatClock
        val newSource: SyncSource

        when (currentLinkState) {
            AbletonLinkClock.LinkState.CONNECTED -> {
                activeClock = linkClock
                newSource = SyncSource.LINK
            }
            AbletonLinkClock.LinkState.SEARCHING -> {
                // During search, still use Link (it may have valid phase from its own tempo)
                activeClock = linkClock
                newSource = SyncSource.LINK
            }
            AbletonLinkClock.LinkState.NO_LINK,
            AbletonLinkClock.LinkState.DISABLED -> {
                activeClock = tapClock
                newSource = SyncSource.TAP
            }
        }

        _syncSource.value = newSource
        _bpm.value = activeClock.bpm.value
        _beatPhase.value = activeClock.beatPhase.value
        _barPhase.value = activeClock.barPhase.value
        _beatState.value = activeClock.beatState.value
    }

    companion object
}

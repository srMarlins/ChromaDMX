package com.chromadmx.networking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

/**
 * Routes DMX frames to real or simulated transport based on [TransportMode].
 *
 * In [TransportMode.Mixed] mode, frames are sent to both transports
 * simultaneously, enabling real hardware output alongside simulation
 * visualization.
 */
class DmxTransportRouter(
    private val real: DmxTransport,
    private val simulated: DmxTransport,
    private val scope: CoroutineScope,
) : DmxTransport {

    private val _mode = MutableStateFlow(TransportMode.Real)
    val mode: StateFlow<TransportMode> = _mode.asStateFlow()

    /**
     * Switch the active transport mode at runtime.
     *
     * Manages transport lifecycle: starts transports needed for the new mode,
     * stops transports no longer needed (unless switching to Mixed which needs both).
     */
    fun switchTo(mode: TransportMode) {
        val previous = _mode.value
        _mode.value = mode

        // If any transport was running, adjust lifecycle for the new mode
        if (real.isRunning || simulated.isRunning) {
            when (mode) {
                TransportMode.Real -> {
                    if (!real.isRunning) real.start()
                    if (previous != TransportMode.Mixed) simulated.stop()
                    else simulated.stop()
                }
                TransportMode.Simulated -> {
                    if (!simulated.isRunning) simulated.start()
                    if (previous != TransportMode.Mixed) real.stop()
                    else real.stop()
                }
                TransportMode.Mixed -> {
                    if (!real.isRunning) real.start()
                    if (!simulated.isRunning) simulated.start()
                }
            }
        }
    }

    override fun start() {
        when (_mode.value) {
            TransportMode.Real -> real.start()
            TransportMode.Simulated -> simulated.start()
            TransportMode.Mixed -> { real.start(); simulated.start() }
        }
    }

    override fun stop() {
        real.stop()
        simulated.stop()
    }

    override fun sendFrame(universe: Int, channels: ByteArray) {
        when (_mode.value) {
            TransportMode.Real -> real.sendFrame(universe, channels)
            TransportMode.Simulated -> simulated.sendFrame(universe, channels)
            TransportMode.Mixed -> {
                real.sendFrame(universe, channels)
                simulated.sendFrame(universe, channels)
            }
        }
    }

    override fun updateFrame(universeData: Map<Int, ByteArray>) {
        when (_mode.value) {
            TransportMode.Real -> real.updateFrame(universeData)
            TransportMode.Simulated -> simulated.updateFrame(universeData)
            TransportMode.Mixed -> {
                real.updateFrame(universeData)
                simulated.updateFrame(universeData)
            }
        }
    }

    override val connectionState: StateFlow<ConnectionState> by lazy {
        combine(_mode, real.connectionState, simulated.connectionState) { mode, realState, simState ->
            when (mode) {
                TransportMode.Real -> realState
                TransportMode.Simulated -> simState
                TransportMode.Mixed -> {
                    // In Mixed mode, Error takes priority so hardware failures
                    // are never masked by a healthy simulator
                    when {
                        realState == ConnectionState.Error || simState == ConnectionState.Error -> ConnectionState.Error
                        realState == ConnectionState.Connected && simState == ConnectionState.Connected -> ConnectionState.Connected
                        realState == ConnectionState.Connecting || simState == ConnectionState.Connecting -> ConnectionState.Connecting
                        realState == ConnectionState.Connected || simState == ConnectionState.Connected -> ConnectionState.Connected
                        else -> ConnectionState.Disconnected
                    }
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, ConnectionState.Disconnected)
    }

    override val isRunning: Boolean
        get() = when (_mode.value) {
            TransportMode.Real -> real.isRunning
            TransportMode.Simulated -> simulated.isRunning
            TransportMode.Mixed -> real.isRunning || simulated.isRunning
        }
}

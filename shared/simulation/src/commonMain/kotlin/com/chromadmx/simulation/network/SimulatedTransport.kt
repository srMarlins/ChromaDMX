package com.chromadmx.simulation.network

import com.chromadmx.networking.ConnectionState
import com.chromadmx.networking.DmxTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Simulated DMX transport that captures frames in memory.
 *
 * Exposes [lastFrame] for renderers to read simulated fixture colors
 * without touching real network hardware.
 *
 * @param coroutineContext Context for launching coroutines (injectable for tests)
 */
class SimulatedTransport(
    private val coroutineContext: CoroutineContext = Dispatchers.Default,
) : DmxTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lastFrame = MutableStateFlow<Map<Int, ByteArray>>(emptyMap())

    /** The most recent frame data, keyed by universe number. */
    val lastFrame: StateFlow<Map<Int, ByteArray>> = _lastFrame.asStateFlow()

    override var isRunning: Boolean = false
        private set

    private var scope: CoroutineScope? = null

    override fun start() {
        if (isRunning) return
        scope = CoroutineScope(SupervisorJob() + coroutineContext)
        _connectionState.value = ConnectionState.Connecting
        scope!!.launch {
            delay(100) // Simulate brief connection delay
            _connectionState.value = ConnectionState.Connected
        }
        isRunning = true
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        isRunning = false
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun sendFrame(universe: Int, channels: ByteArray) {
        _lastFrame.value = _lastFrame.value + (universe to channels.copyOf())
    }

    override fun updateFrame(universeData: Map<Int, ByteArray>) {
        _lastFrame.value = universeData.mapValues { it.value.copyOf() }
    }

    /**
     * Get the last DMX values for a specific universe.
     */
    fun getUniverseData(universe: Int): ByteArray? = _lastFrame.value[universe]

    /**
     * Get a specific channel value from a universe.
     *
     * @return the unsigned channel value (0-255), or null if not available
     */
    fun getChannelValue(universe: Int, channel: Int): Int? {
        val data = _lastFrame.value[universe] ?: return null
        if (channel !in data.indices) return null
        return data[channel].toInt() and 0xFF
    }

    /**
     * Clear all captured frames.
     */
    fun reset() {
        _lastFrame.value = emptyMap()
    }
}

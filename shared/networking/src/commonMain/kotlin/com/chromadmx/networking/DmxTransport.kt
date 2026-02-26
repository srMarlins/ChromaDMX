package com.chromadmx.networking

import kotlinx.coroutines.flow.StateFlow

/**
 * Connection state for DMX transport.
 */
enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Error
}

/**
 * Abstraction over DMX frame output — both real Art-Net/sACN and simulated.
 *
 * Implementors include [com.chromadmx.networking.output.DmxOutputService]
 * for real hardware and SimulatedTransport for testing.
 */
interface DmxTransport {
    fun start()
    fun stop()
    fun sendFrame(universe: Int, channels: ByteArray)
    fun updateFrame(universeData: Map<Int, ByteArray>)
    val connectionState: StateFlow<ConnectionState>
    val isRunning: Boolean
}

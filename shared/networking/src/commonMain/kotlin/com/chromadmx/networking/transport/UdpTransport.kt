package com.chromadmx.networking.transport

import com.chromadmx.networking.model.UdpPacket

/**
 * Platform-agnostic UDP transport abstraction.
 *
 * Provides suspend functions for sending/receiving UDP datagrams.
 */
interface UdpTransport {
    /**
     * Send a UDP datagram.
     *
     * @param data    Bytes to send
     * @param address Destination IP address (dotted string)
     * @param port    Destination UDP port
     */
    suspend fun send(data: ByteArray, address: String, port: Int)

    /**
     * Receive a UDP datagram.
     *
     * Blocks (suspending) until a packet arrives or the timeout expires.
     *
     * @param buffer     Pre-allocated buffer for the received data
     * @param timeoutMs  Maximum wait time in milliseconds
     * @return received packet, or null on timeout
     */
    suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket?

    /**
     * Close the transport and release resources.
     */
    fun close()
}

/**
 * Actual implementations use platform-specific socket APIs:
 * - Android: `java.net.DatagramSocket`
 * - iOS: NWConnection / native sockets (stub for now)
 */
expect class PlatformUdpTransport() : UdpTransport {
    override suspend fun send(data: ByteArray, address: String, port: Int)
    override suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket?
    override fun close()
}

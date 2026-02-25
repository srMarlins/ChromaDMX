package com.chromadmx.networking.transport

import com.chromadmx.networking.model.UdpPacket

/**
 * iOS stub implementation.
 *
 * TODO: Implement using NWConnection (Network framework) or POSIX sockets.
 * This stub allows the module to compile for iOS targets.
 */
actual class PlatformUdpTransport actual constructor() {

    actual suspend fun send(data: ByteArray, address: String, port: Int) {
        // Stub: iOS UDP send not yet implemented
    }

    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        // Stub: iOS UDP receive not yet implemented
        return null
    }

    actual fun close() {
        // Stub: nothing to close
    }
}

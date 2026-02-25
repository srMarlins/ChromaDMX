package com.chromadmx.networking.transport

import com.chromadmx.networking.model.UdpPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Android actual implementation using [DatagramSocket].
 *
 * All blocking socket operations are dispatched to [Dispatchers.IO].
 */
actual class PlatformUdpTransport actual constructor() {

    private val socket: DatagramSocket = DatagramSocket(null).apply {
        reuseAddress = true
        broadcast = true
    }

    actual suspend fun send(data: ByteArray, address: String, port: Int) {
        withContext(Dispatchers.IO) {
            val inetAddress = InetAddress.getByName(address)
            val datagram = DatagramPacket(data, data.size, inetAddress, port)
            socket.send(datagram)
        }
    }

    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        return withContext(Dispatchers.IO) {
            try {
                socket.soTimeout = timeoutMs.toInt()
                val datagram = DatagramPacket(buffer, buffer.size)
                socket.receive(datagram)

                UdpPacket(
                    data = buffer.copyOfRange(0, datagram.length),
                    address = datagram.address.hostAddress ?: "",
                    port = datagram.port
                )
            } catch (_: java.net.SocketTimeoutException) {
                null
            } catch (_: java.net.SocketException) {
                // Socket was closed
                null
            }
        }
    }

    actual fun close() {
        socket.close()
    }
}

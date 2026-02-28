package com.chromadmx.networking.transport

import com.chromadmx.networking.model.UdpPacket
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.posix.AF_INET
import platform.posix.IPPROTO_UDP
import platform.posix.SOCK_DGRAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_BROADCAST
import platform.posix.SO_RCVTIMEO
import platform.posix.SO_REUSEADDR
import platform.posix.close
import platform.posix.errno
import platform.posix.recvfrom
import platform.posix.sendto
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval

/**
 * iOS actual implementation using POSIX UDP sockets via `platform.posix`.
 *
 * All blocking socket operations are dispatched to [Dispatchers.Default]
 * (Kotlin/Native has no Dispatchers.IO).
 *
 * The socket is configured with:
 * - SO_REUSEADDR: allows multiple sockets to bind to the same address
 * - SO_BROADCAST: enables sending to broadcast addresses (255.255.255.255)
 *
 * These match the Android DatagramSocket configuration.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformUdpTransport actual constructor() {

    /** File descriptor for the underlying UDP socket, or -1 if closed. */
    @Volatile
    private var fd: Int = -1

    init {
        fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        if (fd < 0) {
            throw RuntimeException("Failed to create UDP socket: errno=$errno")
        }
        // Enable address reuse (mirrors Android's reuseAddress = true)
        setIntOption(SOL_SOCKET, SO_REUSEADDR, 1)
        // Enable broadcast (mirrors Android's broadcast = true)
        setIntOption(SOL_SOCKET, SO_BROADCAST, 1)
    }

    actual suspend fun send(data: ByteArray, address: String, port: Int) {
        val socketFd = fd
        if (socketFd < 0) return

        withContext(Dispatchers.Default) {
            memScoped {
                val addr = alloc<sockaddr_in>()
                addr.sin_family = AF_INET.convert()
                addr.sin_port = hostToNetworkShort(port.toUShort())
                addr.sin_addr.s_addr = ipStringToUInt(address)

                data.usePinned { pinned ->
                    sendto(
                        socketFd,
                        pinned.addressOf(0),
                        data.size.convert(),
                        0,
                        addr.ptr.reinterpret(),
                        sizeOf<sockaddr_in>().convert()
                    )
                }
            }
        }
    }

    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        val socketFd = fd
        if (socketFd < 0) return null

        return withContext(Dispatchers.Default) {
            memScoped {
                // Set receive timeout via SO_RCVTIMEO
                val tv = alloc<timeval>()
                tv.tv_sec = (timeoutMs / 1000).convert()
                tv.tv_usec = ((timeoutMs % 1000) * 1000).convert()
                setsockopt(
                    socketFd,
                    SOL_SOCKET,
                    SO_RCVTIMEO,
                    tv.ptr,
                    sizeOf<timeval>().convert()
                )

                val senderAddr = alloc<sockaddr_in>()
                val addrLen = alloc<UIntVar>()
                addrLen.value = sizeOf<sockaddr_in>().convert()

                val bytesRead = buffer.usePinned { pinned ->
                    recvfrom(
                        socketFd,
                        pinned.addressOf(0),
                        buffer.size.convert(),
                        0,
                        senderAddr.ptr.reinterpret(),
                        addrLen.ptr
                    )
                }

                if (bytesRead <= 0) {
                    // Timeout or error — return null (matches Android behavior)
                    null
                } else {
                    val addressStr = uIntToIpString(senderAddr.sin_addr.s_addr)
                    val senderPort = networkToHostShort(senderAddr.sin_port).toInt()

                    UdpPacket(
                        data = buffer.copyOfRange(0, bytesRead.toInt()),
                        address = addressStr,
                        port = senderPort
                    )
                }
            }
        }
    }

    actual fun close() {
        val socketFd = fd
        if (socketFd >= 0) {
            fd = -1
            close(socketFd)
        }
    }

    /**
     * Helper to set an integer socket option.
     */
    private fun setIntOption(level: Int, option: Int, value: Int) {
        memScoped {
            val optVal = alloc<IntVar>()
            optVal.value = value
            setsockopt(
                fd,
                level,
                option,
                optVal.ptr,
                sizeOf<IntVar>().convert()
            )
        }
    }
}

/**
 * Convert host-order 16-bit to network byte order (big-endian).
 *
 * All iOS targets (ARM64 device + ARM64 simulator + x86_64 simulator)
 * are little-endian, so this always byte-swaps.
 */
private fun hostToNetworkShort(value: UShort): UShort {
    val v = value.toInt()
    return (((v and 0xFF) shl 8) or ((v shr 8) and 0xFF)).toUShort()
}

/**
 * Convert network byte order (big-endian) to host-order 16-bit.
 */
private fun networkToHostShort(value: UShort): UShort = hostToNetworkShort(value)

/**
 * Convert a dotted-quad IPv4 string (e.g. "192.168.1.1") to a UInt in
 * network byte order (big-endian), matching `inet_addr` semantics.
 *
 * On little-endian (all iOS targets), the first octet occupies the
 * least-significant byte of the UInt.
 */
private fun ipStringToUInt(address: String): UInt {
    val octets = address.split(".")
    if (octets.size != 4) return 0u
    return (octets[0].toUInt() and 0xFFu) or
        ((octets[1].toUInt() and 0xFFu) shl 8) or
        ((octets[2].toUInt() and 0xFFu) shl 16) or
        ((octets[3].toUInt() and 0xFFu) shl 24)
}

/**
 * Convert a UInt in network byte order back to a dotted-quad IPv4
 * string, matching `inet_ntop` semantics.
 */
private fun uIntToIpString(addr: UInt): String {
    return "${addr and 0xFFu}.${(addr shr 8) and 0xFFu}.${(addr shr 16) and 0xFFu}.${(addr shr 24) and 0xFFu}"
}

package com.chromadmx.networking.model

/**
 * Simple wrapper for a received UDP datagram.
 *
 * @property data     Raw bytes of the datagram payload
 * @property address  Source IP address as a string (e.g. "192.168.1.100")
 * @property port     Source UDP port number
 */
data class UdpPacket(
    val data: ByteArray,
    val address: String,
    val port: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UdpPacket) return false
        return data.contentEquals(other.data) &&
            address == other.address &&
            port == other.port
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + port
        return result
    }

    override fun toString(): String = "UdpPacket(address=$address, port=$port, size=${data.size})"
}

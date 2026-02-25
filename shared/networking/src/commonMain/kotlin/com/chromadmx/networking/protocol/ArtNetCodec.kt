package com.chromadmx.networking.protocol

import com.chromadmx.networking.protocol.ArtNetConstants.ART_DMX_HEADER_SIZE
import com.chromadmx.networking.protocol.ArtNetConstants.ART_DMX_MAX_SIZE
import com.chromadmx.networking.protocol.ArtNetConstants.ART_POLL_REPLY_SIZE
import com.chromadmx.networking.protocol.ArtNetConstants.ART_POLL_SIZE
import com.chromadmx.networking.protocol.ArtNetConstants.DMX_DATA_MAX_LENGTH
import com.chromadmx.networking.protocol.ArtNetConstants.HEADER
import com.chromadmx.networking.protocol.ArtNetConstants.HEADER_SIZE
import com.chromadmx.networking.protocol.ArtNetConstants.OP_DMX
import com.chromadmx.networking.protocol.ArtNetConstants.OP_POLL
import com.chromadmx.networking.protocol.ArtNetConstants.OP_POLL_REPLY
import com.chromadmx.networking.protocol.ArtNetConstants.PROTOCOL_VERSION
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_BIND_IP
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_IP
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_LONG_NAME
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_MAC
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_NET_SWITCH
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_NUM_PORTS
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_PORT
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_SHORT_NAME
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_STATUS
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_STYLE
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_SUB_SWITCH
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_SW_IN
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_SW_OUT
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_VERSION_HI
import com.chromadmx.networking.protocol.ArtNetConstants.REPLY_OFFSET_VERSION_LO

/**
 * Encode/decode Art-Net 4 packets.
 *
 * Supports:
 * - **ArtDmx** (OpCode 0x5000) — DMX512 channel data
 * - **ArtPoll** (OpCode 0x2000) — network discovery request
 * - **ArtPollReply** (OpCode 0x2100) — node announcement
 *
 * All packets begin with the 8-byte header "Art-Net\0".
 * OpCodes are encoded little-endian; ProtVer is big-endian.
 */
object ArtNetCodec {

    // ------------------------------------------------------------------ //
    //  ArtDmx — encode / decode                                           //
    // ------------------------------------------------------------------ //

    /**
     * Encode an ArtDmx packet.
     *
     * @param sequence   Rolling sequence number 1..255 (0 disables reordering)
     * @param physical   Physical input port (informational only)
     * @param universe   15-bit Art-Net universe (0..32767): low 8 bits = SubUni, high 7 bits = Net
     * @param data       DMX channel data, 2..512 bytes (must be even length)
     * @return encoded packet bytes
     */
    fun encodeArtDmx(
        sequence: Byte,
        physical: Byte,
        universe: Int,
        data: ByteArray
    ): ByteArray {
        require(data.size in 2..DMX_DATA_MAX_LENGTH) {
            "DMX data length must be 2..512, got ${data.size}"
        }
        // Art-Net spec requires even data length
        val paddedLength = if (data.size % 2 != 0) data.size + 1 else data.size
        val padded = if (paddedLength != data.size) data.copyOf(paddedLength) else data

        val packet = ByteArray(ART_DMX_HEADER_SIZE + paddedLength)
        var offset = 0

        // Header "Art-Net\0"
        HEADER.copyInto(packet, offset)
        offset += HEADER_SIZE

        // OpCode 0x5000 little-endian
        packet[offset++] = (OP_DMX and 0xFF).toByte()
        packet[offset++] = ((OP_DMX shr 8) and 0xFF).toByte()

        // ProtVer 14 big-endian
        packet[offset++] = ((PROTOCOL_VERSION shr 8) and 0xFF).toByte()
        packet[offset++] = (PROTOCOL_VERSION and 0xFF).toByte()

        // Sequence
        packet[offset++] = sequence

        // Physical
        packet[offset++] = physical

        // SubUni (low byte of universe)
        packet[offset++] = (universe and 0xFF).toByte()

        // Net (high byte of universe — bits 8..14)
        packet[offset++] = ((universe shr 8) and 0x7F).toByte()

        // Length big-endian
        packet[offset++] = ((paddedLength shr 8) and 0xFF).toByte()
        packet[offset++] = (paddedLength and 0xFF).toByte()

        // DMX data
        padded.copyInto(packet, offset)

        return packet
    }

    /**
     * Decoded ArtDmx packet fields.
     */
    data class ArtDmxPacket(
        val sequence: Byte,
        val physical: Byte,
        val universe: Int,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ArtDmxPacket) return false
            return sequence == other.sequence &&
                physical == other.physical &&
                universe == other.universe &&
                data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = sequence.hashCode()
            result = 31 * result + physical.hashCode()
            result = 31 * result + universe
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * Decode an ArtDmx packet from raw bytes.
     *
     * @return decoded packet or null if the data is not a valid ArtDmx packet
     */
    fun decodeArtDmx(packet: ByteArray): ArtDmxPacket? {
        if (packet.size < ART_DMX_HEADER_SIZE) return null
        if (!hasValidHeader(packet)) return null
        if (readOpCode(packet) != OP_DMX) return null

        val sequence = packet[12]
        val physical = packet[13]
        val subUni = packet[14].toInt() and 0xFF
        val net = packet[15].toInt() and 0x7F
        val universe = (net shl 8) or subUni

        val lengthHi = packet[16].toInt() and 0xFF
        val lengthLo = packet[17].toInt() and 0xFF
        val length = (lengthHi shl 8) or lengthLo

        if (length < 2 || length > DMX_DATA_MAX_LENGTH) return null
        if (packet.size < ART_DMX_HEADER_SIZE + length) return null

        val data = packet.copyOfRange(ART_DMX_HEADER_SIZE, ART_DMX_HEADER_SIZE + length)
        return ArtDmxPacket(sequence, physical, universe, data)
    }

    // ------------------------------------------------------------------ //
    //  ArtPoll — encode / decode                                          //
    // ------------------------------------------------------------------ //

    /**
     * Encode an ArtPoll packet.
     *
     * @param flags        TalkToMe flags (default 0x00)
     * @param diagPriority Diagnostic priority (default 0x00)
     * @return encoded packet bytes (14 bytes)
     */
    fun encodeArtPoll(
        flags: Byte = 0x00,
        diagPriority: Byte = 0x00
    ): ByteArray {
        val packet = ByteArray(ART_POLL_SIZE)
        var offset = 0

        // Header
        HEADER.copyInto(packet, offset)
        offset += HEADER_SIZE

        // OpCode 0x2000 little-endian
        packet[offset++] = (OP_POLL and 0xFF).toByte()
        packet[offset++] = ((OP_POLL shr 8) and 0xFF).toByte()

        // ProtVer 14 big-endian
        packet[offset++] = ((PROTOCOL_VERSION shr 8) and 0xFF).toByte()
        packet[offset++] = (PROTOCOL_VERSION and 0xFF).toByte()

        // TalkToMe flags
        packet[offset++] = flags

        // DiagPriority
        packet[offset] = diagPriority

        return packet
    }

    /**
     * Decoded ArtPoll packet fields.
     */
    data class ArtPollPacket(
        val flags: Byte,
        val diagPriority: Byte
    )

    /**
     * Decode an ArtPoll packet from raw bytes.
     *
     * @return decoded packet or null if invalid
     */
    fun decodeArtPoll(packet: ByteArray): ArtPollPacket? {
        if (packet.size < ART_POLL_SIZE) return null
        if (!hasValidHeader(packet)) return null
        if (readOpCode(packet) != OP_POLL) return null

        return ArtPollPacket(
            flags = packet[12],
            diagPriority = packet[13]
        )
    }

    // ------------------------------------------------------------------ //
    //  ArtPollReply — encode / decode                                     //
    // ------------------------------------------------------------------ //

    /**
     * Decoded ArtPollReply fields.
     */
    data class ArtPollReplyPacket(
        val ipAddress: ByteArray,
        val port: Int,
        val firmwareVersion: Int,
        val netSwitch: Int,
        val subSwitch: Int,
        val shortName: String,
        val longName: String,
        val numPorts: Int,
        val swIn: ByteArray,
        val swOut: ByteArray,
        val style: Byte,
        val macAddress: ByteArray,
        val bindIp: ByteArray,
        val status: Byte
    ) {
        /** Full 15-bit Art-Net address for the first output port. */
        val universe: Int
            get() = ((netSwitch and 0x7F) shl 8) or ((subSwitch and 0x0F) shl 4) or (swOut.firstOrNull()?.toInt()?.and(0x0F) ?: 0)

        /** IP address as a dotted string. */
        val ipString: String
            get() = ipAddress.joinToString(".") { (it.toInt() and 0xFF).toString() }

        /** MAC address as colon-separated hex string. */
        val macString: String
            get() = macAddress.joinToString(":") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ArtPollReplyPacket) return false
            return ipAddress.contentEquals(other.ipAddress) &&
                port == other.port &&
                firmwareVersion == other.firmwareVersion &&
                netSwitch == other.netSwitch &&
                subSwitch == other.subSwitch &&
                shortName == other.shortName &&
                longName == other.longName &&
                numPorts == other.numPorts &&
                swIn.contentEquals(other.swIn) &&
                swOut.contentEquals(other.swOut) &&
                style == other.style &&
                macAddress.contentEquals(other.macAddress) &&
                bindIp.contentEquals(other.bindIp) &&
                status == other.status
        }

        override fun hashCode(): Int {
            var result = ipAddress.contentHashCode()
            result = 31 * result + port
            result = 31 * result + firmwareVersion
            result = 31 * result + shortName.hashCode()
            result = 31 * result + longName.hashCode()
            return result
        }
    }

    /**
     * Encode an ArtPollReply packet.
     *
     * @param ipAddress     4-byte IP address
     * @param port          UDP port (typically 6454)
     * @param firmwareVersion firmware version number
     * @param netSwitch     Net field (bits 14..8 of the 15-bit universe address)
     * @param subSwitch     SubNet field (bits 7..4 of the universe address)
     * @param shortName     Short node name (max 17 chars + null, padded to 18 bytes)
     * @param longName      Long node name (max 63 chars + null, padded to 64 bytes)
     * @param numPorts      Number of input/output ports
     * @param swIn          4-byte array of input universe addresses
     * @param swOut         4-byte array of output universe addresses
     * @param style         Node style code
     * @param macAddress    6-byte MAC address
     * @param bindIp        4-byte bind IP address
     * @param status        Status byte
     * @return encoded packet bytes (239 bytes)
     */
    fun encodeArtPollReply(
        ipAddress: ByteArray = ByteArray(4),
        port: Int = ArtNetConstants.PORT,
        firmwareVersion: Int = 0,
        netSwitch: Int = 0,
        subSwitch: Int = 0,
        shortName: String = "",
        longName: String = "",
        numPorts: Int = 0,
        swIn: ByteArray = ByteArray(4),
        swOut: ByteArray = ByteArray(4),
        style: Byte = ArtNetConstants.STYLE_NODE,
        macAddress: ByteArray = ByteArray(6),
        bindIp: ByteArray = ByteArray(4),
        status: Byte = 0
    ): ByteArray {
        val packet = ByteArray(ART_POLL_REPLY_SIZE)
        var offset = 0

        // Header
        HEADER.copyInto(packet, offset)
        offset += HEADER_SIZE

        // OpCode 0x2100 little-endian
        packet[offset++] = (OP_POLL_REPLY and 0xFF).toByte()
        packet[offset++] = ((OP_POLL_REPLY shr 8) and 0xFF).toByte()

        // IP Address (4 bytes)
        ipAddress.copyInto(packet, REPLY_OFFSET_IP, 0, minOf(4, ipAddress.size))

        // Port (little-endian)
        packet[REPLY_OFFSET_PORT] = (port and 0xFF).toByte()
        packet[REPLY_OFFSET_PORT + 1] = ((port shr 8) and 0xFF).toByte()

        // Firmware version (big-endian)
        packet[REPLY_OFFSET_VERSION_HI] = ((firmwareVersion shr 8) and 0xFF).toByte()
        packet[REPLY_OFFSET_VERSION_LO] = (firmwareVersion and 0xFF).toByte()

        // NetSwitch
        packet[REPLY_OFFSET_NET_SWITCH] = (netSwitch and 0x7F).toByte()

        // SubSwitch
        packet[REPLY_OFFSET_SUB_SWITCH] = (subSwitch and 0x0F).toByte()

        // Status
        packet[REPLY_OFFSET_STATUS] = status

        // Short name (18 bytes, null-terminated)
        val shortNameBytes = shortName.encodeToByteArray()
        shortNameBytes.copyInto(packet, REPLY_OFFSET_SHORT_NAME, 0, minOf(17, shortNameBytes.size))

        // Long name (64 bytes, null-terminated)
        val longNameBytes = longName.encodeToByteArray()
        longNameBytes.copyInto(packet, REPLY_OFFSET_LONG_NAME, 0, minOf(63, longNameBytes.size))

        // NumPorts (big-endian)
        packet[REPLY_OFFSET_NUM_PORTS] = ((numPorts shr 8) and 0xFF).toByte()
        packet[REPLY_OFFSET_NUM_PORTS + 1] = (numPorts and 0xFF).toByte()

        // SwIn (4 bytes)
        swIn.copyInto(packet, REPLY_OFFSET_SW_IN, 0, minOf(4, swIn.size))

        // SwOut (4 bytes)
        swOut.copyInto(packet, REPLY_OFFSET_SW_OUT, 0, minOf(4, swOut.size))

        // Style
        packet[REPLY_OFFSET_STYLE] = style

        // MAC Address (6 bytes)
        macAddress.copyInto(packet, REPLY_OFFSET_MAC, 0, minOf(6, macAddress.size))

        // Bind IP (4 bytes)
        bindIp.copyInto(packet, REPLY_OFFSET_BIND_IP, 0, minOf(4, bindIp.size))

        return packet
    }

    /**
     * Decode an ArtPollReply packet from raw bytes.
     *
     * @return decoded packet or null if invalid
     */
    fun decodeArtPollReply(packet: ByteArray): ArtPollReplyPacket? {
        if (packet.size < ART_POLL_REPLY_SIZE) return null
        if (!hasValidHeader(packet)) return null
        if (readOpCode(packet) != OP_POLL_REPLY) return null

        val ipAddress = packet.copyOfRange(REPLY_OFFSET_IP, REPLY_OFFSET_IP + 4)

        val portLo = packet[REPLY_OFFSET_PORT].toInt() and 0xFF
        val portHi = packet[REPLY_OFFSET_PORT + 1].toInt() and 0xFF
        val port = (portHi shl 8) or portLo

        val versionHi = packet[REPLY_OFFSET_VERSION_HI].toInt() and 0xFF
        val versionLo = packet[REPLY_OFFSET_VERSION_LO].toInt() and 0xFF
        val firmwareVersion = (versionHi shl 8) or versionLo

        val netSwitch = packet[REPLY_OFFSET_NET_SWITCH].toInt() and 0x7F
        val subSwitch = packet[REPLY_OFFSET_SUB_SWITCH].toInt() and 0x0F

        val shortName = decodeNullTerminatedString(packet, REPLY_OFFSET_SHORT_NAME, 18)
        val longName = decodeNullTerminatedString(packet, REPLY_OFFSET_LONG_NAME, 64)

        val numPortsHi = packet[REPLY_OFFSET_NUM_PORTS].toInt() and 0xFF
        val numPortsLo = packet[REPLY_OFFSET_NUM_PORTS + 1].toInt() and 0xFF
        val numPorts = (numPortsHi shl 8) or numPortsLo

        val swIn = packet.copyOfRange(REPLY_OFFSET_SW_IN, REPLY_OFFSET_SW_IN + 4)
        val swOut = packet.copyOfRange(REPLY_OFFSET_SW_OUT, REPLY_OFFSET_SW_OUT + 4)

        val style = packet[REPLY_OFFSET_STYLE]
        val macAddress = packet.copyOfRange(REPLY_OFFSET_MAC, REPLY_OFFSET_MAC + 6)
        val bindIp = packet.copyOfRange(REPLY_OFFSET_BIND_IP, REPLY_OFFSET_BIND_IP + 4)
        val status = packet[REPLY_OFFSET_STATUS]

        return ArtPollReplyPacket(
            ipAddress = ipAddress,
            port = port,
            firmwareVersion = firmwareVersion,
            netSwitch = netSwitch,
            subSwitch = subSwitch,
            shortName = shortName,
            longName = longName,
            numPorts = numPorts,
            swIn = swIn,
            swOut = swOut,
            style = style,
            macAddress = macAddress,
            bindIp = bindIp,
            status = status
        )
    }

    // ------------------------------------------------------------------ //
    //  Utility                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Read the OpCode from an Art-Net packet (little-endian at offset 8-9).
     */
    fun readOpCode(packet: ByteArray): Int {
        if (packet.size < HEADER_SIZE + 2) return -1
        val lo = packet[HEADER_SIZE].toInt() and 0xFF
        val hi = packet[HEADER_SIZE + 1].toInt() and 0xFF
        return (hi shl 8) or lo
    }

    /**
     * Check if a packet has a valid "Art-Net\0" header.
     */
    fun hasValidHeader(packet: ByteArray): Boolean {
        if (packet.size < HEADER_SIZE) return false
        for (i in 0 until HEADER_SIZE) {
            if (packet[i] != HEADER[i]) return false
        }
        return true
    }

    /**
     * Decode a null-terminated ASCII string from a byte region.
     */
    private fun decodeNullTerminatedString(
        data: ByteArray,
        offset: Int,
        maxLength: Int
    ): String {
        val end = minOf(offset + maxLength, data.size)
        var nullPos = end
        for (i in offset until end) {
            if (data[i] == 0.toByte()) {
                nullPos = i
                break
            }
        }
        return data.decodeToString(offset, nullPos)
    }
}

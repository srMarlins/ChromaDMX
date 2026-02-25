package com.chromadmx.networking.protocol

import com.chromadmx.networking.protocol.SacnConstants.ACN_ID_SIZE
import com.chromadmx.networking.protocol.SacnConstants.ACN_PACKET_IDENTIFIER
import com.chromadmx.networking.protocol.SacnConstants.CID_SIZE
import com.chromadmx.networking.protocol.SacnConstants.DEFAULT_PRIORITY
import com.chromadmx.networking.protocol.SacnConstants.DMP_ADDRESS_DATA_TYPE
import com.chromadmx.networking.protocol.SacnConstants.DMP_ADDRESS_INCREMENT
import com.chromadmx.networking.protocol.SacnConstants.DMP_FIRST_PROPERTY_ADDRESS
import com.chromadmx.networking.protocol.SacnConstants.DMP_HEADER_SIZE
import com.chromadmx.networking.protocol.SacnConstants.FLAGS_MASK
import com.chromadmx.networking.protocol.SacnConstants.FRAMING_PDU_SIZE
import com.chromadmx.networking.protocol.SacnConstants.FULL_PACKET_SIZE
import com.chromadmx.networking.protocol.SacnConstants.MAX_DMX_SLOTS
import com.chromadmx.networking.protocol.SacnConstants.POSTAMBLE_SIZE
import com.chromadmx.networking.protocol.SacnConstants.PREAMBLE_SIZE
import com.chromadmx.networking.protocol.SacnConstants.ROOT_LAYER_SIZE
import com.chromadmx.networking.protocol.SacnConstants.ROOT_PREAMBLE_SIZE
import com.chromadmx.networking.protocol.SacnConstants.SOURCE_NAME_SIZE
import com.chromadmx.networking.protocol.SacnConstants.VECTOR_DMP_SET_PROPERTY
import com.chromadmx.networking.protocol.SacnConstants.VECTOR_E131_DATA_PACKET
import com.chromadmx.networking.protocol.SacnConstants.VECTOR_ROOT_E131_DATA

/**
 * Encode/decode sACN (E1.31) data packets.
 *
 * Supports the E1.31 Data Packet format which consists of three layers:
 * - **ACN Root Layer** — preamble, ACN identifier, CID
 * - **Framing Layer** — source name, priority, sequence, universe
 * - **DMP Layer** — DMX start code + slot data
 *
 * All multi-byte fields are big-endian (network byte order).
 */
object SacnCodec {

    /**
     * Decoded sACN data packet fields.
     */
    data class SacnDataPacket(
        val cid: ByteArray,
        val sourceName: String,
        val priority: Int,
        val sequence: Int,
        val options: Int,
        val universe: Int,
        val startCode: Byte,
        val dmxData: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SacnDataPacket) return false
            return cid.contentEquals(other.cid) &&
                sourceName == other.sourceName &&
                priority == other.priority &&
                sequence == other.sequence &&
                options == other.options &&
                universe == other.universe &&
                startCode == other.startCode &&
                dmxData.contentEquals(other.dmxData)
        }

        override fun hashCode(): Int {
            var result = cid.contentHashCode()
            result = 31 * result + sourceName.hashCode()
            result = 31 * result + priority
            result = 31 * result + sequence
            result = 31 * result + universe
            result = 31 * result + dmxData.contentHashCode()
            return result
        }
    }

    // ------------------------------------------------------------------ //
    //  Encode                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Encode a sACN E1.31 data packet.
     *
     * @param cid          16-byte Component Identifier (UUID)
     * @param sourceName   User-assigned source name (max 63 chars + null)
     * @param priority     Packet priority 0-200 (default 100)
     * @param sequence     Sequence number 0-255
     * @param options      Options flags byte
     * @param universe     DMX universe number (1-63999)
     * @param startCode    DMX512-A start code (usually 0x00)
     * @param dmxData      DMX slot data, 1..512 bytes
     * @return encoded packet bytes
     */
    fun encode(
        cid: ByteArray = ByteArray(CID_SIZE),
        sourceName: String = "",
        priority: Int = DEFAULT_PRIORITY,
        sequence: Int = 0,
        options: Int = 0,
        universe: Int = 1,
        startCode: Byte = 0x00,
        dmxData: ByteArray = ByteArray(MAX_DMX_SLOTS)
    ): ByteArray {
        require(cid.size == CID_SIZE) { "CID must be $CID_SIZE bytes, got ${cid.size}" }
        require(dmxData.size in 1..MAX_DMX_SLOTS) { "DMX data must be 1..$MAX_DMX_SLOTS bytes, got ${dmxData.size}" }
        require(universe in 1..63999) { "Universe must be 1..63999, got $universe" }
        require(priority in 0..200) { "Priority must be 0..200, got $priority" }

        val slotCount = dmxData.size
        val propertyValueCount = 1 + slotCount  // start code + DMX slots
        val dmpLayerLength = DMP_HEADER_SIZE + propertyValueCount
        val framingLayerLength = FRAMING_PDU_SIZE + dmpLayerLength
        val rootPduLength = 22 + framingLayerLength  // flags+len(2) + vector(4) + CID(16) + framing

        // Actually the root PDU length counts from after the preamble:
        // flags+length(2) + vector(4) + CID(16) + framing layer
        val rootFlagsLengthValue = framingLayerLength + 22  // 22 = 2 (itself not counted) ...
        // Per spec: Root layer PDU length = number of octets from (and including) the Flags & Length
        // to the end of the PDU. So it includes itself (2) + vector(4) + CID(16) + all downstream.
        val totalPacketSize = ROOT_PREAMBLE_SIZE + 2 + 4 + CID_SIZE + framingLayerLength

        val packet = ByteArray(totalPacketSize)
        var offset = 0

        // ---- ACN Root Layer Preamble ---- //

        // Preamble Size (2 bytes BE)
        packet[offset++] = ((PREAMBLE_SIZE shr 8) and 0xFF).toByte()
        packet[offset++] = (PREAMBLE_SIZE and 0xFF).toByte()

        // Postamble Size (2 bytes BE)
        packet[offset++] = ((POSTAMBLE_SIZE shr 8) and 0xFF).toByte()
        packet[offset++] = (POSTAMBLE_SIZE and 0xFF).toByte()

        // ACN Packet Identifier (12 bytes)
        ACN_PACKET_IDENTIFIER.copyInto(packet, offset)
        offset += ACN_ID_SIZE

        // ---- ACN Root Layer PDU ---- //

        // Flags & Length (2 bytes): high 4 bits = 0x7, low 12 bits = PDU length
        val rootPduLen = 4 + CID_SIZE + framingLayerLength  // vector + CID + downstream
        val rootFlagsLength = FLAGS_MASK or (rootPduLen + 2)  // +2 for the flags+length field itself
        // Wait — per the spec, PDU length INCLUDES the flags & length field.
        // Root PDU length = from flags&length to end = 2 + 4 + 16 + framingLayerLength
        val rootLen = 2 + 4 + CID_SIZE + framingLayerLength
        val rootFL = FLAGS_MASK or (rootLen and 0x0FFF)
        packet[offset++] = ((rootFL shr 8) and 0xFF).toByte()
        packet[offset++] = (rootFL and 0xFF).toByte()

        // Root Layer Vector (4 bytes BE)
        packet[offset++] = ((VECTOR_ROOT_E131_DATA shr 24) and 0xFF).toByte()
        packet[offset++] = ((VECTOR_ROOT_E131_DATA shr 16) and 0xFF).toByte()
        packet[offset++] = ((VECTOR_ROOT_E131_DATA shr 8) and 0xFF).toByte()
        packet[offset++] = (VECTOR_ROOT_E131_DATA and 0xFF).toByte()

        // CID (16 bytes)
        cid.copyInto(packet, offset)
        offset += CID_SIZE

        // ---- Framing Layer PDU ---- //

        // Flags & Length
        val framingLen = 2 + 4 + SOURCE_NAME_SIZE + 1 + 2 + 1 + 1 + 2 + dmpLayerLength
        val framingFL = FLAGS_MASK or (framingLen and 0x0FFF)
        packet[offset++] = ((framingFL shr 8) and 0xFF).toByte()
        packet[offset++] = (framingFL and 0xFF).toByte()

        // Framing Layer Vector (4 bytes BE)
        packet[offset++] = ((VECTOR_E131_DATA_PACKET shr 24) and 0xFF).toByte()
        packet[offset++] = ((VECTOR_E131_DATA_PACKET shr 16) and 0xFF).toByte()
        packet[offset++] = ((VECTOR_E131_DATA_PACKET shr 8) and 0xFF).toByte()
        packet[offset++] = (VECTOR_E131_DATA_PACKET and 0xFF).toByte()

        // Source Name (64 bytes, null-terminated)
        val nameBytes = sourceName.encodeToByteArray()
        nameBytes.copyInto(packet, offset, 0, minOf(63, nameBytes.size))
        offset += SOURCE_NAME_SIZE

        // Priority (1 byte)
        packet[offset++] = (priority and 0xFF).toByte()

        // Synchronization Address — Reserved (2 bytes, 0x0000)
        packet[offset++] = 0x00
        packet[offset++] = 0x00

        // Sequence Number (1 byte)
        packet[offset++] = (sequence and 0xFF).toByte()

        // Options Flags (1 byte)
        packet[offset++] = (options and 0xFF).toByte()

        // Universe (2 bytes BE)
        packet[offset++] = ((universe shr 8) and 0xFF).toByte()
        packet[offset++] = (universe and 0xFF).toByte()

        // ---- DMP Layer PDU ---- //

        // Flags & Length
        val dmpLen = 2 + 1 + 1 + 2 + 2 + 2 + propertyValueCount  // includes flags+length itself
        val dmpFL = FLAGS_MASK or (dmpLen and 0x0FFF)
        packet[offset++] = ((dmpFL shr 8) and 0xFF).toByte()
        packet[offset++] = (dmpFL and 0xFF).toByte()

        // DMP Vector (1 byte)
        packet[offset++] = VECTOR_DMP_SET_PROPERTY.toByte()

        // Address Type & Data Type (1 byte)
        packet[offset++] = DMP_ADDRESS_DATA_TYPE.toByte()

        // First Property Address (2 bytes BE)
        packet[offset++] = ((DMP_FIRST_PROPERTY_ADDRESS shr 8) and 0xFF).toByte()
        packet[offset++] = (DMP_FIRST_PROPERTY_ADDRESS and 0xFF).toByte()

        // Address Increment (2 bytes BE)
        packet[offset++] = ((DMP_ADDRESS_INCREMENT shr 8) and 0xFF).toByte()
        packet[offset++] = (DMP_ADDRESS_INCREMENT and 0xFF).toByte()

        // Property Value Count (2 bytes BE) — includes start code
        packet[offset++] = ((propertyValueCount shr 8) and 0xFF).toByte()
        packet[offset++] = (propertyValueCount and 0xFF).toByte()

        // Start Code (1 byte)
        packet[offset++] = startCode

        // DMX Slot Data
        dmxData.copyInto(packet, offset)

        return packet
    }

    // ------------------------------------------------------------------ //
    //  Decode                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Decode a sACN E1.31 data packet from raw bytes.
     *
     * @return decoded packet or null if invalid
     */
    fun decode(packet: ByteArray): SacnDataPacket? {
        // Minimum: root preamble(16) + root PDU header(22) + framing minimum + DMP minimum
        if (packet.size < ROOT_LAYER_SIZE + 77 + 10 + 1) return null  // At least minimal size

        var offset = 0

        // ---- ACN Root Layer Preamble ---- //

        // Preamble Size
        val preambleSize = readUInt16BE(packet, offset)
        offset += 2
        if (preambleSize != PREAMBLE_SIZE) return null

        // Postamble Size
        val postambleSize = readUInt16BE(packet, offset)
        offset += 2
        if (postambleSize != POSTAMBLE_SIZE) return null

        // ACN Packet Identifier
        for (i in 0 until ACN_ID_SIZE) {
            if (packet[offset + i] != ACN_PACKET_IDENTIFIER[i]) return null
        }
        offset += ACN_ID_SIZE

        // ---- ACN Root Layer PDU ---- //

        // Flags & Length
        val rootFL = readUInt16BE(packet, offset)
        offset += 2
        val rootLength = rootFL and 0x0FFF
        if ((rootFL and 0xF000) != FLAGS_MASK) return null

        // Root Vector
        val rootVector = readUInt32BE(packet, offset)
        offset += 4
        if (rootVector != VECTOR_ROOT_E131_DATA) return null

        // CID
        if (offset + CID_SIZE > packet.size) return null
        val cid = packet.copyOfRange(offset, offset + CID_SIZE)
        offset += CID_SIZE

        // ---- Framing Layer PDU ---- //

        if (offset + 2 > packet.size) return null
        val framingFL = readUInt16BE(packet, offset)
        offset += 2
        val framingLength = framingFL and 0x0FFF
        if ((framingFL and 0xF000) != FLAGS_MASK) return null

        // Framing Vector
        if (offset + 4 > packet.size) return null
        val framingVector = readUInt32BE(packet, offset)
        offset += 4
        if (framingVector != VECTOR_E131_DATA_PACKET) return null

        // Source Name
        if (offset + SOURCE_NAME_SIZE > packet.size) return null
        val sourceName = decodeNullTerminatedString(packet, offset, SOURCE_NAME_SIZE)
        offset += SOURCE_NAME_SIZE

        // Priority
        if (offset >= packet.size) return null
        val priority = packet[offset].toInt() and 0xFF
        offset++

        // Reserved (Synchronization Address)
        offset += 2

        // Sequence
        if (offset >= packet.size) return null
        val sequence = packet[offset].toInt() and 0xFF
        offset++

        // Options
        if (offset >= packet.size) return null
        val options = packet[offset].toInt() and 0xFF
        offset++

        // Universe
        if (offset + 2 > packet.size) return null
        val universe = readUInt16BE(packet, offset)
        offset += 2

        // ---- DMP Layer PDU ---- //

        if (offset + 2 > packet.size) return null
        val dmpFL = readUInt16BE(packet, offset)
        offset += 2
        val dmpLength = dmpFL and 0x0FFF
        if ((dmpFL and 0xF000) != FLAGS_MASK) return null

        // DMP Vector
        if (offset >= packet.size) return null
        val dmpVector = packet[offset].toInt() and 0xFF
        offset++
        if (dmpVector != VECTOR_DMP_SET_PROPERTY) return null

        // Address Type & Data Type
        if (offset >= packet.size) return null
        val addrType = packet[offset].toInt() and 0xFF
        offset++
        if (addrType != DMP_ADDRESS_DATA_TYPE) return null

        // First Property Address
        if (offset + 2 > packet.size) return null
        offset += 2  // skip, already validated by vector

        // Address Increment
        if (offset + 2 > packet.size) return null
        offset += 2  // skip

        // Property Value Count
        if (offset + 2 > packet.size) return null
        val propertyValueCount = readUInt16BE(packet, offset)
        offset += 2

        if (propertyValueCount < 1) return null
        val slotCount = propertyValueCount - 1  // minus start code

        // Start Code
        if (offset >= packet.size) return null
        val startCode = packet[offset]
        offset++

        // DMX Data
        if (offset + slotCount > packet.size) return null
        val dmxData = if (slotCount > 0) {
            packet.copyOfRange(offset, offset + slotCount)
        } else {
            ByteArray(0)
        }

        return SacnDataPacket(
            cid = cid,
            sourceName = sourceName,
            priority = priority,
            sequence = sequence,
            options = options,
            universe = universe,
            startCode = startCode,
            dmxData = dmxData
        )
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Check if a packet looks like a valid sACN packet (checks preamble and identifier).
     */
    fun isValidPacket(packet: ByteArray): Boolean {
        if (packet.size < ROOT_LAYER_SIZE) return false
        val preamble = readUInt16BE(packet, 0)
        if (preamble != PREAMBLE_SIZE) return false
        for (i in 0 until ACN_ID_SIZE) {
            if (packet[4 + i] != ACN_PACKET_IDENTIFIER[i]) return false
        }
        return true
    }

    private fun readUInt16BE(data: ByteArray, offset: Int): Int {
        val hi = data[offset].toInt() and 0xFF
        val lo = data[offset + 1].toInt() and 0xFF
        return (hi shl 8) or lo
    }

    private fun readUInt32BE(data: ByteArray, offset: Int): Int {
        val b0 = data[offset].toInt() and 0xFF
        val b1 = data[offset + 1].toInt() and 0xFF
        val b2 = data[offset + 2].toInt() and 0xFF
        val b3 = data[offset + 3].toInt() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

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

package com.chromadmx.networking.protocol

/**
 * sACN (E1.31) protocol constants.
 *
 * Reference: ANSI E1.31-2016 Entertainment Technology —
 * Lightweight streaming protocol for transport of DMX512 using ACN.
 *
 * All multi-byte fields are big-endian (network byte order).
 */
object SacnConstants {

    /** Default sACN UDP port. */
    const val PORT: Int = 5568

    /** Default packet priority (0-200). */
    const val DEFAULT_PRIORITY: Int = 100

    // ---- ACN Root Layer ---- //

    /** Preamble size: 0x0010 (16). */
    const val PREAMBLE_SIZE: Int = 0x0010

    /** Post-amble size: always 0. */
    const val POSTAMBLE_SIZE: Int = 0x0000

    /**
     * ACN Packet Identifier (12 bytes).
     * ASCII: "ASC-E1.17\0\0\0"
     */
    val ACN_PACKET_IDENTIFIER: ByteArray = byteArrayOf(
        0x41, 0x53, 0x43, 0x2D, // "ASC-"
        0x45, 0x31, 0x2E, 0x31, // "E1.1"
        0x37, 0x00, 0x00, 0x00  // "7\0\0\0"
    )

    /** ACN Packet Identifier size. */
    const val ACN_ID_SIZE: Int = 12

    /** Root layer vector for E1.31 data packets. */
    const val VECTOR_ROOT_E131_DATA: Int = 0x00000004

    // ---- Framing Layer ---- //

    /** Framing layer vector for E1.31 data packets. */
    const val VECTOR_E131_DATA_PACKET: Int = 0x00000002

    /** Source name field size (64 bytes, null-terminated UTF-8). */
    const val SOURCE_NAME_SIZE: Int = 64

    // ---- DMP Layer ---- //

    /** DMP layer vector. */
    const val VECTOR_DMP_SET_PROPERTY: Int = 0x02

    /** DMP address type and data type. */
    const val DMP_ADDRESS_DATA_TYPE: Int = 0xA1

    /** DMP first property address. */
    const val DMP_FIRST_PROPERTY_ADDRESS: Int = 0x0000

    /** DMP address increment. */
    const val DMP_ADDRESS_INCREMENT: Int = 0x0001

    // ---- Options flags ---- //

    /** Options: Preview data (bit 7). */
    const val OPTION_PREVIEW: Int = 0x80

    /** Options: Stream terminated (bit 6). */
    const val OPTION_STREAM_TERMINATED: Int = 0x40

    /** Options: Force synchronization (bit 5). */
    const val OPTION_FORCE_SYNC: Int = 0x20

    // ---- Flags/Length mask ---- //

    /**
     * Flags portion of the Flags & Length field.
     * High 4 bits = 0x7 (protocol defined), low 12 bits = PDU length.
     */
    const val FLAGS_MASK: Int = 0x7000

    // ---- Layer sizes ---- //

    /** Root layer preamble: preamble(2) + postamble(2) + id(12) = 16 bytes. */
    const val ROOT_PREAMBLE_SIZE: Int = 16

    /** Root layer PDU: flags+length(2) + vector(4) + CID(16) = 22 bytes. */
    const val ROOT_PDU_SIZE: Int = 22

    /** Full root layer size. */
    const val ROOT_LAYER_SIZE: Int = ROOT_PREAMBLE_SIZE + ROOT_PDU_SIZE // 38

    /** Framing layer PDU size (without data):
     * flags+length(2) + vector(4) + sourceName(64) + priority(1) +
     * reserved(2) + sequence(1) + options(1) + universe(2) = 77 bytes.
     */
    const val FRAMING_PDU_SIZE: Int = 77

    /** DMP layer header size (without property values):
     * flags+length(2) + vector(1) + addressType(1) + firstAddr(2) +
     * increment(2) + count(2) = 10 bytes.
     */
    const val DMP_HEADER_SIZE: Int = 10

    /** Maximum property value count (1 start code + 512 DMX slots). */
    const val MAX_PROPERTY_VALUES: Int = 513

    /** Maximum DMX slots in one packet. */
    const val MAX_DMX_SLOTS: Int = 512

    /** Full packet size for 512 DMX slots. */
    const val FULL_PACKET_SIZE: Int = ROOT_LAYER_SIZE + FRAMING_PDU_SIZE + DMP_HEADER_SIZE + MAX_PROPERTY_VALUES // 638

    /** CID size (16-byte UUID). */
    const val CID_SIZE: Int = 16

    // ---- Multicast addressing ---- //

    /**
     * Compute the multicast address for a given sACN universe.
     *
     * Format: 239.255.{high byte}.{low byte}
     *
     * @param universe sACN universe number (1..63999)
     * @return multicast IP address string
     */
    fun multicastAddress(universe: Int): String {
        require(universe in 1..63999) { "sACN universe must be 1..63999, got $universe" }
        val hi = (universe shr 8) and 0xFF
        val lo = universe and 0xFF
        return "239.255.$hi.$lo"
    }
}

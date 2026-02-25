package com.chromadmx.networking.protocol

/**
 * Art-Net 4 protocol constants.
 *
 * Reference: Art-Net 4 Specification (art-net.org.uk)
 * All Art-Net packets start with the header "Art-Net\0" (8 bytes).
 * OpCodes are transmitted little-endian; ProtVer is big-endian.
 */
object ArtNetConstants {

    /** Art-Net header string including null terminator (8 bytes). */
    val HEADER: ByteArray = byteArrayOf(
        0x41, 0x72, 0x74, 0x2D, // "Art-"
        0x4E, 0x65, 0x74, 0x00  // "Net\0"
    )

    /** Header size in bytes. */
    const val HEADER_SIZE = 8

    /** Protocol version (14). */
    const val PROTOCOL_VERSION: Int = 14

    /** Default Art-Net UDP port. */
    const val PORT: Int = 6454

    /** Broadcast address for Art-Net discovery. */
    const val BROADCAST_ADDRESS = "255.255.255.255"

    // ---- OpCodes (little-endian on wire) ----

    /** ArtPoll — discover nodes on the network. */
    const val OP_POLL: Int = 0x2000

    /** ArtPollReply — node announces itself. */
    const val OP_POLL_REPLY: Int = 0x2100

    /** ArtDmx (ArtOutput) — DMX512 data. */
    const val OP_DMX: Int = 0x5000

    /** ArtSync — synchronization packet. */
    const val OP_SYNC: Int = 0x5200

    // ---- Packet sizes ----

    /** Minimum ArtPoll packet size. */
    const val ART_POLL_SIZE = 14

    /** Fixed ArtPollReply packet size. */
    const val ART_POLL_REPLY_SIZE = 239

    /** ArtDmx header size (before DMX data). */
    const val ART_DMX_HEADER_SIZE = 18

    /** Maximum DMX channel data length. */
    const val DMX_DATA_MAX_LENGTH = 512

    /** Maximum ArtDmx packet size (header + 512 data bytes). */
    const val ART_DMX_MAX_SIZE = ART_DMX_HEADER_SIZE + DMX_DATA_MAX_LENGTH

    // ---- ArtPollReply field offsets ----

    const val REPLY_OFFSET_IP = 10
    const val REPLY_OFFSET_PORT = 14
    const val REPLY_OFFSET_VERSION_HI = 16
    const val REPLY_OFFSET_VERSION_LO = 17
    const val REPLY_OFFSET_NET_SWITCH = 18
    const val REPLY_OFFSET_SUB_SWITCH = 19
    const val REPLY_OFFSET_OEM = 20
    const val REPLY_OFFSET_UBEA_VERSION = 22
    const val REPLY_OFFSET_STATUS = 23
    const val REPLY_OFFSET_ESTA = 24
    const val REPLY_OFFSET_SHORT_NAME = 26
    const val REPLY_OFFSET_LONG_NAME = 44
    const val REPLY_OFFSET_NODE_REPORT = 108
    const val REPLY_OFFSET_NUM_PORTS = 172
    const val REPLY_OFFSET_PORT_TYPES = 174
    const val REPLY_OFFSET_GOOD_INPUT = 178
    const val REPLY_OFFSET_GOOD_OUTPUT = 182
    const val REPLY_OFFSET_SW_IN = 186
    const val REPLY_OFFSET_SW_OUT = 190
    const val REPLY_OFFSET_STYLE = 200
    const val REPLY_OFFSET_MAC = 201
    const val REPLY_OFFSET_BIND_IP = 207
    const val REPLY_OFFSET_BIND_INDEX = 211
    const val REPLY_OFFSET_STATUS2 = 212

    // ---- Style codes ----

    /** Node (DMX to/from Art-Net). */
    const val STYLE_NODE: Byte = 0x00

    /** Controller (Art-Net controller). */
    const val STYLE_CONTROLLER: Byte = 0x01
}

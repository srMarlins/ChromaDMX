package com.chromadmx.networking.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArtNetCodecTest {

    // ------------------------------------------------------------------ //
    //  Header validation                                                  //
    // ------------------------------------------------------------------ //

    @Test
    fun hasValidHeader_validPacket_returnsTrue() {
        val packet = byteArrayOf(
            0x41, 0x72, 0x74, 0x2D, 0x4E, 0x65, 0x74, 0x00,
            0x00, 0x00 // dummy opcode
        )
        assertTrue(ArtNetCodec.hasValidHeader(packet))
    }

    @Test
    fun hasValidHeader_invalidPacket_returnsFalse() {
        val packet = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        assertTrue(!ArtNetCodec.hasValidHeader(packet))
    }

    @Test
    fun hasValidHeader_tooShort_returnsFalse() {
        val packet = byteArrayOf(0x41, 0x72, 0x74)
        assertTrue(!ArtNetCodec.hasValidHeader(packet))
    }

    // ------------------------------------------------------------------ //
    //  OpCode reading                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun readOpCode_artDmx_returns0x5000() {
        val packet = byteArrayOf(
            0x41, 0x72, 0x74, 0x2D, 0x4E, 0x65, 0x74, 0x00, // header
            0x00, 0x50 // OpCode 0x5000 LE
        )
        assertEquals(0x5000, ArtNetCodec.readOpCode(packet))
    }

    @Test
    fun readOpCode_artPoll_returns0x2000() {
        val packet = byteArrayOf(
            0x41, 0x72, 0x74, 0x2D, 0x4E, 0x65, 0x74, 0x00,
            0x00, 0x20 // OpCode 0x2000 LE
        )
        assertEquals(0x2000, ArtNetCodec.readOpCode(packet))
    }

    @Test
    fun readOpCode_artPollReply_returns0x2100() {
        val packet = byteArrayOf(
            0x41, 0x72, 0x74, 0x2D, 0x4E, 0x65, 0x74, 0x00,
            0x00, 0x21 // OpCode 0x2100 LE
        )
        assertEquals(0x2100, ArtNetCodec.readOpCode(packet))
    }

    // ------------------------------------------------------------------ //
    //  ArtDmx encode                                                      //
    // ------------------------------------------------------------------ //

    @Test
    fun encodeArtDmx_headerIsCorrect() {
        val data = ByteArray(512)
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 1,
            physical = 0,
            universe = 0,
            data = data
        )

        // First 8 bytes: "Art-Net\0"
        assertEquals(0x41, packet[0].toInt() and 0xFF)
        assertEquals(0x72, packet[1].toInt() and 0xFF)
        assertEquals(0x74, packet[2].toInt() and 0xFF)
        assertEquals(0x2D, packet[3].toInt() and 0xFF)
        assertEquals(0x4E, packet[4].toInt() and 0xFF)
        assertEquals(0x65, packet[5].toInt() and 0xFF)
        assertEquals(0x74, packet[6].toInt() and 0xFF)
        assertEquals(0x00, packet[7].toInt() and 0xFF)
    }

    @Test
    fun encodeArtDmx_opCodeIsLittleEndian() {
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 0,
            data = ByteArray(2)
        )
        // OpCode 0x5000 stored LE: low byte first
        assertEquals(0x00, packet[8].toInt() and 0xFF) // low byte
        assertEquals(0x50, packet[9].toInt() and 0xFF) // high byte
    }

    @Test
    fun encodeArtDmx_protVerIsBigEndian() {
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 0,
            data = ByteArray(2)
        )
        // ProtVer 14 (0x000E) stored BE: high byte first
        assertEquals(0x00, packet[10].toInt() and 0xFF) // high byte
        assertEquals(0x0E, packet[11].toInt() and 0xFF) // low byte (14)
    }

    @Test
    fun encodeArtDmx_sequenceAndPhysical() {
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 42,
            physical = 3,
            universe = 0,
            data = ByteArray(2)
        )
        assertEquals(42, packet[12].toInt() and 0xFF)
        assertEquals(3, packet[13].toInt() and 0xFF)
    }

    @Test
    fun encodeArtDmx_universe0() {
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 0,
            data = ByteArray(2)
        )
        assertEquals(0x00, packet[14].toInt() and 0xFF) // SubUni
        assertEquals(0x00, packet[15].toInt() and 0xFF) // Net
    }

    @Test
    fun encodeArtDmx_universe1() {
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 1,
            data = ByteArray(2)
        )
        assertEquals(0x01, packet[14].toInt() and 0xFF) // SubUni
        assertEquals(0x00, packet[15].toInt() and 0xFF) // Net
    }

    @Test
    fun encodeArtDmx_universe256() {
        // Universe 256 = Net 1, SubUni 0
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 256,
            data = ByteArray(2)
        )
        assertEquals(0x00, packet[14].toInt() and 0xFF) // SubUni
        assertEquals(0x01, packet[15].toInt() and 0xFF) // Net
    }

    @Test
    fun encodeArtDmx_universe32767() {
        // Max universe: 0x7FFF = Net 127, SubUni 255
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 32767,
            data = ByteArray(2)
        )
        assertEquals(0xFF.toByte(), packet[14]) // SubUni = 0xFF
        assertEquals(0x7F, packet[15].toInt() and 0xFF) // Net = 0x7F
    }

    @Test
    fun encodeArtDmx_lengthIsBigEndian() {
        val data = ByteArray(512)
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 0,
            data = data
        )
        // Length = 512 = 0x0200 BE
        assertEquals(0x02, packet[16].toInt() and 0xFF) // high
        assertEquals(0x00, packet[17].toInt() and 0xFF) // low
    }

    @Test
    fun encodeArtDmx_dataIsCopied() {
        val data = byteArrayOf(0xFF.toByte(), 0x80.toByte())
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 0,
            data = data
        )
        assertEquals(0xFF.toByte(), packet[18])
        assertEquals(0x80.toByte(), packet[19])
    }

    @Test
    fun encodeArtDmx_totalSize512() {
        val data = ByteArray(512)
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 0,
            data = data
        )
        assertEquals(ArtNetConstants.ART_DMX_MAX_SIZE, packet.size) // 18 + 512 = 530
    }

    @Test
    fun encodeArtDmx_oddLengthIsPadded() {
        val data = ByteArray(3) { it.toByte() }
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 0,
            physical = 0,
            universe = 0,
            data = data
        )
        // Length should be padded to 4 (even)
        assertEquals(0x00, packet[16].toInt() and 0xFF)
        assertEquals(0x04, packet[17].toInt() and 0xFF)
        assertEquals(18 + 4, packet.size)
    }

    // ------------------------------------------------------------------ //
    //  ArtDmx decode                                                      //
    // ------------------------------------------------------------------ //

    @Test
    fun decodeArtDmx_roundTrip() {
        val data = ByteArray(512) { (it % 256).toByte() }
        val encoded = ArtNetCodec.encodeArtDmx(
            sequence = 99,
            physical = 2,
            universe = 300,
            data = data
        )
        val decoded = ArtNetCodec.decodeArtDmx(encoded)
        assertNotNull(decoded)
        assertEquals(99.toByte(), decoded.sequence)
        assertEquals(2.toByte(), decoded.physical)
        assertEquals(300, decoded.universe)
        assertTrue(data.contentEquals(decoded.data))
    }

    @Test
    fun decodeArtDmx_knownByteSequence() {
        // Construct a known ArtDmx packet manually
        val packet = ByteArray(22) // 18 header + 4 data
        // Header
        byteArrayOf(0x41, 0x72, 0x74, 0x2D, 0x4E, 0x65, 0x74, 0x00).copyInto(packet, 0)
        // OpCode 0x5000 LE
        packet[8] = 0x00
        packet[9] = 0x50
        // ProtVer 14 BE
        packet[10] = 0x00
        packet[11] = 0x0E
        // Sequence = 5
        packet[12] = 0x05
        // Physical = 1
        packet[13] = 0x01
        // SubUni = 2
        packet[14] = 0x02
        // Net = 0
        packet[15] = 0x00
        // Length = 4 BE
        packet[16] = 0x00
        packet[17] = 0x04
        // Data
        packet[18] = 0xFF.toByte()
        packet[19] = 0x80.toByte()
        packet[20] = 0x40
        packet[21] = 0x20

        val decoded = ArtNetCodec.decodeArtDmx(packet)
        assertNotNull(decoded)
        assertEquals(5.toByte(), decoded.sequence)
        assertEquals(1.toByte(), decoded.physical)
        assertEquals(2, decoded.universe)
        assertEquals(4, decoded.data.size)
        assertEquals(0xFF.toByte(), decoded.data[0])
        assertEquals(0x80.toByte(), decoded.data[1])
        assertEquals(0x40.toByte(), decoded.data[2])
        assertEquals(0x20.toByte(), decoded.data[3])
    }

    @Test
    fun decodeArtDmx_tooShort_returnsNull() {
        assertNull(ArtNetCodec.decodeArtDmx(ByteArray(10)))
    }

    @Test
    fun decodeArtDmx_wrongOpCode_returnsNull() {
        val packet = ArtNetCodec.encodeArtPoll()
        assertNull(ArtNetCodec.decodeArtDmx(packet))
    }

    @Test
    fun decodeArtDmx_badHeader_returnsNull() {
        val data = ByteArray(530)
        // Don't write Art-Net header
        data[8] = 0x00
        data[9] = 0x50
        assertNull(ArtNetCodec.decodeArtDmx(data))
    }

    // ------------------------------------------------------------------ //
    //  ArtPoll encode / decode                                            //
    // ------------------------------------------------------------------ //

    @Test
    fun encodeArtPoll_size() {
        val packet = ArtNetCodec.encodeArtPoll()
        assertEquals(ArtNetConstants.ART_POLL_SIZE, packet.size) // 14 bytes
    }

    @Test
    fun encodeArtPoll_headerAndOpCode() {
        val packet = ArtNetCodec.encodeArtPoll()

        // Verify header
        for (i in 0 until 8) {
            assertEquals(ArtNetConstants.HEADER[i], packet[i])
        }

        // OpCode 0x2000 LE
        assertEquals(0x00, packet[8].toInt() and 0xFF)
        assertEquals(0x20, packet[9].toInt() and 0xFF)

        // ProtVer 14 BE
        assertEquals(0x00, packet[10].toInt() and 0xFF)
        assertEquals(0x0E, packet[11].toInt() and 0xFF)
    }

    @Test
    fun encodeArtPoll_flagsAndPriority() {
        val packet = ArtNetCodec.encodeArtPoll(
            flags = 0x06,
            diagPriority = 0x10
        )
        assertEquals(0x06, packet[12].toInt() and 0xFF)
        assertEquals(0x10, packet[13].toInt() and 0xFF)
    }

    @Test
    fun decodeArtPoll_roundTrip() {
        val encoded = ArtNetCodec.encodeArtPoll(flags = 0x02, diagPriority = 0x40)
        val decoded = ArtNetCodec.decodeArtPoll(encoded)
        assertNotNull(decoded)
        assertEquals(0x02.toByte(), decoded.flags)
        assertEquals(0x40.toByte(), decoded.diagPriority)
    }

    @Test
    fun decodeArtPoll_knownBytes() {
        val packet = byteArrayOf(
            0x41, 0x72, 0x74, 0x2D, 0x4E, 0x65, 0x74, 0x00, // header
            0x00, 0x20,                                         // OpCode 0x2000 LE
            0x00, 0x0E,                                         // ProtVer 14 BE
            0x06,                                               // Flags
            0x10                                                // DiagPriority
        )
        val decoded = ArtNetCodec.decodeArtPoll(packet)
        assertNotNull(decoded)
        assertEquals(0x06.toByte(), decoded.flags)
        assertEquals(0x10.toByte(), decoded.diagPriority)
    }

    @Test
    fun decodeArtPoll_tooShort_returnsNull() {
        assertNull(ArtNetCodec.decodeArtPoll(ByteArray(10)))
    }

    @Test
    fun decodeArtPoll_wrongOpCode_returnsNull() {
        val data = ByteArray(512)
        val packet = ArtNetCodec.encodeArtDmx(sequence = 0, physical = 0, universe = 0, data = data)
        assertNull(ArtNetCodec.decodeArtPoll(packet))
    }

    // ------------------------------------------------------------------ //
    //  ArtPollReply encode / decode                                       //
    // ------------------------------------------------------------------ //

    @Test
    fun encodeArtPollReply_size() {
        val packet = ArtNetCodec.encodeArtPollReply()
        assertEquals(ArtNetConstants.ART_POLL_REPLY_SIZE, packet.size) // 239 bytes
    }

    @Test
    fun encodeArtPollReply_headerAndOpCode() {
        val packet = ArtNetCodec.encodeArtPollReply()

        // Verify header
        for (i in 0 until 8) {
            assertEquals(ArtNetConstants.HEADER[i], packet[i])
        }

        // OpCode 0x2100 LE
        assertEquals(0x00, packet[8].toInt() and 0xFF)
        assertEquals(0x21, packet[9].toInt() and 0xFF)
    }

    @Test
    fun encodeArtPollReply_roundTrip() {
        val ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 100)
        val mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        val swIn = byteArrayOf(0, 1, 2, 3)
        val swOut = byteArrayOf(0, 1, 2, 3)

        val encoded = ArtNetCodec.encodeArtPollReply(
            ipAddress = ip,
            port = 6454,
            firmwareVersion = 0x0102,
            netSwitch = 0,
            subSwitch = 1,
            shortName = "TestNode",
            longName = "Test Node Long Name",
            numPorts = 4,
            swIn = swIn,
            swOut = swOut,
            style = ArtNetConstants.STYLE_NODE,
            macAddress = mac,
            bindIp = ip,
            status = 0x40
        )

        val decoded = ArtNetCodec.decodeArtPollReply(encoded)
        assertNotNull(decoded)

        // Check IP
        assertTrue(ip.contentEquals(decoded.ipAddress))
        assertEquals("192.168.1.100", decoded.ipString)

        // Check port
        assertEquals(6454, decoded.port)

        // Check firmware
        assertEquals(0x0102, decoded.firmwareVersion)

        // Check names
        assertEquals("TestNode", decoded.shortName)
        assertEquals("Test Node Long Name", decoded.longName)

        // Check net/sub
        assertEquals(0, decoded.netSwitch)
        assertEquals(1, decoded.subSwitch)

        // Check ports
        assertEquals(4, decoded.numPorts)

        // Check MAC
        assertEquals("aa:bb:cc:dd:ee:ff", decoded.macString)

        // Check style and status
        assertEquals(ArtNetConstants.STYLE_NODE, decoded.style)
        assertEquals(0x40.toByte(), decoded.status)
    }

    @Test
    fun decodeArtPollReply_tooShort_returnsNull() {
        assertNull(ArtNetCodec.decodeArtPollReply(ByteArray(100)))
    }

    @Test
    fun decodeArtPollReply_wrongOpCode_returnsNull() {
        val packet = ArtNetCodec.encodeArtPoll()
        // Pad to 239 bytes
        val padded = ByteArray(239)
        packet.copyInto(padded)
        assertNull(ArtNetCodec.decodeArtPollReply(padded))
    }

    // ------------------------------------------------------------------ //
    //  Known byte sequence (full ArtDmx verification)                     //
    // ------------------------------------------------------------------ //

    @Test
    fun encodeArtDmx_fullKnownPacket() {
        // Universe 1, sequence 10, physical 0, 4 bytes of data: FF 80 40 00
        val data = byteArrayOf(0xFF.toByte(), 0x80.toByte(), 0x40, 0x00)
        val packet = ArtNetCodec.encodeArtDmx(
            sequence = 10,
            physical = 0,
            universe = 1,
            data = data
        )

        // Verify each byte of the header portion
        val expected = byteArrayOf(
            // Header "Art-Net\0"
            0x41, 0x72, 0x74, 0x2D, 0x4E, 0x65, 0x74, 0x00,
            // OpCode 0x5000 LE
            0x00, 0x50,
            // ProtVer 14 BE
            0x00, 0x0E,
            // Sequence = 10
            0x0A,
            // Physical = 0
            0x00,
            // SubUni = 1
            0x01,
            // Net = 0
            0x00,
            // Length = 4 BE
            0x00, 0x04,
            // Data
            0xFF.toByte(), 0x80.toByte(), 0x40, 0x00
        )

        assertEquals(expected.size, packet.size)
        for (i in expected.indices) {
            assertEquals(
                expected[i],
                packet[i],
                "Mismatch at byte $i: expected 0x${(expected[i].toInt() and 0xFF).toString(16)} " +
                    "got 0x${(packet[i].toInt() and 0xFF).toString(16)}"
            )
        }
    }
}

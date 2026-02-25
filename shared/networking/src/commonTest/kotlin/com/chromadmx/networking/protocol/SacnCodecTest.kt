package com.chromadmx.networking.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SacnCodecTest {

    // ------------------------------------------------------------------ //
    //  Constants verification                                             //
    // ------------------------------------------------------------------ //

    @Test
    fun multicastAddress_universe1() {
        assertEquals("239.255.0.1", SacnConstants.multicastAddress(1))
    }

    @Test
    fun multicastAddress_universe256() {
        assertEquals("239.255.1.0", SacnConstants.multicastAddress(256))
    }

    @Test
    fun multicastAddress_universe63999() {
        // 63999 = 0xF9FF -> hi=0xF9, lo=0xFF
        assertEquals("239.255.249.255", SacnConstants.multicastAddress(63999))
    }

    @Test
    fun acnPacketIdentifier_isCorrect() {
        val expected = byteArrayOf(
            0x41, 0x53, 0x43, 0x2D,
            0x45, 0x31, 0x2E, 0x31,
            0x37, 0x00, 0x00, 0x00
        )
        assertTrue(expected.contentEquals(SacnConstants.ACN_PACKET_IDENTIFIER))
    }

    // ------------------------------------------------------------------ //
    //  Encode — structure verification                                    //
    // ------------------------------------------------------------------ //

    @Test
    fun encode_fullPacketSize_512slots() {
        val cid = ByteArray(16) { it.toByte() }
        val dmxData = ByteArray(512)
        val packet = SacnCodec.encode(
            cid = cid,
            sourceName = "TestSource",
            priority = 100,
            sequence = 0,
            universe = 1,
            dmxData = dmxData
        )
        assertEquals(SacnConstants.FULL_PACKET_SIZE, packet.size) // 638
    }

    @Test
    fun encode_preambleSize_isBigEndian0x0010() {
        val packet = SacnCodec.encode(cid = ByteArray(16))
        assertEquals(0x00, packet[0].toInt() and 0xFF)
        assertEquals(0x10, packet[1].toInt() and 0xFF)
    }

    @Test
    fun encode_postambleSize_isZero() {
        val packet = SacnCodec.encode(cid = ByteArray(16))
        assertEquals(0x00, packet[2].toInt() and 0xFF)
        assertEquals(0x00, packet[3].toInt() and 0xFF)
    }

    @Test
    fun encode_acnPacketIdentifier_isPresent() {
        val packet = SacnCodec.encode(cid = ByteArray(16))
        for (i in 0 until SacnConstants.ACN_ID_SIZE) {
            assertEquals(
                SacnConstants.ACN_PACKET_IDENTIFIER[i],
                packet[4 + i],
                "ACN identifier mismatch at byte $i"
            )
        }
    }

    @Test
    fun encode_rootLayerVector_is0x00000004() {
        val packet = SacnCodec.encode(cid = ByteArray(16))
        // Root layer vector starts at offset 18 (preamble=16 + flags+length=2)
        assertEquals(0x00, packet[18].toInt() and 0xFF)
        assertEquals(0x00, packet[19].toInt() and 0xFF)
        assertEquals(0x00, packet[20].toInt() and 0xFF)
        assertEquals(0x04, packet[21].toInt() and 0xFF)
    }

    @Test
    fun encode_cidIsEmbedded() {
        val cid = ByteArray(16) { (0xAA + it).toByte() }
        val packet = SacnCodec.encode(cid = cid)
        // CID at offset 22 (16 preamble + 2 flags+length + 4 vector)
        for (i in 0 until 16) {
            assertEquals(cid[i], packet[22 + i], "CID mismatch at byte $i")
        }
    }

    @Test
    fun encode_framingLayerVector_is0x00000002() {
        val packet = SacnCodec.encode(cid = ByteArray(16))
        // Framing layer starts at offset 38 (root layer size)
        // Vector at offset 38 + 2 (flags+length) = 40
        assertEquals(0x00, packet[40].toInt() and 0xFF)
        assertEquals(0x00, packet[41].toInt() and 0xFF)
        assertEquals(0x00, packet[42].toInt() and 0xFF)
        assertEquals(0x02, packet[43].toInt() and 0xFF)
    }

    @Test
    fun encode_sourceName_isNullTerminated() {
        val packet = SacnCodec.encode(
            cid = ByteArray(16),
            sourceName = "Hello"
        )
        // Source name at offset 44 (38 root + 2 flags + 4 vector)
        assertEquals('H'.code.toByte(), packet[44])
        assertEquals('e'.code.toByte(), packet[45])
        assertEquals('l'.code.toByte(), packet[46])
        assertEquals('l'.code.toByte(), packet[47])
        assertEquals('o'.code.toByte(), packet[48])
        assertEquals(0x00, packet[49].toInt() and 0xFF) // null terminator
    }

    @Test
    fun encode_priority_isCorrect() {
        val packet = SacnCodec.encode(
            cid = ByteArray(16),
            priority = 150
        )
        // Priority at offset 44 + 64 = 108
        assertEquals(150, packet[108].toInt() and 0xFF)
    }

    @Test
    fun encode_sequence_isCorrect() {
        val packet = SacnCodec.encode(
            cid = ByteArray(16),
            sequence = 42
        )
        // Sequence at offset 108 + 1 (priority) + 2 (reserved) = 111
        assertEquals(42, packet[111].toInt() and 0xFF)
    }

    @Test
    fun encode_options_isCorrect() {
        val packet = SacnCodec.encode(
            cid = ByteArray(16),
            options = SacnConstants.OPTION_PREVIEW
        )
        // Options at offset 112
        assertEquals(0x80, packet[112].toInt() and 0xFF)
    }

    @Test
    fun encode_universe_isBigEndian() {
        val packet = SacnCodec.encode(
            cid = ByteArray(16),
            universe = 300  // 0x012C
        )
        // Universe at offset 113-114
        assertEquals(0x01, packet[113].toInt() and 0xFF)
        assertEquals(0x2C, packet[114].toInt() and 0xFF)
    }

    @Test
    fun encode_dmpVector_is0x02() {
        val packet = SacnCodec.encode(cid = ByteArray(16))
        // DMP layer starts at offset 115
        // DMP vector at offset 115 + 2 = 117
        assertEquals(0x02, packet[117].toInt() and 0xFF)
    }

    @Test
    fun encode_dmpAddressType_is0xA1() {
        val packet = SacnCodec.encode(cid = ByteArray(16))
        assertEquals(0xA1.toByte(), packet[118])
    }

    @Test
    fun encode_startCode_isFirst_propertyValue() {
        val packet = SacnCodec.encode(
            cid = ByteArray(16),
            startCode = 0x00
        )
        // Property values start at offset 125 (after DMP header)
        assertEquals(0x00, packet[125].toInt() and 0xFF)
    }

    @Test
    fun encode_dmxData_followsStartCode() {
        val dmxData = ByteArray(512) { (it % 256).toByte() }
        val packet = SacnCodec.encode(
            cid = ByteArray(16),
            dmxData = dmxData
        )
        // DMX data starts at offset 126
        for (i in 0 until 512) {
            assertEquals(dmxData[i], packet[126 + i], "DMX data mismatch at slot $i")
        }
    }

    // ------------------------------------------------------------------ //
    //  Decode — round trip                                                //
    // ------------------------------------------------------------------ //

    @Test
    fun decode_roundTrip_fullPacket() {
        val cid = ByteArray(16) { (0x10 + it).toByte() }
        val dmxData = ByteArray(512) { (it % 256).toByte() }

        val encoded = SacnCodec.encode(
            cid = cid,
            sourceName = "ChromaDMX",
            priority = 120,
            sequence = 55,
            options = 0,
            universe = 42,
            startCode = 0x00,
            dmxData = dmxData
        )

        val decoded = SacnCodec.decode(encoded)
        assertNotNull(decoded)

        assertTrue(cid.contentEquals(decoded.cid))
        assertEquals("ChromaDMX", decoded.sourceName)
        assertEquals(120, decoded.priority)
        assertEquals(55, decoded.sequence)
        assertEquals(0, decoded.options)
        assertEquals(42, decoded.universe)
        assertEquals(0x00.toByte(), decoded.startCode)
        assertTrue(dmxData.contentEquals(decoded.dmxData))
    }

    @Test
    fun decode_roundTrip_smallPacket() {
        val cid = ByteArray(16)
        val dmxData = byteArrayOf(0xFF.toByte(), 0x80.toByte(), 0x40, 0x00)

        val encoded = SacnCodec.encode(
            cid = cid,
            sourceName = "Test",
            priority = 100,
            sequence = 0,
            universe = 1,
            dmxData = dmxData
        )

        val decoded = SacnCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(4, decoded.dmxData.size)
        assertEquals(0xFF.toByte(), decoded.dmxData[0])
        assertEquals(0x80.toByte(), decoded.dmxData[1])
        assertEquals(0x40.toByte(), decoded.dmxData[2])
        assertEquals(0x00.toByte(), decoded.dmxData[3])
    }

    @Test
    fun decode_roundTrip_highUniverse() {
        val encoded = SacnCodec.encode(
            cid = ByteArray(16),
            universe = 32000,
            dmxData = ByteArray(512)
        )
        val decoded = SacnCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(32000, decoded.universe)
    }

    @Test
    fun decode_roundTrip_options() {
        val encoded = SacnCodec.encode(
            cid = ByteArray(16),
            options = SacnConstants.OPTION_PREVIEW or SacnConstants.OPTION_STREAM_TERMINATED,
            dmxData = ByteArray(512)
        )
        val decoded = SacnCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(0xC0, decoded.options)
    }

    // ------------------------------------------------------------------ //
    //  Decode — error cases                                               //
    // ------------------------------------------------------------------ //

    @Test
    fun decode_tooShort_returnsNull() {
        assertNull(SacnCodec.decode(ByteArray(50)))
    }

    @Test
    fun decode_wrongPreamble_returnsNull() {
        val encoded = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))
        // Corrupt preamble
        encoded[0] = 0xFF.toByte()
        assertNull(SacnCodec.decode(encoded))
    }

    @Test
    fun decode_wrongAcnIdentifier_returnsNull() {
        val encoded = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))
        // Corrupt ACN identifier
        encoded[4] = 0x00
        assertNull(SacnCodec.decode(encoded))
    }

    @Test
    fun decode_wrongRootVector_returnsNull() {
        val encoded = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))
        // Corrupt root vector (at offset 18-21)
        encoded[21] = 0xFF.toByte()
        assertNull(SacnCodec.decode(encoded))
    }

    @Test
    fun decode_wrongFramingVector_returnsNull() {
        val encoded = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))
        // Corrupt framing vector (at offset 40-43)
        encoded[43] = 0xFF.toByte()
        assertNull(SacnCodec.decode(encoded))
    }

    // ------------------------------------------------------------------ //
    //  isValidPacket                                                       //
    // ------------------------------------------------------------------ //

    @Test
    fun isValidPacket_validPacket_returnsTrue() {
        val encoded = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))
        assertTrue(SacnCodec.isValidPacket(encoded))
    }

    @Test
    fun isValidPacket_tooShort_returnsFalse() {
        assertTrue(!SacnCodec.isValidPacket(ByteArray(10)))
    }

    @Test
    fun isValidPacket_wrongPreamble_returnsFalse() {
        val encoded = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))
        encoded[1] = 0xFF.toByte()
        assertTrue(!SacnCodec.isValidPacket(encoded))
    }

    // ------------------------------------------------------------------ //
    //  Known byte sequence verification                                   //
    // ------------------------------------------------------------------ //

    @Test
    fun encode_knownByteSequence_rootPreamble() {
        val packet = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))

        // Bytes 0-1: Preamble Size = 0x0010
        assertEquals(0x00, packet[0].toInt() and 0xFF)
        assertEquals(0x10, packet[1].toInt() and 0xFF)

        // Bytes 2-3: Postamble Size = 0x0000
        assertEquals(0x00, packet[2].toInt() and 0xFF)
        assertEquals(0x00, packet[3].toInt() and 0xFF)

        // Bytes 4-15: ACN Packet Identifier
        val expectedId = byteArrayOf(
            0x41, 0x53, 0x43, 0x2D,
            0x45, 0x31, 0x2E, 0x31,
            0x37, 0x00, 0x00, 0x00
        )
        for (i in expectedId.indices) {
            assertEquals(expectedId[i], packet[4 + i], "ACN ID mismatch at byte $i")
        }
    }

    @Test
    fun encode_flagsAndLength_haveCorrectFlagBits() {
        val packet = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))

        // Root layer flags+length at offset 16: high nibble should be 0x7
        val rootFH = packet[16].toInt() and 0xF0
        assertEquals(0x70, rootFH)

        // Framing layer flags+length at offset 38: high nibble should be 0x7
        val framingFH = packet[38].toInt() and 0xF0
        assertEquals(0x70, framingFH)

        // DMP layer flags+length at offset 115: high nibble should be 0x7
        val dmpFH = packet[115].toInt() and 0xF0
        assertEquals(0x70, dmpFH)
    }

    @Test
    fun encode_dmpLayerKnownValues() {
        val packet = SacnCodec.encode(cid = ByteArray(16), dmxData = ByteArray(512))

        // DMP Vector at offset 117: 0x02
        assertEquals(0x02, packet[117].toInt() and 0xFF)

        // Address Type at offset 118: 0xA1
        assertEquals(0xA1.toByte(), packet[118])

        // First Property Address at offset 119-120: 0x0000
        assertEquals(0x00, packet[119].toInt() and 0xFF)
        assertEquals(0x00, packet[120].toInt() and 0xFF)

        // Address Increment at offset 121-122: 0x0001
        assertEquals(0x00, packet[121].toInt() and 0xFF)
        assertEquals(0x01, packet[122].toInt() and 0xFF)

        // Property Value Count at offset 123-124: 513 (0x0201)
        assertEquals(0x02, packet[123].toInt() and 0xFF)
        assertEquals(0x01, packet[124].toInt() and 0xFF)
    }
}

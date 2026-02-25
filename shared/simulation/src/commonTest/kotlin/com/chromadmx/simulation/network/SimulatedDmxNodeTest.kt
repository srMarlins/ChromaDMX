package com.chromadmx.simulation.network

import com.chromadmx.networking.protocol.ArtNetCodec
import com.chromadmx.networking.protocol.ArtNetConstants
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimulatedDmxNodeTest {

    // ------------------------------------------------------------------ //
    //  ArtPoll → ArtPollReply                                             //
    // ------------------------------------------------------------------ //

    @Test
    fun processPacket_artPoll_sendsReply() = runTest {
        val bus = SimulatedNetworkBus()
        val nodeTransport = SimulatedNetwork()
        val controllerTransport = SimulatedNetwork()
        nodeTransport.connectToBus(bus)
        controllerTransport.connectToBus(bus)

        val node = SimulatedDmxNode(
            transport = nodeTransport,
            ipAddress = "192.168.1.100",
            shortName = "TestNode",
            longName = "Test Node for Unit Tests",
            universes = listOf(0),
            firmwareVersion = 0x0200
        )

        // Send ArtPoll from controller
        val pollPacket = ArtNetCodec.encodeArtPoll(flags = 0x02)
        // Directly process it on the node
        val handled = node.processPacket(pollPacket, "192.168.1.1")

        assertTrue(handled)
        assertEquals(1, node.pollCount)
        assertEquals(1, node.replyCount)

        // The reply should have been sent via the node's transport
        val sent = nodeTransport.sentPackets()
        assertEquals(1, sent.size)

        // Decode the reply
        val replyData = sent[0].data
        val reply = ArtNetCodec.decodeArtPollReply(replyData)
        assertNotNull(reply)
        assertEquals("192.168.1.100", reply.ipString)
        assertEquals("TestNode", reply.shortName)
        assertEquals("Test Node for Unit Tests", reply.longName)
        assertEquals(0x0200, reply.firmwareVersion)
        assertEquals(1, reply.numPorts)
    }

    @Test
    fun processPacket_artPoll_replyHasCorrectUniverse() = runTest {
        val transport = SimulatedNetwork()

        val node = SimulatedDmxNode(
            transport = transport,
            universes = listOf(5),
            shortName = "Uni5Node"
        )

        val pollPacket = ArtNetCodec.encodeArtPoll()
        node.processPacket(pollPacket, "10.0.0.1")

        val sent = transport.sentPackets()
        assertEquals(1, sent.size)

        val reply = ArtNetCodec.decodeArtPollReply(sent[0].data)
        assertNotNull(reply)

        // Universe 5 -> netSwitch=0, subSwitch=0, swOut[0]=5
        assertEquals(0, reply.netSwitch)
        assertEquals(0, reply.subSwitch)
        assertEquals(5, reply.swOut[0].toInt() and 0xFF)
    }

    @Test
    fun processPacket_artPoll_multipleUniverses() = runTest {
        val transport = SimulatedNetwork()

        val node = SimulatedDmxNode(
            transport = transport,
            universes = listOf(0, 1, 2),
            shortName = "MultiUni"
        )

        val pollPacket = ArtNetCodec.encodeArtPoll()
        node.processPacket(pollPacket, "10.0.0.1")

        val sent = transport.sentPackets()
        val reply = ArtNetCodec.decodeArtPollReply(sent[0].data)
        assertNotNull(reply)
        assertEquals(3, reply.numPorts)
    }

    // ------------------------------------------------------------------ //
    //  ArtDmx reception                                                   //
    // ------------------------------------------------------------------ //

    @Test
    fun processPacket_artDmx_storesChannelData() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(
            transport = transport,
            universes = listOf(0)
        )

        val dmxData = ByteArray(512) { (it % 256).toByte() }
        val dmxPacket = ArtNetCodec.encodeArtDmx(
            sequence = 1,
            physical = 0,
            universe = 0,
            data = dmxData
        )

        val handled = node.processPacket(dmxPacket, "192.168.1.1")
        assertTrue(handled)
        assertEquals(1, node.dmxCount)

        val received = node.getReceivedDmx(0)
        assertNotNull(received)
        assertEquals(512, received.size)
        // Check a few channel values
        assertEquals(0, received[0].toInt() and 0xFF)
        assertEquals(128, received[128].toInt() and 0xFF)
        assertEquals(255, received[255].toInt() and 0xFF)
    }

    @Test
    fun processPacket_artDmx_wrongUniverse_ignored() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(
            transport = transport,
            universes = listOf(0)
        )

        val dmxPacket = ArtNetCodec.encodeArtDmx(
            sequence = 1,
            physical = 0,
            universe = 5, // Node only handles universe 0
            data = ByteArray(512)
        )

        node.processPacket(dmxPacket, "192.168.1.1")
        assertEquals(0, node.dmxCount)
        assertNull(node.getReceivedDmx(5))
    }

    @Test
    fun processPacket_artDmx_multipleUniverses() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(
            transport = transport,
            universes = listOf(0, 1)
        )

        // Send data to universe 0
        val data0 = ByteArray(512) { 0xFF.toByte() }
        val packet0 = ArtNetCodec.encodeArtDmx(sequence = 1, physical = 0, universe = 0, data = data0)
        node.processPacket(packet0, "192.168.1.1")

        // Send data to universe 1
        val data1 = ByteArray(512) { 0x80.toByte() }
        val packet1 = ArtNetCodec.encodeArtDmx(sequence = 2, physical = 0, universe = 1, data = data1)
        node.processPacket(packet1, "192.168.1.1")

        assertEquals(2, node.dmxCount)

        // Check universe 0
        val received0 = node.getReceivedDmx(0)
        assertNotNull(received0)
        assertEquals(0xFF, received0[0].toInt() and 0xFF)

        // Check universe 1
        val received1 = node.getReceivedDmx(1)
        assertNotNull(received1)
        assertEquals(0x80, received1[0].toInt() and 0xFF)
    }

    @Test
    fun getChannelValue_returnsCorrectValue() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        val dmxData = ByteArray(512)
        dmxData[0] = 0xFF.toByte()
        dmxData[100] = 0x42
        dmxData[511] = 0xAA.toByte()

        val packet = ArtNetCodec.encodeArtDmx(sequence = 1, physical = 0, universe = 0, data = dmxData)
        node.processPacket(packet, "10.0.0.1")

        assertEquals(255, node.getChannelValue(0, 0))
        assertEquals(0x42, node.getChannelValue(0, 100))
        assertEquals(0xAA, node.getChannelValue(0, 511))
    }

    @Test
    fun getChannelValue_noData_returnsNull() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        assertNull(node.getChannelValue(0, 0))
    }

    @Test
    fun getColorAt_returnsRgbTriple() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        val dmxData = ByteArray(512)
        // Set RGB at channel 0: R=255, G=128, B=64
        dmxData[0] = 0xFF.toByte()
        dmxData[1] = 0x80.toByte()
        dmxData[2] = 0x40.toByte()

        val packet = ArtNetCodec.encodeArtDmx(sequence = 1, physical = 0, universe = 0, data = dmxData)
        node.processPacket(packet, "10.0.0.1")

        val color = node.getColorAt(0, 0)
        assertNotNull(color)
        assertEquals(255, color.first)
        assertEquals(128, color.second)
        assertEquals(64, color.third)
    }

    // ------------------------------------------------------------------ //
    //  Invalid packets                                                    //
    // ------------------------------------------------------------------ //

    @Test
    fun processPacket_invalidHeader_ignored() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        val handled = node.processPacket(ByteArray(100), "10.0.0.1")
        assertTrue(!handled)
        assertEquals(0, node.pollCount)
        assertEquals(0, node.dmxCount)
    }

    @Test
    fun processPacket_unknownOpCode_ignored() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        // Construct a packet with valid header but unknown opcode
        val packet = ByteArray(14)
        ArtNetConstants.HEADER.copyInto(packet)
        // OpCode 0x9999 LE
        packet[8] = 0x99.toByte()
        packet[9] = 0x99.toByte()

        val handled = node.processPacket(packet, "10.0.0.1")
        assertTrue(!handled)
    }

    // ------------------------------------------------------------------ //
    //  Reset                                                              //
    // ------------------------------------------------------------------ //

    @Test
    fun reset_clearsAllState() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        // Receive some data
        val poll = ArtNetCodec.encodeArtPoll()
        node.processPacket(poll, "10.0.0.1")

        val dmxPacket = ArtNetCodec.encodeArtDmx(sequence = 1, physical = 0, universe = 0, data = ByteArray(512))
        node.processPacket(dmxPacket, "10.0.0.1")

        assertEquals(1, node.pollCount)
        assertEquals(1, node.dmxCount)
        assertEquals(1, node.replyCount)

        node.reset()

        assertEquals(0, node.pollCount)
        assertEquals(0, node.dmxCount)
        assertEquals(0, node.replyCount)
        assertNull(node.getReceivedDmx(0))
    }

    // ------------------------------------------------------------------ //
    //  IP address parsing                                                 //
    // ------------------------------------------------------------------ //

    @Test
    fun parseIpAddress_validAddress() {
        val bytes = SimulatedDmxNode.parseIpAddress("192.168.1.100")
        assertEquals(4, bytes.size)
        assertEquals(192.toByte(), bytes[0])
        assertEquals(168.toByte(), bytes[1])
        assertEquals(1.toByte(), bytes[2])
        assertEquals(100.toByte(), bytes[3])
    }

    @Test
    fun parseIpAddress_loopback() {
        val bytes = SimulatedDmxNode.parseIpAddress("127.0.0.1")
        assertEquals(127.toByte(), bytes[0])
        assertEquals(0.toByte(), bytes[1])
        assertEquals(0.toByte(), bytes[2])
        assertEquals(1.toByte(), bytes[3])
    }

    // ------------------------------------------------------------------ //
    //  Start / Stop lifecycle                                             //
    // ------------------------------------------------------------------ //

    @Test
    fun startStop_lifecycle() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        assertTrue(!node.isRunning)
        node.start()
        assertTrue(node.isRunning)
        node.stop()
        assertTrue(!node.isRunning)
    }

    @Test
    fun start_idempotent() = runTest {
        val transport = SimulatedNetwork()
        val node = SimulatedDmxNode(transport = transport, universes = listOf(0))

        node.start()
        node.start() // Should not throw
        assertTrue(node.isRunning)
        node.stop()
    }

    // ------------------------------------------------------------------ //
    //  Integration: full round-trip via bus                                //
    // ------------------------------------------------------------------ //

    @Test
    fun fullRoundTrip_artPollAndReply_viaBus() = runTest {
        val bus = SimulatedNetworkBus()
        val controllerTransport = SimulatedNetwork()
        val nodeTransport = SimulatedNetwork()

        controllerTransport.connectToBus(bus)
        nodeTransport.connectToBus(bus)

        val node = SimulatedDmxNode(
            transport = nodeTransport,
            ipAddress = "10.0.0.50",
            shortName = "IntegNode",
            universes = listOf(0, 1)
        )

        // Controller sends ArtPoll via bus
        val pollData = ArtNetCodec.encodeArtPoll(flags = 0x02)
        controllerTransport.send(pollData, "255.255.255.255", ArtNetConstants.PORT)

        // Node receives the poll from bus
        val buffer = ByteArray(1024)
        val receivedPoll = nodeTransport.receive(buffer, timeoutMs = 1000)
        assertNotNull(receivedPoll)

        // Node processes the poll
        node.processPacket(receivedPoll.data, receivedPoll.address)

        // Controller receives the reply from bus
        val receivedReply = controllerTransport.receive(buffer, timeoutMs = 1000)
        assertNotNull(receivedReply)

        // Decode the reply on controller side
        val reply = ArtNetCodec.decodeArtPollReply(receivedReply.data)
        assertNotNull(reply)
        assertEquals("10.0.0.50", reply.ipString)
        assertEquals("IntegNode", reply.shortName)
        assertEquals(2, reply.numPorts)
    }

    @Test
    fun fullRoundTrip_artDmx_viaBus() = runTest {
        val bus = SimulatedNetworkBus()
        val controllerTransport = SimulatedNetwork()
        val nodeTransport = SimulatedNetwork()

        controllerTransport.connectToBus(bus)
        nodeTransport.connectToBus(bus)

        val node = SimulatedDmxNode(
            transport = nodeTransport,
            universes = listOf(0)
        )

        // Controller sends DMX data
        val dmxData = ByteArray(512)
        dmxData[0] = 0xFF.toByte()
        dmxData[1] = 0x80.toByte()
        dmxData[2] = 0x40.toByte()

        val dmxPacket = ArtNetCodec.encodeArtDmx(
            sequence = 1,
            physical = 0,
            universe = 0,
            data = dmxData
        )
        controllerTransport.send(dmxPacket, "10.0.0.50", ArtNetConstants.PORT)

        // Node receives the DMX from bus
        val buffer = ByteArray(1024)
        val received = nodeTransport.receive(buffer, timeoutMs = 1000)
        assertNotNull(received)

        // Node processes the DMX
        node.processPacket(received.data, received.address)

        // Verify stored DMX data
        assertEquals(1, node.dmxCount)
        val color = node.getColorAt(0, 0)
        assertNotNull(color)
        assertEquals(255, color.first)
        assertEquals(128, color.second)
        assertEquals(64, color.third)
    }
}

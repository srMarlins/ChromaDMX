package com.chromadmx.simulation.network

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimulatedNetworkTest {

    // ------------------------------------------------------------------ //
    //  Basic loopback                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun send_loopback_receivesOwnPacket() = runTest {
        val network = SimulatedNetwork()
        val testData = byteArrayOf(1, 2, 3, 4, 5)

        network.send(testData, "127.0.0.1", 6454)

        val buffer = ByteArray(1024)
        val received = network.receive(buffer, timeoutMs = 1000)

        assertNotNull(received)
        assertEquals("127.0.0.1", received.address)
        assertEquals(6454, received.port)
        assertEquals(5, received.data.size)
        assertTrue(testData.contentEquals(received.data))
    }

    @Test
    fun send_multiplePackets_receivedInOrder() = runTest {
        val network = SimulatedNetwork()

        network.send(byteArrayOf(1), "10.0.0.1", 6454)
        network.send(byteArrayOf(2), "10.0.0.1", 6454)
        network.send(byteArrayOf(3), "10.0.0.1", 6454)

        val buffer = ByteArray(1024)
        val p1 = network.receive(buffer, timeoutMs = 1000)
        val p2 = network.receive(buffer, timeoutMs = 1000)
        val p3 = network.receive(buffer, timeoutMs = 1000)

        assertNotNull(p1)
        assertNotNull(p2)
        assertNotNull(p3)
        assertEquals(1.toByte(), p1.data[0])
        assertEquals(2.toByte(), p2.data[0])
        assertEquals(3.toByte(), p3.data[0])
    }

    @Test
    fun receive_timeout_returnsNull() = runTest {
        val network = SimulatedNetwork()
        val buffer = ByteArray(1024)

        val received = network.receive(buffer, timeoutMs = 50)
        assertNull(received)
    }

    // ------------------------------------------------------------------ //
    //  Packet loss                                                        //
    // ------------------------------------------------------------------ //

    @Test
    fun packetLoss_fullLoss_dropsEverything() = runTest {
        val network = SimulatedNetwork(packetLossRate = 1.0f)

        network.send(byteArrayOf(1), "10.0.0.1", 6454)
        network.send(byteArrayOf(2), "10.0.0.1", 6454)

        val buffer = ByteArray(1024)
        val received = network.receive(buffer, timeoutMs = 100)
        assertNull(received)
        assertEquals(0, network.sentPacketCount())
    }

    @Test
    fun packetLoss_noLoss_deliversAll() = runTest {
        val network = SimulatedNetwork(packetLossRate = 0.0f)

        network.send(byteArrayOf(1), "10.0.0.1", 6454)
        network.send(byteArrayOf(2), "10.0.0.1", 6454)

        val buffer = ByteArray(1024)
        val p1 = network.receive(buffer, timeoutMs = 1000)
        val p2 = network.receive(buffer, timeoutMs = 1000)

        assertNotNull(p1)
        assertNotNull(p2)
        assertEquals(2, network.sentPacketCount())
    }

    @Test
    fun packetLoss_seeded_deterministicBehavior() = runTest {
        // Use a seeded random so we get deterministic results
        val random = Random(42)
        val network = SimulatedNetwork(packetLossRate = 0.5f, random = random)

        // Send many packets and check that some are dropped
        for (i in 0 until 100) {
            network.send(byteArrayOf(i.toByte()), "10.0.0.1", 6454)
        }

        val sentCount = network.sentPacketCount()
        // With 50% loss and 100 sends, we expect roughly 50 sent
        // With a seeded random, this is deterministic
        assertTrue(sentCount in 30..70, "Expected roughly 50 packets, got $sentCount")
    }

    // ------------------------------------------------------------------ //
    //  Sent packets history                                               //
    // ------------------------------------------------------------------ //

    @Test
    fun sentPackets_tracksAllSent() = runTest {
        val network = SimulatedNetwork()

        network.send(byteArrayOf(0xAA.toByte()), "10.0.0.1", 6454)
        network.send(byteArrayOf(0xBB.toByte()), "10.0.0.2", 6455)

        val sent = network.sentPackets()
        assertEquals(2, sent.size)
        assertEquals("10.0.0.1", sent[0].address)
        assertEquals("10.0.0.2", sent[1].address)
    }

    @Test
    fun clearSentPackets_resetsHistory() = runTest {
        val network = SimulatedNetwork()

        network.send(byteArrayOf(1), "10.0.0.1", 6454)
        assertEquals(1, network.sentPacketCount())

        network.clearSentPackets()
        assertEquals(0, network.sentPacketCount())
    }

    // ------------------------------------------------------------------ //
    //  Network bus (multi-transport)                                      //
    // ------------------------------------------------------------------ //

    @Test
    fun bus_routesToOtherTransport() = runTest {
        val bus = SimulatedNetworkBus()
        val controller = SimulatedNetwork()
        val node = SimulatedNetwork()

        controller.connectToBus(bus)
        bus.register(node)

        // Controller sends, node should receive
        controller.send(byteArrayOf(0x42), "255.255.255.255", 6454)

        val buffer = ByteArray(1024)
        val received = node.receive(buffer, timeoutMs = 1000)

        assertNotNull(received)
        assertEquals(0x42.toByte(), received.data[0])
    }

    @Test
    fun bus_doesNotEchoToSender() = runTest {
        val bus = SimulatedNetworkBus()
        val controller = SimulatedNetwork()

        controller.connectToBus(bus)

        // Controller sends but should NOT receive its own packet via bus
        controller.send(byteArrayOf(0x42), "255.255.255.255", 6454)

        val buffer = ByteArray(1024)
        val received = controller.receive(buffer, timeoutMs = 100)
        assertNull(received)
    }

    @Test
    fun bus_multipleNodes_allReceive() = runTest {
        val bus = SimulatedNetworkBus()
        val controller = SimulatedNetwork()
        val node1 = SimulatedNetwork()
        val node2 = SimulatedNetwork()

        controller.connectToBus(bus)
        bus.register(node1)
        bus.register(node2)

        controller.send(byteArrayOf(0xFF.toByte()), "255.255.255.255", 6454)

        val buffer = ByteArray(1024)
        val received1 = node1.receive(buffer, timeoutMs = 1000)
        val received2 = node2.receive(buffer, timeoutMs = 1000)

        assertNotNull(received1)
        assertNotNull(received2)
        assertEquals(0xFF.toByte(), received1.data[0])
        assertEquals(0xFF.toByte(), received2.data[0])
    }

    @Test
    fun bus_bidirectional_nodeCanReplyToController() = runTest {
        val bus = SimulatedNetworkBus()
        val controller = SimulatedNetwork()
        val node = SimulatedNetwork()

        controller.connectToBus(bus)
        node.connectToBus(bus)

        // Controller sends poll
        controller.send(byteArrayOf(0x01), "255.255.255.255", 6454)

        // Node receives poll
        val buffer = ByteArray(1024)
        val poll = node.receive(buffer, timeoutMs = 1000)
        assertNotNull(poll)

        // Node replies
        node.send(byteArrayOf(0x02), "192.168.1.1", 6454)

        // Controller receives reply
        val reply = controller.receive(buffer, timeoutMs = 1000)
        assertNotNull(reply)
        assertEquals(0x02.toByte(), reply.data[0])
    }

    // ------------------------------------------------------------------ //
    //  Close behavior                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun close_stopsReceiving() = runTest {
        val network = SimulatedNetwork()
        network.send(byteArrayOf(1), "10.0.0.1", 6454)
        network.close()

        val buffer = ByteArray(1024)
        val received = network.receive(buffer, timeoutMs = 100)
        assertNull(received)
    }
}

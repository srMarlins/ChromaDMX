package com.chromadmx.networking.discovery

import com.chromadmx.core.model.DmxNode
import com.chromadmx.networking.model.UdpPacket
import com.chromadmx.networking.protocol.ArtNetCodec
import com.chromadmx.networking.protocol.ArtNetConstants
import com.chromadmx.networking.transport.PlatformUdpTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeDiscoveryTest {

    // Use a real transport instance (tests only call processReply, not the network)
    private val transport = PlatformUdpTransport()

    @Test
    fun processReply_validReply_addsNode() {
        val discovery = NodeDiscovery(transport)

        val replyBytes = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 50),
            port = 6454,
            firmwareVersion = 0x0100,
            netSwitch = 0,
            subSwitch = 0,
            shortName = "PixelBar",
            longName = "ChromaDMX Pixel Bar 8ch",
            numPorts = 1,
            swIn = byteArrayOf(0, 0, 0, 0),
            swOut = byteArrayOf(0, 0, 0, 0),
            style = ArtNetConstants.STYLE_NODE,
            macAddress = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte(), 0x00, 0x01)
        )

        val node = discovery.processReply(replyBytes, currentTimeMs = 1000L)
        assertNotNull(node)
        assertEquals("192.168.1.50", node.ipAddress)
        assertEquals("PixelBar", node.shortName)
        assertEquals("ChromaDMX Pixel Bar 8ch", node.longName)
        assertEquals("de:ad:be:ef:00:01", node.macAddress)
        assertEquals(1, node.numPorts)
        assertEquals(0x0100, node.firmwareVersion)

        // Should be in the registry
        assertEquals(1, discovery.nodes.value.size)
        assertTrue(discovery.nodes.value.containsKey(node.nodeKey))
    }

    @Test
    fun processReply_invalidPacket_returnsNull() {
        val discovery = NodeDiscovery(transport)
        val result = discovery.processReply(ByteArray(10), currentTimeMs = 1000L)
        assertNull(result)
        assertEquals(0, discovery.nodes.value.size)
    }

    @Test
    fun processReply_multipleNodes_allTracked() {
        val discovery = NodeDiscovery(transport)

        // Node 1
        val reply1 = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 10),
            macAddress = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
            shortName = "Node1"
        )
        discovery.processReply(reply1, 1000L)

        // Node 2
        val reply2 = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 20),
            macAddress = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F),
            shortName = "Node2"
        )
        discovery.processReply(reply2, 1000L)

        assertEquals(2, discovery.nodes.value.size)
        assertEquals(2, discovery.nodeList.size)
    }

    @Test
    fun processReply_sameNodeUpdated() {
        val discovery = NodeDiscovery(transport)
        val mac = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)

        // First reply
        val reply1 = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 10),
            macAddress = mac,
            shortName = "OldName"
        )
        discovery.processReply(reply1, 1000L)
        assertEquals(1, discovery.nodes.value.size)

        // Second reply from same MAC
        val reply2 = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 10),
            macAddress = mac,
            shortName = "NewName"
        )
        discovery.processReply(reply2, 2000L)
        assertEquals(1, discovery.nodes.value.size) // Still 1 node

        val node = discovery.nodeList.first()
        assertEquals("NewName", node.shortName)
        assertEquals(2000L, node.lastSeenMs)
    }

    @Test
    fun pruneStaleNodes_removesOldNodes() {
        val discovery = NodeDiscovery(transport, nodeTimeoutMs = 5_000L)

        // Add node at time 1000
        val reply = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 10),
            macAddress = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
            shortName = "OldNode"
        )
        discovery.processReply(reply, 1000L)
        assertEquals(1, discovery.nodes.value.size)

        // Prune at time 7000 (>5000ms timeout from 1000)
        discovery.pruneStaleNodes(7000L)
        assertEquals(0, discovery.nodes.value.size)
    }

    @Test
    fun pruneStaleNodes_keepsRecentNodes() {
        val discovery = NodeDiscovery(transport, nodeTimeoutMs = 5_000L)

        val reply = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 10),
            macAddress = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
            shortName = "RecentNode"
        )
        discovery.processReply(reply, 1000L)

        // Prune at time 3000 (only 2000ms since last seen, < 5000ms timeout)
        discovery.pruneStaleNodes(3000L)
        assertEquals(1, discovery.nodes.value.size)
    }

    @Test
    fun processReply_extractsUniverses() {
        val discovery = NodeDiscovery(transport)

        val reply = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(10, 0, 0, 1),
            macAddress = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()),
            netSwitch = 0,
            subSwitch = 0,
            numPorts = 2,
            swOut = byteArrayOf(0, 1, 0, 0)
        )

        val node = discovery.processReply(reply, 1000L)
        assertNotNull(node)
        assertEquals(2, node.universes.size)
        assertEquals(0, node.universes[0]) // net=0, sub=0, swOut=0
        assertEquals(1, node.universes[1]) // net=0, sub=0, swOut=1
    }

    @Test
    fun stop_clearsRegistry() {
        val discovery = NodeDiscovery(transport)

        val reply = ArtNetCodec.encodeArtPollReply(
            ipAddress = byteArrayOf(192.toByte(), 168.toByte(), 1, 10),
            macAddress = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
            shortName = "Node"
        )
        discovery.processReply(reply, 1000L)
        assertEquals(1, discovery.nodes.value.size)

        discovery.stop()
        assertEquals(0, discovery.nodes.value.size)
    }

    @Test
    fun dmxNode_isAlive_withinTimeout() {
        val node = DmxNode(
            ipAddress = "192.168.1.1",
            lastSeenMs = 5000L
        )
        assertTrue(node.isAlive(currentTimeMs = 8000L, timeoutMs = 10000L))
    }

    @Test
    fun dmxNode_isAlive_expired() {
        val node = DmxNode(
            ipAddress = "192.168.1.1",
            lastSeenMs = 1000L
        )
        assertTrue(!node.isAlive(currentTimeMs = 20000L, timeoutMs = 10000L))
    }

    @Test
    fun dmxNode_nodeKey_prefersMac() {
        val node = DmxNode(
            ipAddress = "192.168.1.1",
            macAddress = "aa:bb:cc:dd:ee:ff"
        )
        assertEquals("aa:bb:cc:dd:ee:ff", node.nodeKey)
    }

    @Test
    fun dmxNode_nodeKey_fallsBackToIp() {
        val node = DmxNode(
            ipAddress = "192.168.1.1",
            macAddress = ""
        )
        assertEquals("192.168.1.1", node.nodeKey)
    }
}

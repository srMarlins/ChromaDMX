package com.chromadmx.networking.discovery

import com.chromadmx.networking.protocol.ArtNetCodec
import com.chromadmx.networking.transport.PlatformUdpTransport
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeDiscoverySecurityTest {

    private val transport = PlatformUdpTransport()

    @AfterTest
    fun tearDown() {
        transport.close()
    }

    @Test
    fun processReply_respectsMaxNodes() {
        val maxNodes = 10
        val discovery = NodeDiscovery(transport, maxNodes = maxNodes)

        // Simulate receiving 100 unique nodes
        for (i in 1..100) {
            val ip = byteArrayOf(10, 0, 0, i.toByte())
            val mac = byteArrayOf(0, 0, 0, 0, 0, i.toByte())
            val reply = ArtNetCodec.encodeArtPollReply(
                ipAddress = ip,
                macAddress = mac,
                shortName = "Node-$i"
            )
            discovery.processReply(reply, currentTimeMs = i.toLong())
        }

        // Should be limited to maxNodes
        assertEquals(maxNodes, discovery.nodes.value.size)

        // The last 10 nodes (91 to 100) should be present
        for (i in 91..100) {
            val macStr = "00:00:00:00:00:${i.toString(16).padStart(2, '0')}"
            assertTrue(discovery.nodes.value.containsKey(macStr), "Should contain node $i")
        }

        // Node 1 should have been evicted
        val mac1Str = "00:00:00:00:00:01"
        assertTrue(!discovery.nodes.value.containsKey(mac1Str), "Should NOT contain node 1")
    }
}

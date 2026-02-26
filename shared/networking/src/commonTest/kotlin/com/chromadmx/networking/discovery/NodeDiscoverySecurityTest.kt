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
        val startTime = 1000L

        // Simulate receiving 100 unique nodes, spread out over time
        // We use increments of 10 seconds to ensure previous nodes become stale enough to be evicted
        for (i in 1..100) {
            val ip = byteArrayOf(10, 0, 0, i.toByte())
            val mac = byteArrayOf(0, 0, 0, 0, 0, i.toByte())
            val reply = ArtNetCodec.encodeArtPollReply(
                ipAddress = ip,
                macAddress = mac,
                shortName = "Node-$i"
            )
            // Increment time by 10s per node (well above the 5s degraded threshold)
            val time = startTime + (i * 10_000L)
            discovery.processReply(reply, currentTimeMs = time)
        }

        // Should be limited to maxNodes
        assertEquals(maxNodes, discovery.nodes.value.size)

        // The last 10 nodes (91 to 100) should be present
        for (i in 91..100) {
            val macStr = "00:00:00:00:00:${i.toString(16).padStart(2, '0')}"
            assertTrue(discovery.nodes.value.containsKey(macStr), "Should contain node $i")
        }

        // Node 1 should have been evicted (it was seen very long ago)
        val mac1Str = "00:00:00:00:00:01"
        assertTrue(!discovery.nodes.value.containsKey(mac1Str), "Should NOT contain node 1")
    }

    @Test
    fun processReply_protectsHealthyNodes_fromFlood() {
        val maxNodes = 10
        val discovery = NodeDiscovery(transport, maxNodes = maxNodes)
        val startTime = 1000L

        // 1. Fill the registry with 10 healthy nodes
        for (i in 1..maxNodes) {
            val ip = byteArrayOf(10, 0, 0, i.toByte())
            val mac = byteArrayOf(0, 0, 0, 0, 0, i.toByte())
            val reply = ArtNetCodec.encodeArtPollReply(
                ipAddress = ip,
                macAddress = mac,
                shortName = "HealthyNode-$i"
            )
            // Seen at startTime
            discovery.processReply(reply, currentTimeMs = startTime)
        }

        assertEquals(maxNodes, discovery.nodes.value.size)

        // 2. Simulate an attack: A new node tries to join shortly after (1 second later)
        // Since the existing nodes are fresh (latency < 5s), they should NOT be evicted.
        val attackTime = startTime + 1000L
        val attackIp = byteArrayOf(10, 0, 0, 99)
        val attackMac = byteArrayOf(0, 0, 0, 0, 0, 99)
        val attackReply = ArtNetCodec.encodeArtPollReply(
            ipAddress = attackIp,
            macAddress = attackMac,
            shortName = "AttackerNode"
        )
        discovery.processReply(attackReply, currentTimeMs = attackTime)

        // 3. Verify that the registry size is still maxNodes
        assertEquals(maxNodes, discovery.nodes.value.size)

        // 4. Verify that the attacker was REJECTED
        // (It should not be in the map because no existing node was stale enough to evict)
        val attackerKey = "00:00:00:00:00:63" // hex for 99
        assertTrue(!discovery.nodes.value.containsKey(attackerKey), "Attacker node should have been rejected")

        // 5. Verify that original nodes are still there
        val node1Key = "00:00:00:00:00:01"
        assertTrue(discovery.nodes.value.containsKey(node1Key), "Healthy node 1 should still be present")
    }
}

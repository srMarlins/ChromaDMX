package com.chromadmx.ui.components.network

import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.protocol.ArtNetCodec
import com.chromadmx.networking.protocol.ArtNetConstants
import com.chromadmx.networking.transport.PlatformUdpTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkHealthViewModelTest {

    private fun makeDiscovery() = NodeDiscovery(PlatformUdpTransport())

    /** Helper to inject a node via a valid ArtPollReply packet. */
    private fun NodeDiscovery.injectNode(
        ip: ByteArray,
        shortName: String,
        mac: ByteArray,
        numPorts: Int = 1,
        lastSeenMs: Long,
    ) {
        val reply = ArtNetCodec.encodeArtPollReply(
            ipAddress = ip,
            port = ArtNetConstants.PORT,
            firmwareVersion = 0x0100,
            netSwitch = 0,
            subSwitch = 0,
            shortName = shortName,
            longName = shortName,
            numPorts = numPorts,
            swIn = byteArrayOf(0, 0, 0, 0),
            swOut = byteArrayOf(0, 0, 0, 0),
            style = ArtNetConstants.STYLE_NODE,
            macAddress = mac,
        )
        processReply(reply, lastSeenMs)
    }

    @Test
    fun noNodes_shows_no_nodes_summary() = runTest {
        val discovery = makeDiscovery()
        val vm = NetworkHealthViewModel(
            nodeDiscovery = discovery,
            mascotViewModel = null,
            scope = backgroundScope,
        )
        vm.refresh(currentTimeMs = 1000L)

        assertEquals("No nodes", vm.healthySummary.value)
        assertEquals(emptyList(), vm.nodes.value)
        assertFalse(vm.hasAlert.value)
    }

    @Test
    fun singleHealthyNode_shows_1_of_1_online() = runTest {
        val discovery = makeDiscovery()
        discovery.injectNode(
            ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 10),
            shortName = "Par1",
            mac = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x01),
            lastSeenMs = 9000L,
        )

        val vm = NetworkHealthViewModel(
            nodeDiscovery = discovery,
            mascotViewModel = null,
            scope = backgroundScope,
        )
        // Current time = 10000, lastSeen = 9000 => elapsed 1s => HEALTHY
        vm.refresh(currentTimeMs = 10_000L)

        assertEquals("1/1 online", vm.healthySummary.value)
        assertEquals(1, vm.nodes.value.size)
        assertEquals(NodeHealth.HEALTHY, vm.nodes.value[0].health)
        assertFalse(vm.hasAlert.value)
    }

    @Test
    fun degradedNode_counted_as_not_healthy() = runTest {
        val discovery = makeDiscovery()
        discovery.injectNode(
            ip = byteArrayOf(10, 0, 0, 1),
            shortName = "Mover1",
            mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0x01),
            lastSeenMs = 1000L,
        )

        val vm = NetworkHealthViewModel(
            nodeDiscovery = discovery,
            mascotViewModel = null,
            scope = backgroundScope,
        )
        // Current time = 8000, lastSeen = 1000 => elapsed 7s => DEGRADED
        vm.refresh(currentTimeMs = 8_000L)

        assertEquals("0/1 online", vm.healthySummary.value)
        assertEquals(NodeHealth.DEGRADED, vm.nodes.value[0].health)
        assertFalse(vm.hasAlert.value)
    }

    @Test
    fun lostNode_triggers_alert() = runTest {
        val discovery = makeDiscovery()
        discovery.injectNode(
            ip = byteArrayOf(10, 0, 0, 2),
            shortName = "Strobe1",
            mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0x02),
            lastSeenMs = 1000L,
        )

        val vm = NetworkHealthViewModel(
            nodeDiscovery = discovery,
            mascotViewModel = null,
            scope = backgroundScope,
        )
        // Current time = 20000, lastSeen = 1000 => elapsed 19s => LOST
        vm.refresh(currentTimeMs = 20_000L)

        assertEquals("0/1 online", vm.healthySummary.value)
        assertEquals(NodeHealth.LOST, vm.nodes.value[0].health)
        assertTrue(vm.hasAlert.value)
    }

    @Test
    fun mixedNodes_summary_counts_only_healthy() = runTest {
        val discovery = makeDiscovery()
        // Node A: healthy (lastSeen = 9500, current = 10000 => 500ms)
        discovery.injectNode(
            ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 1),
            shortName = "NodeA",
            mac = byteArrayOf(0x01, 0x01, 0x01, 0x01, 0x01, 0x01),
            lastSeenMs = 9500L,
        )
        // Node B: degraded (lastSeen = 3000, current = 10000 => 7s)
        discovery.injectNode(
            ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 2),
            shortName = "NodeB",
            mac = byteArrayOf(0x02, 0x02, 0x02, 0x02, 0x02, 0x02),
            lastSeenMs = 3000L,
        )
        // Node C: lost (lastSeen = 0, current = 10000 => 10s... wait that's degraded)
        // Let's make lastSeen = -10000 to get lost at currentTime 10000
        // Actually DmxNode lastSeenMs is Long, so use 0 and currentTime 20000
        // But we need all three at the same "current time". Let's use currentTime = 20000.
        // Node A: lastSeen 19500 => 500ms => healthy
        // Node B: lastSeen 13000 => 7s => degraded
        // Node C: lastSeen 1000 => 19s => lost
        discovery.injectNode(
            ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 3),
            shortName = "NodeC",
            mac = byteArrayOf(0x03, 0x03, 0x03, 0x03, 0x03, 0x03),
            lastSeenMs = 1000L,
        )

        // Re-inject A and B with adjusted times for our chosen currentTime
        discovery.injectNode(
            ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 1),
            shortName = "NodeA",
            mac = byteArrayOf(0x01, 0x01, 0x01, 0x01, 0x01, 0x01),
            lastSeenMs = 19500L,
        )
        discovery.injectNode(
            ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 2),
            shortName = "NodeB",
            mac = byteArrayOf(0x02, 0x02, 0x02, 0x02, 0x02, 0x02),
            lastSeenMs = 13000L,
        )

        val vm = NetworkHealthViewModel(
            nodeDiscovery = discovery,
            mascotViewModel = null,
            scope = backgroundScope,
        )
        vm.refresh(currentTimeMs = 20_000L)

        assertEquals("1/3 online", vm.healthySummary.value)
        assertTrue(vm.hasAlert.value) // NodeC is lost
    }
}

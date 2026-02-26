package com.chromadmx.simulation.network

import com.chromadmx.networking.model.DmxNode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedDiscoveryTest {

    // ------------------------------------------------------------------ //
    //  startScan emits nodes                                              //
    // ------------------------------------------------------------------ //

    @Test
    fun startScan_emitsNodesWithStaggeredTiming() = runTest {
        val discovery = SimulatedDiscovery(
            baseDelayMs = 10,
            perNodeDelayMs = 10,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        assertEquals(0, discovery.discoveredNodes.value.size)

        discovery.startScan()
        assertTrue(discovery.isScanning.value)

        // Advance past all delays: base(10) + node0(10) + node1(20) + node2(30)
        advanceUntilIdle()

        val nodes = discovery.discoveredNodes.value
        assertEquals(3, nodes.size)

        discovery.stopScan()
    }

    @Test
    fun startScan_nodesHavePlausibleData() = runTest {
        val discovery = SimulatedDiscovery(
            baseDelayMs = 10,
            perNodeDelayMs = 10,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        discovery.startScan()
        advanceUntilIdle()

        val nodes = discovery.discoveredNodes.value
        assertTrue(nodes.isNotEmpty())

        for (node in nodes) {
            assertTrue(node.ipAddress.isNotEmpty())
            assertTrue(node.shortName.isNotEmpty())
            assertTrue(node.lastSeenMs > 0)
            assertTrue(node.firstSeenMs > 0)
            assertTrue(node.latencyMs >= 0)
        }

        discovery.stopScan()
    }

    @Test
    fun startScan_nodesAppearIncrementally() = runTest {
        val discovery = SimulatedDiscovery(
            baseDelayMs = 50,
            perNodeDelayMs = 100,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        discovery.startScan()

        // After baseDelay(50) + first node delay(100) = 150ms
        advanceTimeBy(160)
        assertTrue(discovery.discoveredNodes.value.size >= 1)

        // Wait for all nodes: 50 + 100 + 200 + 300 = 650ms total
        advanceUntilIdle()
        assertEquals(3, discovery.discoveredNodes.value.size)

        discovery.stopScan()
    }

    // ------------------------------------------------------------------ //
    //  stopScan                                                           //
    // ------------------------------------------------------------------ //

    @Test
    fun stopScan_haltsFurtherEmission() = runTest {
        val discovery = SimulatedDiscovery(
            baseDelayMs = 100,
            perNodeDelayMs = 200,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        discovery.startScan()
        // Stop immediately before nodes can appear
        discovery.stopScan()

        assertFalse(discovery.isScanning.value)

        // Even advancing time should not yield nodes
        advanceUntilIdle()
        assertEquals(0, discovery.discoveredNodes.value.size)
    }

    @Test
    fun stopScan_setsIsScanningFalse() = runTest {
        val discovery = SimulatedDiscovery(
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )
        discovery.startScan()
        assertTrue(discovery.isScanning.value)

        discovery.stopScan()
        assertFalse(discovery.isScanning.value)
    }

    // ------------------------------------------------------------------ //
    //  isScanning transitions                                             //
    // ------------------------------------------------------------------ //

    @Test
    fun isScanning_transitionsCorrectly() = runTest {
        val discovery = SimulatedDiscovery(
            baseDelayMs = 10,
            perNodeDelayMs = 10,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        // Initially not scanning
        assertFalse(discovery.isScanning.value)

        // Scanning after startScan
        discovery.startScan()
        assertTrue(discovery.isScanning.value)

        // Wait for scan to complete naturally
        advanceUntilIdle()
        assertFalse(discovery.isScanning.value)
    }

    // ------------------------------------------------------------------ //
    //  Custom nodes                                                       //
    // ------------------------------------------------------------------ //

    @Test
    fun customNodes_emitsConfiguredNodes() = runTest {
        val customNodes = listOf(
            DmxNode(ipAddress = "10.0.0.1", shortName = "Custom-1"),
            DmxNode(ipAddress = "10.0.0.2", shortName = "Custom-2"),
        )

        val discovery = SimulatedDiscovery(
            nodes = customNodes,
            baseDelayMs = 10,
            perNodeDelayMs = 10,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        discovery.startScan()
        advanceUntilIdle()

        val discovered = discovery.discoveredNodes.value
        assertEquals(2, discovered.size)
        assertEquals("10.0.0.1", discovered[0].ipAddress)
        assertEquals("Custom-2", discovered[1].shortName)

        discovery.stopScan()
    }

    // ------------------------------------------------------------------ //
    //  Restart scan                                                       //
    // ------------------------------------------------------------------ //

    @Test
    fun startScan_afterStop_resetsAndRestartsDiscovery() = runTest {
        val discovery = SimulatedDiscovery(
            baseDelayMs = 10,
            perNodeDelayMs = 10,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        // First scan
        discovery.startScan()
        advanceUntilIdle()
        assertEquals(3, discovery.discoveredNodes.value.size)

        // Second scan should reset
        discovery.startScan()
        assertEquals(0, discovery.discoveredNodes.value.size)
        assertTrue(discovery.isScanning.value)

        advanceUntilIdle()
        assertEquals(3, discovery.discoveredNodes.value.size)

        discovery.stopScan()
    }

    // ------------------------------------------------------------------ //
    //  Default nodes                                                      //
    // ------------------------------------------------------------------ //

    @Test
    fun defaultNodes_returnsThreeNodes() {
        val nodes = SimulatedDiscovery.defaultNodes()
        assertEquals(3, nodes.size)
        assertEquals("SimNode-1", nodes[0].shortName)
        assertEquals("SimNode-2", nodes[1].shortName)
        assertEquals("SimNode-3", nodes[2].shortName)
    }
}

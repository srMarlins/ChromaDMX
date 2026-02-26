package com.chromadmx.networking

import com.chromadmx.networking.model.DmxNode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FixtureDiscoveryRouterTest {

    // ------------------------------------------------------------------ //
    //  Fake discovery for tracking calls                                  //
    // ------------------------------------------------------------------ //

    private class FakeFixtureDiscovery : FixtureDiscovery {
        val startScanCalls = mutableListOf<Unit>()
        val stopScanCalls = mutableListOf<Unit>()
        private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
        override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes
        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning

        override fun startScan() {
            startScanCalls.add(Unit)
            _isScanning.value = true
        }

        override fun stopScan() {
            stopScanCalls.add(Unit)
            _isScanning.value = false
        }

        fun setNodes(nodes: List<DmxNode>) {
            _discoveredNodes.value = nodes
        }

        fun setScanning(scanning: Boolean) {
            _isScanning.value = scanning
        }
    }

    private val nodeA = DmxNode(ipAddress = "192.168.1.10", shortName = "Node-A")
    private val nodeB = DmxNode(ipAddress = "192.168.1.20", shortName = "Node-B")
    private val simNodeC = DmxNode(ipAddress = "10.0.0.1", shortName = "SimNode-C")

    // ------------------------------------------------------------------ //
    //  Real mode                                                          //
    // ------------------------------------------------------------------ //

    @Test
    fun realMode_startScan_delegatesToRealDiscovery() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val router = FixtureDiscoveryRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Real)
        router.startScan()

        assertEquals(1, real.startScanCalls.size)
        assertEquals(0, sim.startScanCalls.size)
    }

    @Test
    fun realMode_discoveredNodes_reflectsRealDiscovery() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val scope = TestScope()
        val router = FixtureDiscoveryRouter(real, sim, scope)

        router.switchTo(TransportMode.Real)
        val nodes = router.discoveredNodes
        scope.advanceUntilIdle()

        real.setNodes(listOf(nodeA, nodeB))
        scope.advanceUntilIdle()

        assertEquals(2, router.discoveredNodes.value.size)
        assertEquals("Node-A", router.discoveredNodes.value[0].shortName)
    }

    @Test
    fun realMode_isScanning_reflectsRealDiscovery() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val scope = TestScope()
        val router = FixtureDiscoveryRouter(real, sim, scope)

        router.switchTo(TransportMode.Real)
        val scanning = router.isScanning
        scope.advanceUntilIdle()

        assertFalse(router.isScanning.value)

        real.setScanning(true)
        scope.advanceUntilIdle()

        assertTrue(router.isScanning.value)
    }

    // ------------------------------------------------------------------ //
    //  Simulated mode                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun simulatedMode_startScan_delegatesToSimulatedDiscovery() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val router = FixtureDiscoveryRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Simulated)
        router.startScan()

        assertEquals(0, real.startScanCalls.size)
        assertEquals(1, sim.startScanCalls.size)
    }

    @Test
    fun simulatedMode_discoveredNodes_reflectsSimulatedDiscovery() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val scope = TestScope()
        val router = FixtureDiscoveryRouter(real, sim, scope)

        router.switchTo(TransportMode.Simulated)
        val nodes = router.discoveredNodes
        scope.advanceUntilIdle()

        sim.setNodes(listOf(simNodeC))
        scope.advanceUntilIdle()

        assertEquals(1, router.discoveredNodes.value.size)
        assertEquals("SimNode-C", router.discoveredNodes.value[0].shortName)
    }

    // ------------------------------------------------------------------ //
    //  Mixed mode                                                         //
    // ------------------------------------------------------------------ //

    @Test
    fun mixedMode_startScan_startsBoth() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val router = FixtureDiscoveryRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Mixed)
        router.startScan()

        assertEquals(1, real.startScanCalls.size)
        assertEquals(1, sim.startScanCalls.size)
    }

    @Test
    fun mixedMode_discoveredNodes_mergesBothSources() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val scope = TestScope()
        val router = FixtureDiscoveryRouter(real, sim, scope)

        router.switchTo(TransportMode.Mixed)
        val nodes = router.discoveredNodes
        scope.advanceUntilIdle()

        real.setNodes(listOf(nodeA))
        sim.setNodes(listOf(simNodeC))
        scope.advanceUntilIdle()

        assertEquals(2, router.discoveredNodes.value.size)
        val names = router.discoveredNodes.value.map { it.shortName }
        assertTrue("Node-A" in names)
        assertTrue("SimNode-C" in names)
    }

    @Test
    fun mixedMode_isScanning_trueIfEitherScanning() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val scope = TestScope()
        val router = FixtureDiscoveryRouter(real, sim, scope)

        router.switchTo(TransportMode.Mixed)
        val scanning = router.isScanning
        scope.advanceUntilIdle()

        assertFalse(router.isScanning.value)

        real.setScanning(true)
        scope.advanceUntilIdle()
        assertTrue(router.isScanning.value)
    }

    // ------------------------------------------------------------------ //
    //  Stop                                                               //
    // ------------------------------------------------------------------ //

    @Test
    fun stopScan_stopsBoth() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val router = FixtureDiscoveryRouter(real, sim, TestScope())

        router.stopScan()

        assertEquals(1, real.stopScanCalls.size)
        assertEquals(1, sim.stopScanCalls.size)
    }

    // ------------------------------------------------------------------ //
    //  Mode switching                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun switchMode_changesActiveDiscovery() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val router = FixtureDiscoveryRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Real)
        router.startScan()
        assertEquals(1, real.startScanCalls.size)
        assertEquals(0, sim.startScanCalls.size)

        router.switchTo(TransportMode.Simulated)
        router.startScan()
        assertEquals(1, real.startScanCalls.size)
        assertEquals(1, sim.startScanCalls.size)
    }

    @Test
    fun mode_defaultsToReal() = runTest {
        val real = FakeFixtureDiscovery()
        val sim = FakeFixtureDiscovery()
        val router = FixtureDiscoveryRouter(real, sim, TestScope())

        assertEquals(TransportMode.Real, router.mode.value)
    }
}

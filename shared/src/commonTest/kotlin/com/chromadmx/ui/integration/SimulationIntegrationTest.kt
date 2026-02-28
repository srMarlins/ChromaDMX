package com.chromadmx.ui.integration

import com.chromadmx.core.model.DmxNode
import com.chromadmx.networking.ConnectionState
import com.chromadmx.networking.DmxTransport
import com.chromadmx.networking.DmxTransportRouter
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.networking.FixtureDiscoveryRouter
import com.chromadmx.networking.TransportMode
import com.chromadmx.simulation.network.SimulatedDiscovery
import com.chromadmx.simulation.network.SimulatedTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the simulation subsystem:
 * router-based transport/discovery switching, simulated frame capture,
 * and mid-session mode switching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimulationIntegrationTest {

    // -- Fake real transport (does nothing, tracks calls) --

    private class FakeRealTransport : DmxTransport {
        private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
        override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
        override var isRunning: Boolean = false
            private set

        val sentFrames = mutableListOf<Pair<Int, ByteArray>>()

        override fun start() {
            isRunning = true
            _connectionState.value = ConnectionState.Connected
        }

        override fun stop() {
            isRunning = false
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun sendFrame(universe: Int, channels: ByteArray) {
            sentFrames.add(universe to channels.copyOf())
        }

        override fun updateFrame(universeData: Map<Int, ByteArray>) {
            universeData.forEach { (u, ch) -> sentFrames.add(u to ch.copyOf()) }
        }
    }

    private class FakeRealDiscovery : FixtureDiscovery {
        private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
        override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

        override fun startScan() { _isScanning.value = true }
        override fun stopScan() { _isScanning.value = false }

        fun emitNodes(nodes: List<DmxNode>) { _discoveredNodes.value = nodes }
    }

    // -- Tests --

    @Test
    fun routerStartsInRealMode() = runTest {
        val real = FakeRealTransport()
        val simulated = SimulatedTransport(UnconfinedTestDispatcher(testScheduler))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        val router = DmxTransportRouter(real, simulated, scope)

        assertEquals(TransportMode.Real, router.mode.value)
    }

    @Test
    fun switchToSimulatedRoutesFramesToSimTransport() = runTest {
        val real = FakeRealTransport()
        val simulated = SimulatedTransport(UnconfinedTestDispatcher(testScheduler))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        val router = DmxTransportRouter(real, simulated, scope)
        router.start()
        advanceUntilIdle()

        // Initially in Real mode — frames go to real
        val testChannels = ByteArray(512) { if (it < 3) 128.toByte() else 0 }
        router.sendFrame(0, testChannels)
        assertEquals(1, real.sentFrames.size)
        assertNull(simulated.getChannelValue(0, 0))

        // Switch to Simulated
        router.switchTo(TransportMode.Simulated)
        advanceUntilIdle()
        assertEquals(TransportMode.Simulated, router.mode.value)

        // Frames now go to simulated
        router.sendFrame(0, testChannels)
        assertEquals(128, simulated.getChannelValue(0, 0))
        // Real should still only have 1 frame from before the switch
        assertEquals(1, real.sentFrames.size)
    }

    @Test
    fun mixedModeSendsToRealAndSimulated() = runTest {
        val real = FakeRealTransport()
        val simulated = SimulatedTransport(UnconfinedTestDispatcher(testScheduler))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        val router = DmxTransportRouter(real, simulated, scope)
        router.start()
        router.switchTo(TransportMode.Mixed)
        advanceUntilIdle()

        val testChannels = ByteArray(3) { 200.toByte() }
        router.sendFrame(1, testChannels)

        // Both should receive the frame
        assertEquals(1, real.sentFrames.size)
        assertEquals(200, simulated.getChannelValue(1, 0))
    }

    @Test
    fun simulatedTransportCapturesFrameData() = runTest {
        val simulated = SimulatedTransport(UnconfinedTestDispatcher(testScheduler))
        simulated.start()
        advanceUntilIdle()

        assertEquals(ConnectionState.Connected, simulated.connectionState.first())

        // Send frame data
        val channels = ByteArray(512) { (it % 256).toByte() }
        simulated.sendFrame(0, channels)

        // Verify captured data
        assertNotNull(simulated.getUniverseData(0))
        assertEquals(0, simulated.getChannelValue(0, 0))
        assertEquals(1, simulated.getChannelValue(0, 1))
        assertEquals(255, simulated.getChannelValue(0, 255))

        // Reset clears everything
        simulated.reset()
        assertNull(simulated.getUniverseData(0))
    }

    @Test
    fun simulatedDiscoveryEmitsNodesStaggered() = runTest {
        val discovery = SimulatedDiscovery(
            keepAliveIntervalMs = 0,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )

        discovery.startScan()
        assertTrue(discovery.isScanning.value)

        // Before base delay: no nodes yet
        assertEquals(0, discovery.discoveredNodes.value.size)

        // After base delay + first per-node delay: 1 node
        advanceTimeBy(150 + 80 + 1)
        assertEquals(1, discovery.discoveredNodes.value.size)
        assertEquals("SimNode-1", discovery.discoveredNodes.value[0].shortName)

        // After second per-node delay: 2 nodes
        advanceTimeBy(80)
        assertEquals(2, discovery.discoveredNodes.value.size)

        // After third per-node delay: 3 nodes, scanning complete
        advanceTimeBy(80)
        assertEquals(3, discovery.discoveredNodes.value.size)
        assertFalse(discovery.isScanning.value)
    }

    @Test
    fun discoveryRouterSwitchesToSimulated() = runTest {
        val realDiscovery = FakeRealDiscovery()
        val simDiscovery = SimulatedDiscovery(
            keepAliveIntervalMs = 0,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        val router = FixtureDiscoveryRouter(realDiscovery, simDiscovery, scope)

        // Start in Real mode
        assertEquals(TransportMode.Real, router.mode.value)

        // Switch to Simulated and scan
        router.switchTo(TransportMode.Simulated)
        router.startScan()
        advanceUntilIdle()

        // Should have 3 default simulated nodes
        assertEquals(3, router.discoveredNodes.value.size)
        assertTrue(router.discoveredNodes.value.all { it.shortName.startsWith("SimNode") })
    }

    @Test
    fun midSessionSwitchPreservesSimulatedState() = runTest {
        val real = FakeRealTransport()
        val simulated = SimulatedTransport(UnconfinedTestDispatcher(testScheduler))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        val router = DmxTransportRouter(real, simulated, scope)

        // Start in Real mode, send some frames
        router.start()
        advanceUntilIdle()
        router.sendFrame(0, ByteArray(3) { 100.toByte() })
        assertEquals(1, real.sentFrames.size)

        // Switch to Simulated mid-session
        router.switchTo(TransportMode.Simulated)
        advanceUntilIdle()

        assertTrue(simulated.isRunning, "Simulated transport should be running after switch")
        assertFalse(real.isRunning, "Real transport should be stopped after switch to simulated")

        // Send frame in simulated mode
        router.sendFrame(0, ByteArray(3) { 50.toByte() })
        assertEquals(50, simulated.getChannelValue(0, 0))

        // Real transport frames unchanged (still just the 1 from before)
        assertEquals(1, real.sentFrames.size)
    }

    @Test
    fun discoveryRouterMixedModeMergesNodes() = runTest {
        val realDiscovery = FakeRealDiscovery()
        val simDiscovery = SimulatedDiscovery(
            keepAliveIntervalMs = 0,
            coroutineContext = UnconfinedTestDispatcher(testScheduler),
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        val router = FixtureDiscoveryRouter(realDiscovery, simDiscovery, scope)

        // Switch to Mixed mode
        router.switchTo(TransportMode.Mixed)

        // Emit real nodes
        realDiscovery.emitNodes(listOf(
            DmxNode(
                ipAddress = "10.0.0.1",
                macAddress = "aa:bb:cc:dd:ee:01",
                shortName = "RealNode-1",
                longName = "Real Art-Net Node 1",
                numPorts = 1,
                universes = listOf(0),
            )
        ))

        // Scan simulated
        router.startScan()
        advanceUntilIdle()

        // Should have 4 nodes total (1 real + 3 simulated)
        val nodes = router.discoveredNodes.value
        assertEquals(4, nodes.size)
        assertTrue(nodes.any { it.shortName == "RealNode-1" })
        assertTrue(nodes.any { it.shortName == "SimNode-1" })
    }

    @Test
    fun connectionStateReflectsActiveMode() = runTest {
        val real = FakeRealTransport()
        val simulated = SimulatedTransport(UnconfinedTestDispatcher(testScheduler))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        val router = DmxTransportRouter(real, simulated, scope)

        // Before start: disconnected
        assertEquals(ConnectionState.Disconnected, router.connectionState.value)

        // Start in Real mode
        router.start()
        advanceUntilIdle()
        assertEquals(ConnectionState.Connected, router.connectionState.value)

        // Switch to Simulated — simulated starts, real stops
        router.switchTo(TransportMode.Simulated)
        advanceUntilIdle()
        assertEquals(ConnectionState.Connected, router.connectionState.value)

        // Stop everything
        router.stop()
        assertEquals(ConnectionState.Disconnected, router.connectionState.value)
    }
}

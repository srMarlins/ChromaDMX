package com.chromadmx.networking

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
class DmxTransportRouterTest {

    // ------------------------------------------------------------------ //
    //  Fake transport for tracking calls                                  //
    // ------------------------------------------------------------------ //

    private class FakeDmxTransport : DmxTransport {
        val startCalls = mutableListOf<Unit>()
        val stopCalls = mutableListOf<Unit>()
        val sentFrames = mutableListOf<Pair<Int, ByteArray>>()
        val updatedFrames = mutableListOf<Map<Int, ByteArray>>()
        private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
        override val connectionState: StateFlow<ConnectionState> = _connectionState
        override var isRunning = false
            private set

        override fun start() {
            startCalls.add(Unit)
            isRunning = true
            _connectionState.value = ConnectionState.Connected
        }

        override fun stop() {
            stopCalls.add(Unit)
            isRunning = false
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun sendFrame(universe: Int, channels: ByteArray) {
            sentFrames.add(universe to channels)
        }

        override fun updateFrame(universeData: Map<Int, ByteArray>) {
            updatedFrames.add(universeData)
        }

        fun setConnectionState(state: ConnectionState) {
            _connectionState.value = state
        }
    }

    // ------------------------------------------------------------------ //
    //  Real mode                                                          //
    // ------------------------------------------------------------------ //

    @Test
    fun realMode_start_delegatesToRealTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Real)
        router.start()

        assertEquals(1, real.startCalls.size)
        assertEquals(0, sim.startCalls.size)
    }

    @Test
    fun realMode_sendFrame_delegatesToRealTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Real)
        val data = byteArrayOf(1, 2, 3)
        router.sendFrame(0, data)

        assertEquals(1, real.sentFrames.size)
        assertEquals(0, sim.sentFrames.size)
        assertEquals(0, real.sentFrames[0].first)
    }

    @Test
    fun realMode_updateFrame_delegatesToRealTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Real)
        val data = mapOf(0 to byteArrayOf(1, 2, 3))
        router.updateFrame(data)

        assertEquals(1, real.updatedFrames.size)
        assertEquals(0, sim.updatedFrames.size)
    }

    @Test
    fun realMode_isRunning_reflectsRealTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Real)
        assertFalse(router.isRunning)

        real.start()
        assertTrue(router.isRunning)
    }

    // ------------------------------------------------------------------ //
    //  Simulated mode                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun simulatedMode_start_delegatesToSimulatedTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Simulated)
        router.start()

        assertEquals(0, real.startCalls.size)
        assertEquals(1, sim.startCalls.size)
    }

    @Test
    fun simulatedMode_sendFrame_delegatesToSimulatedTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Simulated)
        router.sendFrame(1, byteArrayOf(10, 20))

        assertEquals(0, real.sentFrames.size)
        assertEquals(1, sim.sentFrames.size)
        assertEquals(1, sim.sentFrames[0].first)
    }

    @Test
    fun simulatedMode_isRunning_reflectsSimulatedTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Simulated)
        assertFalse(router.isRunning)

        sim.start()
        assertTrue(router.isRunning)
    }

    // ------------------------------------------------------------------ //
    //  Mixed mode                                                         //
    // ------------------------------------------------------------------ //

    @Test
    fun mixedMode_start_startsBothTransports() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Mixed)
        router.start()

        assertEquals(1, real.startCalls.size)
        assertEquals(1, sim.startCalls.size)
    }

    @Test
    fun mixedMode_sendFrame_sendsToBoth() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Mixed)
        router.sendFrame(0, byteArrayOf(42))

        assertEquals(1, real.sentFrames.size)
        assertEquals(1, sim.sentFrames.size)
    }

    @Test
    fun mixedMode_updateFrame_sendsToBoth() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Mixed)
        val data = mapOf(0 to byteArrayOf(1))
        router.updateFrame(data)

        assertEquals(1, real.updatedFrames.size)
        assertEquals(1, sim.updatedFrames.size)
    }

    @Test
    fun mixedMode_isRunning_trueIfEitherRunning() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Mixed)
        assertFalse(router.isRunning)

        real.start()
        assertTrue(router.isRunning)
    }

    // ------------------------------------------------------------------ //
    //  Stop                                                               //
    // ------------------------------------------------------------------ //

    @Test
    fun stop_stopsBothTransports() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        real.start()
        sim.start()
        router.stop()

        assertEquals(1, real.stopCalls.size)
        assertEquals(1, sim.stopCalls.size)
        assertFalse(real.isRunning)
        assertFalse(sim.isRunning)
    }

    // ------------------------------------------------------------------ //
    //  Mode switching                                                     //
    // ------------------------------------------------------------------ //

    @Test
    fun switchMode_changesActiveTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        router.switchTo(TransportMode.Real)
        router.sendFrame(0, byteArrayOf(1))
        assertEquals(1, real.sentFrames.size)
        assertEquals(0, sim.sentFrames.size)

        router.switchTo(TransportMode.Simulated)
        router.sendFrame(0, byteArrayOf(2))
        assertEquals(1, real.sentFrames.size)
        assertEquals(1, sim.sentFrames.size)
    }

    @Test
    fun mode_defaultsToReal() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val router = DmxTransportRouter(real, sim, TestScope())

        assertEquals(TransportMode.Real, router.mode.value)
    }

    // ------------------------------------------------------------------ //
    //  Connection state                                                   //
    // ------------------------------------------------------------------ //

    @Test
    fun connectionState_realMode_reflectsRealTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val scope = TestScope()
        val router = DmxTransportRouter(real, sim, scope)

        router.switchTo(TransportMode.Real)
        // Access lazy property to trigger collection
        val state = router.connectionState
        scope.advanceUntilIdle()

        real.setConnectionState(ConnectionState.Connected)
        scope.advanceUntilIdle()

        assertEquals(ConnectionState.Connected, router.connectionState.value)
    }

    @Test
    fun connectionState_simulatedMode_reflectsSimulatedTransport() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val scope = TestScope()
        val router = DmxTransportRouter(real, sim, scope)

        router.switchTo(TransportMode.Simulated)
        val state = router.connectionState
        scope.advanceUntilIdle()

        sim.setConnectionState(ConnectionState.Connected)
        scope.advanceUntilIdle()

        assertEquals(ConnectionState.Connected, router.connectionState.value)
    }

    @Test
    fun connectionState_mixedMode_connectedIfEitherConnected() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val scope = TestScope()
        val router = DmxTransportRouter(real, sim, scope)

        router.switchTo(TransportMode.Mixed)
        val state = router.connectionState
        scope.advanceUntilIdle()

        real.setConnectionState(ConnectionState.Connected)
        scope.advanceUntilIdle()

        assertEquals(ConnectionState.Connected, router.connectionState.value)
    }

    @Test
    fun connectionState_mixedMode_disconnectedIfBothDisconnected() = runTest {
        val real = FakeDmxTransport()
        val sim = FakeDmxTransport()
        val scope = TestScope()
        val router = DmxTransportRouter(real, sim, scope)

        router.switchTo(TransportMode.Mixed)
        val state = router.connectionState
        scope.advanceUntilIdle()

        assertEquals(ConnectionState.Disconnected, router.connectionState.value)
    }
}

package com.chromadmx.simulation.network

import com.chromadmx.networking.ConnectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedTransportTest {

    // ------------------------------------------------------------------ //
    //  Start / Stop lifecycle                                             //
    // ------------------------------------------------------------------ //

    @Test
    fun start_setsIsRunningTrue() = runTest {
        val transport = SimulatedTransport(coroutineContext = UnconfinedTestDispatcher(testScheduler))
        assertFalse(transport.isRunning)

        transport.start()
        assertTrue(transport.isRunning)

        transport.stop()
    }

    @Test
    fun start_transitionsToConnected() = runTest {
        val transport = SimulatedTransport(coroutineContext = UnconfinedTestDispatcher(testScheduler))
        assertEquals(ConnectionState.Disconnected, transport.connectionState.value)

        transport.start()
        advanceUntilIdle()

        assertEquals(ConnectionState.Connected, transport.connectionState.value)

        transport.stop()
    }

    @Test
    fun stop_transitionsToDisconnected() = runTest {
        val transport = SimulatedTransport(coroutineContext = UnconfinedTestDispatcher(testScheduler))
        transport.start()
        advanceUntilIdle()

        transport.stop()
        assertFalse(transport.isRunning)
        assertEquals(ConnectionState.Disconnected, transport.connectionState.value)
    }

    @Test
    fun start_idempotent() = runTest {
        val transport = SimulatedTransport(coroutineContext = UnconfinedTestDispatcher(testScheduler))
        transport.start()
        transport.start() // Should not throw
        assertTrue(transport.isRunning)
        transport.stop()
    }

    // ------------------------------------------------------------------ //
    //  sendFrame                                                          //
    // ------------------------------------------------------------------ //

    @Test
    fun sendFrame_capturesData() {
        val transport = SimulatedTransport()
        val data = byteArrayOf(255.toByte(), 128.toByte(), 0)

        transport.sendFrame(0, data)

        val universe0 = transport.getUniverseData(0)
        assertNotNull(universe0)
        assertEquals(3, universe0.size)
        assertEquals(255.toByte(), universe0[0])
        assertEquals(128.toByte(), universe0[1])
        assertEquals(0.toByte(), universe0[2])
    }

    @Test
    fun sendFrame_multipleUniverses_accumulates() {
        val transport = SimulatedTransport()

        transport.sendFrame(0, byteArrayOf(10))
        transport.sendFrame(1, byteArrayOf(20))

        assertEquals(2, transport.lastFrame.value.size)
        assertNotNull(transport.getUniverseData(0))
        assertNotNull(transport.getUniverseData(1))
    }

    @Test
    fun sendFrame_copiesInputData() {
        val transport = SimulatedTransport()
        val data = byteArrayOf(100)

        transport.sendFrame(0, data)
        // Mutate original
        data[0] = 0

        // Captured copy should be unchanged
        assertEquals(100.toByte(), transport.getUniverseData(0)!![0])
    }

    // ------------------------------------------------------------------ //
    //  updateFrame                                                        //
    // ------------------------------------------------------------------ //

    @Test
    fun updateFrame_replacesAllData() {
        val transport = SimulatedTransport()

        // First set two universes
        transport.sendFrame(0, byteArrayOf(1))
        transport.sendFrame(1, byteArrayOf(2))
        assertEquals(2, transport.lastFrame.value.size)

        // Replace with single universe
        transport.updateFrame(mapOf(5 to byteArrayOf(50)))

        assertEquals(1, transport.lastFrame.value.size)
        assertNull(transport.getUniverseData(0))
        assertNull(transport.getUniverseData(1))
        assertNotNull(transport.getUniverseData(5))
    }

    @Test
    fun updateFrame_copiesInputData() {
        val transport = SimulatedTransport()
        val data = byteArrayOf(42)

        transport.updateFrame(mapOf(0 to data))
        data[0] = 0

        assertEquals(42.toByte(), transport.getUniverseData(0)!![0])
    }

    // ------------------------------------------------------------------ //
    //  getChannelValue                                                    //
    // ------------------------------------------------------------------ //

    @Test
    fun getChannelValue_returnsCorrectByte() {
        val transport = SimulatedTransport()
        val data = ByteArray(512) { (it % 256).toByte() }

        transport.sendFrame(0, data)

        assertEquals(0, transport.getChannelValue(0, 0))
        assertEquals(127, transport.getChannelValue(0, 127))
        assertEquals(255, transport.getChannelValue(0, 255))
    }

    @Test
    fun getChannelValue_unknownUniverse_returnsNull() {
        val transport = SimulatedTransport()
        assertNull(transport.getChannelValue(99, 0))
    }

    @Test
    fun getChannelValue_outOfRangeChannel_returnsNull() {
        val transport = SimulatedTransport()
        transport.sendFrame(0, byteArrayOf(1, 2, 3))

        assertNull(transport.getChannelValue(0, 5))
        assertNull(transport.getChannelValue(0, -1))
    }

    // ------------------------------------------------------------------ //
    //  reset                                                              //
    // ------------------------------------------------------------------ //

    @Test
    fun reset_clearsAllFrames() {
        val transport = SimulatedTransport()

        transport.sendFrame(0, byteArrayOf(1))
        transport.sendFrame(1, byteArrayOf(2))
        assertEquals(2, transport.lastFrame.value.size)

        transport.reset()
        assertTrue(transport.lastFrame.value.isEmpty())
    }
}

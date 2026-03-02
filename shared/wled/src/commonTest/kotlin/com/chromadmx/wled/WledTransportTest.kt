package com.chromadmx.wled

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.chromadmx.networking.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

class FakeDeviceRegistry : WledDeviceRegistry {
    private val _devices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val adoptedDevices: StateFlow<List<WledDevice>> = _devices

    private val mappings = mutableMapOf<Int, WledUniverseMapping>()

    override fun getUniverseMapping(universe: Int): WledUniverseMapping? = mappings[universe]

    override fun adoptDevice(device: WledDevice, universe: Int) {
        _devices.value = _devices.value + device
        mappings[universe] = WledUniverseMapping(
            deviceIp = device.ipAddress,
            segmentMappings = listOf(0 to 0..2),
        )
    }

    override fun removeDevice(ip: String) {
        _devices.value = _devices.value.filter { it.ipAddress != ip }
    }
}

class RecordingApiClient : WledApiClient {
    val colorCalls = mutableListOf<Triple<String, Int, Triple<Int, Int, Int>>>()

    override suspend fun getFullState(ip: String): WledFullState? = null

    override suspend fun setState(ip: String, state: WledState): Boolean = true

    override suspend fun setSegmentColor(ip: String, segmentId: Int, r: Int, g: Int, b: Int): Boolean {
        colorCalls.add(Triple(ip, segmentId, Triple(r, g, b)))
        return true
    }

    override suspend fun setSegmentEffect(
        ip: String,
        segmentId: Int,
        effectId: Int,
        speed: Int,
        intensity: Int,
    ): Boolean = true

    override suspend fun setPower(ip: String, on: Boolean): Boolean = true

    override suspend fun setBrightness(ip: String, brightness: Int): Boolean = true
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class WledTransportTest {

    private fun createTransport(
        scope: TestScope,
        apiClient: RecordingApiClient = RecordingApiClient(),
        registry: FakeDeviceRegistry = FakeDeviceRegistry(),
    ): Triple<WledTransport, RecordingApiClient, FakeDeviceRegistry> {
        val transport = WledTransport(apiClient, registry, scope)
        return Triple(transport, apiClient, registry)
    }

    @Test
    fun startWithNoDevicesStaysDisconnected() = runTest {
        val (transport, _, _) = createTransport(this)

        transport.start()

        assertTrue(transport.isRunning)
        assertEquals(ConnectionState.Disconnected, transport.connectionState.value)
    }

    @Test
    fun startWithAdoptedDeviceConnects() = runTest {
        val (transport, _, registry) = createTransport(this)
        registry.adoptDevice(
            WledDevice(ipAddress = "192.168.1.50", name = "Test Strip"),
            universe = 0,
        )

        transport.start()

        assertTrue(transport.isRunning)
        assertEquals(ConnectionState.Connected, transport.connectionState.value)
    }

    @Test
    fun sendFrameRoutesToCorrectDevice() = runTest {
        val (transport, apiClient, registry) = createTransport(this)
        registry.adoptDevice(
            WledDevice(ipAddress = "192.168.1.50", name = "Test Strip"),
            universe = 0,
        )
        transport.start()

        // Build a 3-byte RGB frame: R=255, G=128, B=64
        val channels = byteArrayOf(255.toByte(), 128.toByte(), 64.toByte())
        transport.sendFrame(0, channels)

        // Let the launched coroutine complete
        advanceUntilIdle()

        assertEquals(1, apiClient.colorCalls.size)
        val (ip, segId, rgb) = apiClient.colorCalls.first()
        assertEquals("192.168.1.50", ip)
        assertEquals(0, segId)
        assertEquals(Triple(255, 128, 64), rgb)
    }

    @Test
    fun stopSetsDisconnected() = runTest {
        val (transport, _, registry) = createTransport(this)
        registry.adoptDevice(
            WledDevice(ipAddress = "192.168.1.50", name = "Test Strip"),
            universe = 0,
        )
        transport.start()
        assertEquals(ConnectionState.Connected, transport.connectionState.value)

        transport.stop()

        assertFalse(transport.isRunning)
        assertEquals(ConnectionState.Disconnected, transport.connectionState.value)
    }
}

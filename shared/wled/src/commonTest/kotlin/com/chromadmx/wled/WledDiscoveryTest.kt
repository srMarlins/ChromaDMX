package com.chromadmx.wled

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

/** Fake mDNS browser for testing — allows manual control of discovered devices. */
class FakeMdnsBrowser : WledMdnsBrowser {
    val devices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<WledDevice>> = devices
    var browsing = false
    override fun startBrowse() { browsing = true }
    override fun stopBrowse() { browsing = false }
}

/** Fake API client that returns null for all methods — no network calls. */
class FakeWledApiClient : WledApiClient {
    override suspend fun getFullState(ip: String): WledFullState? = null
    override suspend fun setState(ip: String, state: WledState): Boolean = false
    override suspend fun setSegmentsState(ip: String, segments: List<SegmentColorPayload>): Boolean = false
    override suspend fun setSegmentColor(ip: String, segmentId: Int, r: Int, g: Int, b: Int): Boolean = false
    override suspend fun setSegmentEffect(ip: String, segmentId: Int, effectId: Int, speed: Int, intensity: Int): Boolean = false
    override suspend fun setPower(ip: String, on: Boolean): Boolean = false
    override suspend fun setBrightness(ip: String, brightness: Int): Boolean = false
}

@OptIn(ExperimentalCoroutinesApi::class)
class WledDiscoveryTest {

    private fun createDiscovery(
        browser: FakeMdnsBrowser = FakeMdnsBrowser(),
        apiClient: WledApiClient = FakeWledApiClient(),
        scope: TestScope = TestScope(),
    ): Triple<WledDiscovery, FakeMdnsBrowser, TestScope> {
        val discovery = WledDiscovery(browser, apiClient, scope)
        return Triple(discovery, browser, scope)
    }

    @Test
    fun startScanActivatesMdnsBrowser() = runTest {
        val browser = FakeMdnsBrowser()
        val discovery = WledDiscovery(browser, FakeWledApiClient(), this)

        assertFalse(browser.browsing, "Browser should not be browsing before startScan")
        discovery.startScan()
        assertTrue(browser.browsing, "Browser should be browsing after startScan")
        assertTrue(discovery.isScanning.value, "isScanning should be true after startScan")

        // Clean up the collection coroutine
        discovery.stopScan()
    }

    @Test
    fun stopScanDeactivatesMdnsBrowser() = runTest {
        val browser = FakeMdnsBrowser()
        val discovery = WledDiscovery(browser, FakeWledApiClient(), this)

        discovery.startScan()
        assertTrue(browser.browsing)
        assertTrue(discovery.isScanning.value)

        discovery.stopScan()
        assertFalse(browser.browsing, "Browser should stop browsing after stopScan")
        assertFalse(discovery.isScanning.value, "isScanning should be false after stopScan")
    }

    @Test
    fun discoveredDeviceConvertsToDmxNode() {
        val device = WledDevice(
            ipAddress = "192.168.1.42",
            name = "Living Room WLED",
            macAddress = "aa:bb:cc:dd:ee:ff",
            totalLeds = 60,
            segments = listOf(
                WledSegmentState(id = 0, start = 0, stop = 30, len = 30),
                WledSegmentState(id = 1, start = 30, stop = 60, len = 30),
            ),
            firmwareVersion = "0.14.0",
            isOnline = true,
            lastSeenMs = 1000L,
        )

        val node = device.toDmxNode()

        assertEquals("192.168.1.42", node.ipAddress)
        assertEquals("aa:bb:cc:dd:ee:ff", node.macAddress)
        assertEquals("Living Room WLED", node.shortName)
        assertEquals("WLED: Living Room WLED", node.longName)
        assertEquals(2, node.numPorts, "numPorts should equal segment count")
        assertEquals(listOf(0, 1), node.universes, "universes should map segment IDs")
        assertEquals(0, node.style, "style should be 0 (Node)")
        assertEquals(1000L, node.lastSeenMs)
    }

    @Test
    fun toDmxNodeWithNoSegmentsDefaultsToOnePort() {
        val device = WledDevice(
            ipAddress = "10.0.0.5",
            name = "Simple Strip",
        )

        val node = device.toDmxNode()

        assertEquals(1, node.numPorts, "No segments should default to 1 port")
        assertEquals(listOf(0), node.universes, "No segments should default to universe 0")
    }

    @Test
    fun toDmxNodeTruncatesLongNames() {
        val device = WledDevice(
            ipAddress = "10.0.0.1",
            name = "This Is A Very Long WLED Device Name That Exceeds Seventeen Characters",
        )

        val node = device.toDmxNode()

        assertEquals(17, node.shortName.length, "shortName should be truncated to 17 chars")
        assertEquals("This Is A Very Lo", node.shortName)
    }

    @Test
    fun discoveredDevicesFlowToDmxNodes() = runTest {
        val browser = FakeMdnsBrowser()
        val discovery = WledDiscovery(browser, FakeWledApiClient(), this)

        discovery.startScan()

        // Emit a device from the browser
        browser.devices.value = listOf(
            WledDevice(
                ipAddress = "192.168.1.100",
                name = "Test WLED",
                isOnline = true,
                lastSeenMs = 500L,
            )
        )

        advanceUntilIdle()

        assertEquals(1, discovery.discoveredNodes.value.size, "Should have one DmxNode")
        assertEquals("192.168.1.100", discovery.discoveredNodes.value[0].ipAddress)
        assertEquals("Test WLED", discovery.discoveredNodes.value[0].shortName)

        assertEquals(1, discovery.wledDevices.value.size, "Should have one WledDevice")
        assertEquals("Test WLED", discovery.wledDevices.value[0].name)

        discovery.stopScan()
    }
}

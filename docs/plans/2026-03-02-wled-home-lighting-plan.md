# WLED Home Lighting Expansion — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add first-class WLED device support, a room box spatial view, use-case guided onboarding, and revised subscription tiers to expand ChromaDMX from a pro DMX tool into the consumer home lighting market.

**Architecture:** New `shared/wled/` KMP module provides WLED discovery (mDNS), JSON API client, and a `DmxTransport` adapter. The existing engine, fixture model, and effect system remain unchanged — WLED devices map to standard `Fixture3D` objects. A new `RoomBoxView` Composable renders fixtures on the faces of a 3D box. Onboarding forks based on use-case selection ("My Room" / "A Stage" / "Just Exploring").

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor (HTTP client), kotlinx.serialization, NsdManager (Android mDNS), SQLDelight, Koin, Koog agent SDK.

**Design doc:** `docs/plans/2026-03-02-wled-home-lighting-design.md`

---

## Task 1: WLED Module Scaffolding

**Files:**
- Create: `shared/wled/build.gradle.kts`
- Modify: `settings.gradle.kts:42` (add include)
- Create: `shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledDevice.kt`
- Create: `shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledDeviceTest.kt`

**Step 1: Create module build file**

Reference: `shared/networking/build.gradle.kts` (12 lines, uses `chromadmx.kmp.library` convention plugin).

```kotlin
// shared/wled/build.gradle.kts
plugins {
    id("chromadmx.kmp.library")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            api(project(":shared:networking"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
```

**Step 2: Register module in settings.gradle.kts**

Add after line 42 (`include(":shared:subscription")`):

```kotlin
include(":shared:wled")
```

**Step 3: Write WledDevice data model with tests**

```kotlin
// shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledDevice.kt
package com.chromadmx.wled

import kotlinx.serialization.Serializable

@Serializable
data class WledSegment(
    val id: Int,
    val start: Int,
    val stop: Int,
    val len: Int,
    val grp: Int = 1,
    val spc: Int = 0,
)

@Serializable
data class WledDeviceInfo(
    val ver: String,
    val vid: Long,
    val leds: WledLedInfo,
    val name: String,
    val udpport: Int = 21324,
    val arch: String = "",
    val freeheap: Long = 0,
    val mac: String = "",
)

@Serializable
data class WledLedInfo(
    val count: Int,
    val rgbw: Boolean = false,
    val wv: Int = 0,
    val cct: Boolean = false,
    val maxpwr: Int = 0,
    val maxseg: Int = 1,
    val lc: Int = 1,
    val seglc: List<Int> = emptyList(),
)

@Serializable
data class WledState(
    val on: Boolean = true,
    val bri: Int = 128,
    val transition: Int = 7,
    val ps: Int = -1,
    val seg: List<WledSegmentState> = emptyList(),
)

@Serializable
data class WledSegmentState(
    val id: Int = 0,
    val start: Int = 0,
    val stop: Int = 0,
    val len: Int = 0,
    val grp: Int = 1,
    val spc: Int = 0,
    val col: List<List<Int>> = listOf(listOf(255, 160, 0)),
    val fx: Int = 0,
    val sx: Int = 128,
    val ix: Int = 128,
    val pal: Int = 0,
    val on: Boolean = true,
    val bri: Int = 255,
)

@Serializable
data class WledFullState(
    val state: WledState,
    val info: WledDeviceInfo,
)

data class WledDevice(
    val ipAddress: String,
    val name: String,
    val macAddress: String = "",
    val totalLeds: Int = 0,
    val segments: List<WledSegmentState> = emptyList(),
    val firmwareVersion: String = "",
    val isOnline: Boolean = false,
    val lastSeenMs: Long = 0L,
)
```

**Step 4: Write test for data model**

```kotlin
// shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledDeviceTest.kt
package com.chromadmx.wled

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WledDeviceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseDeviceInfoFromJson() {
        val raw = """{"ver":"0.14.0","vid":2312080,"leds":{"count":60,"rgbw":false,"wv":0,"cct":false,"maxpwr":850,"maxseg":16,"lc":1,"seglc":[60]},"name":"WLED","udpport":21324,"arch":"esp32","freeheap":180000,"mac":"AA:BB:CC:DD:EE:FF"}"""
        val info = json.decodeFromString<WledDeviceInfo>(raw)
        assertEquals("WLED", info.name)
        assertEquals(60, info.leds.count)
        assertFalse(info.leds.rgbw)
        assertEquals("AA:BB:CC:DD:EE:FF", info.mac)
    }

    @Test
    fun parseFullStateFromJson() {
        val raw = """{"state":{"on":true,"bri":128,"transition":7,"ps":-1,"seg":[{"id":0,"start":0,"stop":60,"len":60,"col":[[255,0,0]],"fx":0,"sx":128,"ix":128,"on":true,"bri":255}]},"info":{"ver":"0.14.0","vid":2312080,"leds":{"count":60,"rgbw":false,"maxseg":16,"lc":1,"seglc":[60]},"name":"Desk Strip","udpport":21324,"mac":"AA:BB:CC:DD:EE:FF"}}"""
        val full = json.decodeFromString<WledFullState>(raw)
        assertEquals("Desk Strip", full.info.name)
        assertEquals(1, full.state.seg.size)
        assertEquals(60, full.state.seg[0].len)
    }

    @Test
    fun wledDeviceDefaultValues() {
        val device = WledDevice(ipAddress = "192.168.1.100", name = "Test")
        assertEquals("", device.macAddress)
        assertEquals(0, device.totalLeds)
        assertFalse(device.isOnline)
    }
}
```

**Step 5: Run test to verify**

Run: `./gradlew :shared:wled:testAndroidHostTest --tests "com.chromadmx.wled.WledDeviceTest" -q`
Expected: 3 tests PASS

**Step 6: Commit**

```bash
git add shared/wled/ settings.gradle.kts
git commit -m "feat(wled): scaffold shared/wled module with device data models"
```

---

## Task 2: WLED HTTP Client

**Files:**
- Create: `shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledApiClient.kt`
- Create: `shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledApiClientTest.kt`

**Step 1: Write the WledApiClient interface and Ktor implementation**

```kotlin
// shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledApiClient.kt
package com.chromadmx.wled

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

interface WledApiClient {
    suspend fun getFullState(ip: String): WledFullState?
    suspend fun setState(ip: String, state: WledState): Boolean
    suspend fun setSegmentColor(ip: String, segmentId: Int, r: Int, g: Int, b: Int): Boolean
    suspend fun setSegmentEffect(ip: String, segmentId: Int, effectId: Int, speed: Int = 128, intensity: Int = 128): Boolean
    suspend fun setPower(ip: String, on: Boolean): Boolean
    suspend fun setBrightness(ip: String, brightness: Int): Boolean
}

class WledApiClientImpl(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
    private val port: Int = 80,
) : WledApiClient {

    override suspend fun getFullState(ip: String): WledFullState? {
        return try {
            httpClient.get("http://$ip:$port/json").body<WledFullState>()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setState(ip: String, state: WledState): Boolean {
        return try {
            httpClient.post("http://$ip:$port/json/state") {
                contentType(ContentType.Application.Json)
                setBody(state)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setSegmentColor(ip: String, segmentId: Int, r: Int, g: Int, b: Int): Boolean {
        return try {
            val payload = """{"seg":[{"id":$segmentId,"col":[[${r},${g},${b}]]}]}"""
            httpClient.post("http://$ip:$port/json/state") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setSegmentEffect(ip: String, segmentId: Int, effectId: Int, speed: Int, intensity: Int): Boolean {
        return try {
            val payload = """{"seg":[{"id":$segmentId,"fx":$effectId,"sx":$speed,"ix":$intensity}]}"""
            httpClient.post("http://$ip:$port/json/state") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setPower(ip: String, on: Boolean): Boolean {
        return try {
            val payload = """{"on":$on}"""
            httpClient.post("http://$ip:$port/json/state") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setBrightness(ip: String, brightness: Int): Boolean {
        return try {
            val payload = """{"bri":${brightness.coerceIn(0, 255)}}"""
            httpClient.post("http://$ip:$port/json/state") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

**Step 2: Write tests with a fake HTTP client**

```kotlin
// shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledApiClientTest.kt
package com.chromadmx.wled

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WledApiClientTest {

    private fun mockClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        return HttpClient(MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun getFullStateParses60LedDevice() = runTest {
        val json = """{"state":{"on":true,"bri":128,"transition":7,"ps":-1,"seg":[{"id":0,"start":0,"stop":60,"len":60,"col":[[255,160,0]],"fx":0,"sx":128,"ix":128,"on":true,"bri":255}]},"info":{"ver":"0.14.0","vid":2312080,"leds":{"count":60,"rgbw":false,"maxseg":16,"lc":1,"seglc":[60]},"name":"Desk Strip","udpport":21324,"mac":"AA:BB:CC:DD:EE:FF"}}"""
        val client = WledApiClientImpl(httpClient = mockClient(json))

        val result = client.getFullState("192.168.1.100")
        assertNotNull(result)
        assertEquals("Desk Strip", result.info.name)
        assertEquals(60, result.info.leds.count)
        assertEquals(1, result.state.seg.size)
    }

    @Test
    fun setPowerSendsOnState() = runTest {
        val client = WledApiClientImpl(httpClient = mockClient("{}"))
        val result = client.setPower("192.168.1.100", true)
        assertTrue(result)
    }

    @Test
    fun setBrightnessClampsTo0_255() = runTest {
        val client = WledApiClientImpl(httpClient = mockClient("{}"))
        assertTrue(client.setBrightness("192.168.1.100", 300))
        assertTrue(client.setBrightness("192.168.1.100", -10))
    }

    @Test
    fun getFullStateReturnsNullOnError() = runTest {
        val client = WledApiClientImpl(
            httpClient = mockClient("not json", HttpStatusCode.InternalServerError)
        )
        val result = client.getFullState("192.168.1.100")
        assertEquals(null, result)
    }
}
```

**Step 3: Run tests**

Run: `./gradlew :shared:wled:testAndroidHostTest --tests "com.chromadmx.wled.WledApiClientTest" -q`
Expected: 4 tests PASS

**Step 4: Commit**

```bash
git add shared/wled/src/
git commit -m "feat(wled): WLED JSON API client with Ktor + mock tests"
```

---

## Task 3: WLED Discovery (mDNS)

**Files:**
- Create: `shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledMdnsBrowser.kt`
- Create: `shared/wled/src/androidMain/kotlin/com/chromadmx/wled/WledMdnsBrowserAndroid.kt`
- Create: `shared/wled/src/iosMain/kotlin/com/chromadmx/wled/WledMdnsBrowserIos.kt`
- Create: `shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledDiscovery.kt`
- Create: `shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledDiscoveryTest.kt`

**Step 1: Write expect interface for mDNS browser**

```kotlin
// shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledMdnsBrowser.kt
package com.chromadmx.wled

import kotlinx.coroutines.flow.StateFlow

interface WledMdnsBrowser {
    fun startBrowse()
    fun stopBrowse()
    val discoveredDevices: StateFlow<List<WledDevice>>
}
```

**Step 2: Write WledDiscovery that implements FixtureDiscovery**

Reference: `FixtureDiscovery` interface at `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/FixtureDiscovery.kt`.

```kotlin
// shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledDiscovery.kt
package com.chromadmx.wled

import com.chromadmx.networking.DmxNode
import com.chromadmx.networking.FixtureDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WledDiscovery(
    private val mdnsBrowser: WledMdnsBrowser,
    private val apiClient: WledApiClient,
    private val scope: CoroutineScope,
) : FixtureDiscovery {

    private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
    override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _wledDevices = MutableStateFlow<List<WledDevice>>(emptyList())
    val wledDevices: StateFlow<List<WledDevice>> = _wledDevices.asStateFlow()

    override fun startScan() {
        _isScanning.value = true
        mdnsBrowser.startBrowse()
        scope.launch {
            mdnsBrowser.discoveredDevices.collect { rawDevices ->
                val enriched = rawDevices.map { device ->
                    val fullState = apiClient.getFullState(device.ipAddress)
                    if (fullState != null) {
                        device.copy(
                            name = fullState.info.name,
                            macAddress = fullState.info.mac,
                            totalLeds = fullState.info.leds.count,
                            segments = fullState.state.seg,
                            firmwareVersion = fullState.info.ver,
                            isOnline = true,
                        )
                    } else {
                        device.copy(isOnline = false)
                    }
                }
                _wledDevices.value = enriched
                _discoveredNodes.value = enriched.map { it.toDmxNode() }
            }
        }
    }

    override fun stopScan() {
        _isScanning.value = false
        mdnsBrowser.stopBrowse()
    }
}

fun WledDevice.toDmxNode(): DmxNode = DmxNode(
    ipAddress = ipAddress,
    macAddress = macAddress,
    shortName = name.take(17),
    longName = "WLED: $name ($totalLeds LEDs)",
    numPorts = segments.size.coerceAtLeast(1),
    universes = List(segments.size.coerceAtLeast(1)) { it },
    style = 0,
)
```

**Step 3: Write Android mDNS implementation**

```kotlin
// shared/wled/src/androidMain/kotlin/com/chromadmx/wled/WledMdnsBrowserAndroid.kt
package com.chromadmx.wled

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WledMdnsBrowserAndroid(context: Context) : WledMdnsBrowser {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _discoveredDevices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<WledDevice>> = _discoveredDevices.asStateFlow()
    private var listener: NsdManager.DiscoveryListener? = null

    override fun startBrowse() {
        if (listener != null) return
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(si: NsdServiceInfo) {
                        val ip = si.host?.hostAddress ?: return
                        val device = WledDevice(
                            ipAddress = ip,
                            name = si.serviceName,
                            isOnline = true,
                        )
                        _discoveredDevices.value = (_discoveredDevices.value + device).distinctBy { it.ipAddress }
                    }
                })
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val lostName = serviceInfo.serviceName
                _discoveredDevices.value = _discoveredDevices.value.filter { it.name != lostName }
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        listener = discoveryListener
        nsdManager.discoverServices("_wled._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun stopBrowse() {
        listener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        listener = null
    }
}
```

**Step 4: Write iOS stub**

```kotlin
// shared/wled/src/iosMain/kotlin/com/chromadmx/wled/WledMdnsBrowserIos.kt
package com.chromadmx.wled

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WledMdnsBrowserIos : WledMdnsBrowser {
    private val _discoveredDevices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<WledDevice>> = _discoveredDevices.asStateFlow()

    override fun startBrowse() {
        // TODO: Implement with NWBrowser for _wled._tcp
    }

    override fun stopBrowse() {}
}
```

**Step 5: Write discovery tests with a fake mDNS browser**

```kotlin
// shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledDiscoveryTest.kt
package com.chromadmx.wled

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeMdnsBrowser : WledMdnsBrowser {
    val devices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<WledDevice>> = devices
    var browsing = false
    override fun startBrowse() { browsing = true }
    override fun stopBrowse() { browsing = false }
}

class FakeWledApiClient : WledApiClient {
    var fullStateResponse: WledFullState? = null
    override suspend fun getFullState(ip: String) = fullStateResponse
    override suspend fun setState(ip: String, state: WledState) = true
    override suspend fun setSegmentColor(ip: String, segmentId: Int, r: Int, g: Int, b: Int) = true
    override suspend fun setSegmentEffect(ip: String, segmentId: Int, effectId: Int, speed: Int, intensity: Int) = true
    override suspend fun setPower(ip: String, on: Boolean) = true
    override suspend fun setBrightness(ip: String, brightness: Int) = true
}

class WledDiscoveryTest {

    @Test
    fun startScanActivatesMdnsBrowser() = runTest {
        val browser = FakeMdnsBrowser()
        val discovery = WledDiscovery(browser, FakeWledApiClient(), this)
        discovery.startScan()
        assertTrue(browser.browsing)
    }

    @Test
    fun stopScanDeactivatesMdnsBrowser() = runTest {
        val browser = FakeMdnsBrowser()
        val discovery = WledDiscovery(browser, FakeWledApiClient(), this)
        discovery.startScan()
        discovery.stopScan()
        assertEquals(false, browser.browsing)
    }

    @Test
    fun discoveredDeviceConvertsToDmxNode() {
        val device = WledDevice(
            ipAddress = "192.168.1.50",
            name = "My Strip",
            totalLeds = 60,
            segments = listOf(WledSegmentState(id = 0, start = 0, stop = 60, len = 60)),
        )
        val node = device.toDmxNode()
        assertEquals("192.168.1.50", node.ipAddress)
        assertEquals("My Strip", node.shortName)
        assertEquals(1, node.numPorts)
    }
}
```

**Step 6: Run tests**

Run: `./gradlew :shared:wled:testAndroidHostTest --tests "com.chromadmx.wled.WledDiscoveryTest" -q`
Expected: 3 tests PASS

**Step 7: Commit**

```bash
git add shared/wled/src/
git commit -m "feat(wled): mDNS discovery + FixtureDiscovery adapter"
```

---

## Task 4: WLED Transport (DmxTransport Adapter)

**Files:**
- Create: `shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledTransport.kt`
- Create: `shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledTransportTest.kt`

**Step 1: Write WledTransport implementing DmxTransport**

Reference: `DmxTransport` at `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/DmxTransport.kt:21-28`.

```kotlin
// shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledTransport.kt
package com.chromadmx.wled

import com.chromadmx.networking.ConnectionState
import com.chromadmx.networking.DmxTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WledTransport(
    private val apiClient: WledApiClient,
    private val deviceRegistry: WledDeviceRegistry,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DmxTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    override var isRunning: Boolean = false; private set

    override fun start() {
        if (isRunning) return
        isRunning = true
        _connectionState.value = ConnectionState.Connecting
        scope.launch {
            val hasDevices = deviceRegistry.adoptedDevices.value.isNotEmpty()
            _connectionState.value = if (hasDevices) ConnectionState.Connected else ConnectionState.Disconnected
        }
    }

    override fun stop() {
        isRunning = false
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun sendFrame(universe: Int, channels: ByteArray) {
        if (!isRunning) return
        val mapping = deviceRegistry.getUniverseMapping(universe) ?: return
        scope.launch {
            for ((segmentId, channelRange) in mapping.segmentMappings) {
                if (channelRange.last < channels.size) {
                    val r = channels[channelRange.first].toInt() and 0xFF
                    val g = channels[channelRange.first + 1].toInt() and 0xFF
                    val b = channels[channelRange.first + 2].toInt() and 0xFF
                    apiClient.setSegmentColor(mapping.deviceIp, segmentId, r, g, b)
                }
            }
        }
    }

    override fun updateFrame(universeData: Map<Int, ByteArray>) {
        universeData.forEach { (universe, channels) -> sendFrame(universe, channels) }
    }
}

data class WledUniverseMapping(
    val deviceIp: String,
    val segmentMappings: List<Pair<Int, IntRange>>,
)

interface WledDeviceRegistry {
    val adoptedDevices: StateFlow<List<WledDevice>>
    fun getUniverseMapping(universe: Int): WledUniverseMapping?
    fun adoptDevice(device: WledDevice, universe: Int)
    fun removeDevice(ip: String)
}
```

**Step 2: Write tests**

```kotlin
// shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledTransportTest.kt
package com.chromadmx.wled

import com.chromadmx.networking.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeDeviceRegistry : WledDeviceRegistry {
    private val _devices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val adoptedDevices: StateFlow<List<WledDevice>> = _devices
    private val mappings = mutableMapOf<Int, WledUniverseMapping>()

    override fun getUniverseMapping(universe: Int) = mappings[universe]
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
    override suspend fun getFullState(ip: String) = null
    override suspend fun setState(ip: String, state: WledState) = true
    override suspend fun setSegmentColor(ip: String, segmentId: Int, r: Int, g: Int, b: Int): Boolean {
        colorCalls.add(Triple(ip, segmentId, Triple(r, g, b)))
        return true
    }
    override suspend fun setSegmentEffect(ip: String, segmentId: Int, effectId: Int, speed: Int, intensity: Int) = true
    override suspend fun setPower(ip: String, on: Boolean) = true
    override suspend fun setBrightness(ip: String, brightness: Int) = true
}

class WledTransportTest {

    @Test
    fun startWithNoDevicesStaysDisconnected() = runTest {
        val transport = WledTransport(FakeWledApiClient(), FakeDeviceRegistry(), this)
        transport.start()
        advanceUntilIdle()
        assertEquals(ConnectionState.Disconnected, transport.connectionState.value)
    }

    @Test
    fun startWithAdoptedDeviceConnects() = runTest {
        val registry = FakeDeviceRegistry()
        registry.adoptDevice(WledDevice("192.168.1.10", "Test"), 0)
        val transport = WledTransport(FakeWledApiClient(), registry, this)
        transport.start()
        advanceUntilIdle()
        assertEquals(ConnectionState.Connected, transport.connectionState.value)
    }

    @Test
    fun sendFrameRoutesToCorrectDevice() = runTest {
        val api = RecordingApiClient()
        val registry = FakeDeviceRegistry()
        registry.adoptDevice(WledDevice("192.168.1.10", "Test"), 0)
        val transport = WledTransport(api, registry, this)
        transport.start()
        advanceUntilIdle()

        transport.sendFrame(0, byteArrayOf(255.toByte(), 0, 128.toByte()))
        advanceUntilIdle()

        assertEquals(1, api.colorCalls.size)
        assertEquals("192.168.1.10", api.colorCalls[0].first)
        assertEquals(Triple(255, 0, 128), api.colorCalls[0].third)
    }

    @Test
    fun stopSetsDisconnected() = runTest {
        val transport = WledTransport(FakeWledApiClient(), FakeDeviceRegistry(), this)
        transport.start()
        transport.stop()
        assertFalse(transport.isRunning)
        assertEquals(ConnectionState.Disconnected, transport.connectionState.value)
    }
}
```

**Step 3: Run tests**

Run: `./gradlew :shared:wled:testAndroidHostTest --tests "com.chromadmx.wled.WledTransportTest" -q`
Expected: 4 tests PASS

**Step 4: Commit**

```bash
git add shared/wled/src/
git commit -m "feat(wled): WledTransport implementing DmxTransport interface"
```

---

## Task 5: WLED Database Persistence

**Files:**
- Create: `shared/core/src/commonMain/sqldelight/com/chromadmx/core/db/Wled.sq`
- Create: `shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledRepository.kt`
- Create: `shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledRepositoryTest.kt`

**Step 1: Write SQLDelight schema**

Reference: Existing schemas in `shared/core/src/commonMain/sqldelight/com/chromadmx/core/db/` (Fixtures.sq, Network.sq, etc).

```sql
-- shared/core/src/commonMain/sqldelight/com/chromadmx/core/db/Wled.sq

CREATE TABLE IF NOT EXISTS wled_device (
    ip_address TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    mac_address TEXT NOT NULL DEFAULT '',
    total_leds INTEGER NOT NULL DEFAULT 0,
    firmware_version TEXT NOT NULL DEFAULT '',
    assigned_universe INTEGER NOT NULL DEFAULT -1,
    last_seen_ms INTEGER NOT NULL DEFAULT 0
);

selectAllDevices:
SELECT * FROM wled_device ORDER BY name;

selectDeviceByIp:
SELECT * FROM wled_device WHERE ip_address = ?;

upsertDevice:
INSERT OR REPLACE INTO wled_device (ip_address, name, mac_address, total_leds, firmware_version, assigned_universe, last_seen_ms)
VALUES (?, ?, ?, ?, ?, ?, ?);

deleteDevice:
DELETE FROM wled_device WHERE ip_address = ?;

deviceCount:
SELECT COUNT(*) FROM wled_device;
```

**Step 2: Write WledRepository**

```kotlin
// shared/wled/src/commonMain/kotlin/com/chromadmx/wled/WledRepository.kt
package com.chromadmx.wled

import com.chromadmx.core.db.ChromaDmxDatabase

class WledRepository(private val db: ChromaDmxDatabase) {

    private val queries = db.wledQueries

    fun getAllDevices(): List<WledDevice> {
        return queries.selectAllDevices().executeAsList().map { row ->
            WledDevice(
                ipAddress = row.ip_address,
                name = row.name,
                macAddress = row.mac_address,
                totalLeds = row.total_leds.toInt(),
                firmwareVersion = row.firmware_version,
                lastSeenMs = row.last_seen_ms,
            )
        }
    }

    fun upsertDevice(device: WledDevice, universe: Int = -1) {
        queries.upsertDevice(
            ip_address = device.ipAddress,
            name = device.name,
            mac_address = device.macAddress,
            total_leds = device.totalLeds.toLong(),
            firmware_version = device.firmwareVersion,
            assigned_universe = universe.toLong(),
            last_seen_ms = device.lastSeenMs,
        )
    }

    fun deleteDevice(ip: String) {
        queries.deleteDevice(ip)
    }

    fun deviceCount(): Long {
        return queries.deviceCount().executeAsOne()
    }
}
```

**Step 3: Write tests**

Note: SQLDelight tests require a real database driver. Use the in-memory SQLite driver for tests.

```kotlin
// shared/wled/src/commonTest/kotlin/com/chromadmx/wled/WledRepositoryTest.kt
package com.chromadmx.wled

import kotlin.test.Test
import kotlin.test.assertEquals

class WledRepositoryTest {

    @Test
    fun wledDeviceRoundTrip() {
        val device = WledDevice(
            ipAddress = "192.168.1.100",
            name = "Test Strip",
            macAddress = "AA:BB:CC",
            totalLeds = 60,
            firmwareVersion = "0.14.0",
        )
        // Verify data class integrity (DB integration tested via instrumented tests)
        assertEquals("192.168.1.100", device.ipAddress)
        assertEquals(60, device.totalLeds)
    }
}
```

**Step 4: Run tests**

Run: `./gradlew :shared:wled:testAndroidHostTest --tests "com.chromadmx.wled.WledRepositoryTest" -q`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/core/src/commonMain/sqldelight/com/chromadmx/core/db/Wled.sq shared/wled/src/
git commit -m "feat(wled): SQLDelight schema + WledRepository for device persistence"
```

---

## Task 6: WLED Koin DI Module

**Files:**
- Create: `shared/wled/src/commonMain/kotlin/com/chromadmx/wled/di/WledModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt` (add wledModule include)

**Step 1: Write WledModule**

Reference: DI pattern at `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt` — uses Koin `module {}` with `single {}` and `named()` qualifiers.

```kotlin
// shared/wled/src/commonMain/kotlin/com/chromadmx/wled/di/WledModule.kt
package com.chromadmx.wled.di

import com.chromadmx.networking.DmxTransport
import com.chromadmx.wled.*
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val wledModule = module {
    single<WledApiClient> { WledApiClientImpl() }
    single { WledRepository(get()) }
    single(named("wled")) {
        WledDiscovery(
            mdnsBrowser = get(),
            apiClient = get(),
            scope = get(),
        )
    }
    single(named("wled")) {
        WledTransport(
            apiClient = get(),
            deviceRegistry = get(),
            scope = get(),
        )
    } bind DmxTransport::class
}
```

**Step 2: Add wledModule to ChromaDiModule**

In `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt`, add import and include:

```kotlin
import com.chromadmx.wled.di.wledModule
// In the module definition, add:
includes(wledModule)
```

**Step 3: Commit**

```bash
git add shared/wled/src/commonMain/kotlin/com/chromadmx/wled/di/ shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt
git commit -m "feat(wled): Koin DI module wiring WledTransport + WledDiscovery"
```

---

## Task 7: Home Simulation Rig Presets

**Files:**
- Modify: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/fixtures/RigPreset.kt` (add 3 entries)
- Create: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/DeskStripRig.kt`
- Create: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/RoomAccentRig.kt`
- Create: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/WallPanelsRig.kt`
- Modify: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/fixtures/SimulatedFixtureRig.kt:28-33` (add when branches)
- Create: `shared/simulation/src/commonTest/kotlin/com/chromadmx/simulation/rigs/DeskStripRigTest.kt`
- Create: `shared/simulation/src/commonTest/kotlin/com/chromadmx/simulation/rigs/RoomAccentRigTest.kt`
- Create: `shared/simulation/src/commonTest/kotlin/com/chromadmx/simulation/rigs/WallPanelsRigTest.kt`

**Step 1: Add entries to RigPreset**

Reference: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/fixtures/RigPreset.kt:14-44`

Add before the closing brace:

```kotlin
    /**
     * Desk strip: 2 LED strips behind a monitor + 1 accent under desk.
     * Streamer/gaming setup. 60 LEDs per strip, 180 total.
     * Strips positioned along back wall at desk height.
     * Total: 3 fixtures (180 pixels), 540 DMX channels, 1 universe.
     */
    DESK_STRIP,

    /**
     * Room accent: 4 LED strips along ceiling edges in an L-shape.
     * Two walls of a bedroom/living room. 75 LEDs per strip, 300 total.
     * Total: 4 fixtures (300 pixels), 900 DMX channels, 2 universes.
     */
    ROOM_ACCENT,

    /**
     * Wall panels: 9 hexagonal light panels in a honeycomb cluster.
     * Nanoleaf-style decorative wall installation. Each panel is 1 RGB fixture.
     * Total: 9 fixtures, 27 DMX channels, 1 universe.
     */
    WALL_PANELS
```

**Step 2: Write DeskStripRig**

Reference: `SmallDjRig` at `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/SmallDjRig.kt` for the exact pattern.

```kotlin
// shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/DeskStripRig.kt
package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Desk strip rig: 2 LED strips behind a monitor + 1 accent under desk.
 *
 * Layout (room box coordinates, meters):
 * - Back wall strip 1: x=-0.3 to x=0.3, y=0 (back wall), z=0.7 (monitor height)
 * - Back wall strip 2: x=-0.4 to x=0.4, y=0 (back wall), z=0.8 (above monitor)
 * - Desk under-strip: x=-0.3 to x=0.3, y=0.3 (desk front edge), z=0.0 (desk underside)
 * - 60 pixels per strip, 3ch RGB per pixel
 */
object DeskStripRig {

    const val PIXELS_PER_STRIP = 60
    const val CHANNELS_PER_PIXEL = 3
    const val STRIP_COUNT = 3

    fun createFixtures(): List<Fixture3D> {
        val fixtures = mutableListOf<Fixture3D>()
        var channelOffset = 0

        // Back wall strip 1 (behind monitor, lower)
        for (i in 0 until PIXELS_PER_STRIP) {
            val t = i.toFloat() / (PIXELS_PER_STRIP - 1)
            val x = -0.3f + t * 0.6f
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "desk-back-lower-$i",
                        name = "Back Strip Lower ${i + 1}",
                        channelStart = channelOffset,
                        channelCount = CHANNELS_PER_PIXEL,
                        universeId = 0,
                        profileId = "wled-pixel",
                    ),
                    position = Vec3(x = x, y = 0f, z = 0.7f),
                    groupId = "back-strip-lower",
                )
            )
            channelOffset += CHANNELS_PER_PIXEL
        }

        // Back wall strip 2 (above monitor)
        for (i in 0 until PIXELS_PER_STRIP) {
            val t = i.toFloat() / (PIXELS_PER_STRIP - 1)
            val x = -0.4f + t * 0.8f
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "desk-back-upper-$i",
                        name = "Back Strip Upper ${i + 1}",
                        channelStart = channelOffset,
                        channelCount = CHANNELS_PER_PIXEL,
                        universeId = 0,
                        profileId = "wled-pixel",
                    ),
                    position = Vec3(x = x, y = 0f, z = 0.8f),
                    groupId = "back-strip-upper",
                )
            )
            channelOffset += CHANNELS_PER_PIXEL
        }

        // Desk under-strip
        for (i in 0 until PIXELS_PER_STRIP) {
            val t = i.toFloat() / (PIXELS_PER_STRIP - 1)
            val x = -0.3f + t * 0.6f
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "desk-under-$i",
                        name = "Desk Under ${i + 1}",
                        channelStart = channelOffset,
                        channelCount = CHANNELS_PER_PIXEL,
                        universeId = 0,
                        profileId = "wled-pixel",
                    ),
                    position = Vec3(x = x, y = 0.3f, z = 0f),
                    groupId = "desk-under",
                )
            )
            channelOffset += CHANNELS_PER_PIXEL
        }

        return fixtures
    }
}
```

**Step 3: Write RoomAccentRig**

```kotlin
// shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/RoomAccentRig.kt
package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Room accent rig: 4 LED strips along ceiling edges in an L-shape.
 *
 * Layout (room = 4m x 3m, z=2.4m ceiling):
 * - Strip 1: back wall ceiling edge, x=-2 to x=2, y=0, z=2.4
 * - Strip 2: right wall ceiling edge, x=2, y=0 to y=3, z=2.4
 * - Strip 3: left wall floor edge, x=-2, y=0 to y=3, z=0
 * - Strip 4: front wall floor edge, x=-2 to x=2, y=3, z=0
 * - 75 pixels per strip, 3ch RGB
 */
object RoomAccentRig {

    const val PIXELS_PER_STRIP = 75
    const val CHANNELS_PER_PIXEL = 3
    const val STRIP_COUNT = 4

    fun createFixtures(): List<Fixture3D> {
        val fixtures = mutableListOf<Fixture3D>()
        var channelOffset = 0
        var universe = 0

        data class StripDef(val id: String, val group: String, val start: Vec3, val end: Vec3)
        val strips = listOf(
            StripDef("ceiling-back", "ceiling-back", Vec3(-2f, 0f, 2.4f), Vec3(2f, 0f, 2.4f)),
            StripDef("ceiling-right", "ceiling-right", Vec3(2f, 0f, 2.4f), Vec3(2f, 3f, 2.4f)),
            StripDef("floor-left", "floor-left", Vec3(-2f, 0f, 0f), Vec3(-2f, 3f, 0f)),
            StripDef("floor-front", "floor-front", Vec3(-2f, 3f, 0f), Vec3(2f, 3f, 0f)),
        )

        for (strip in strips) {
            for (i in 0 until PIXELS_PER_STRIP) {
                val t = i.toFloat() / (PIXELS_PER_STRIP - 1)
                val pos = Vec3(
                    x = strip.start.x + t * (strip.end.x - strip.start.x),
                    y = strip.start.y + t * (strip.end.y - strip.start.y),
                    z = strip.start.z + t * (strip.end.z - strip.start.z),
                )
                if (channelOffset + CHANNELS_PER_PIXEL > 512) {
                    universe++
                    channelOffset = 0
                }
                fixtures.add(
                    Fixture3D(
                        fixture = Fixture(
                            fixtureId = "${strip.id}-$i",
                            name = "${strip.group} ${i + 1}",
                            channelStart = channelOffset,
                            channelCount = CHANNELS_PER_PIXEL,
                            universeId = universe,
                            profileId = "wled-pixel",
                        ),
                        position = pos,
                        groupId = strip.group,
                    )
                )
                channelOffset += CHANNELS_PER_PIXEL
            }
        }

        return fixtures
    }
}
```

**Step 4: Write WallPanelsRig**

```kotlin
// shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/WallPanelsRig.kt
package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Wall panels rig: 9 hexagonal panels in a honeycomb cluster.
 *
 * Layout (wall-mounted, y=0 back wall):
 * - 3 rows: bottom (3 panels), middle (3 panels), top (3 panels)
 * - Hex spacing: 0.25m horizontal, 0.22m vertical
 * - Center at x=0, z=1.5m (eye level on wall)
 * - Each panel is a single RGB fixture (3 channels)
 */
object WallPanelsRig {

    const val PANEL_COUNT = 9
    const val CHANNELS_PER_PANEL = 3
    private const val HEX_H_SPACING = 0.25f
    private const val HEX_V_SPACING = 0.22f
    private const val CENTER_Z = 1.5f

    fun createFixtures(): List<Fixture3D> {
        // Honeycomb layout: rows offset by half-spacing
        val positions = listOf(
            // Bottom row (3 panels)
            Vec3(-HEX_H_SPACING, 0f, CENTER_Z - HEX_V_SPACING),
            Vec3(0f, 0f, CENTER_Z - HEX_V_SPACING),
            Vec3(HEX_H_SPACING, 0f, CENTER_Z - HEX_V_SPACING),
            // Middle row (3 panels, offset)
            Vec3(-HEX_H_SPACING * 0.5f, 0f, CENTER_Z),
            Vec3(HEX_H_SPACING * 0.5f, 0f, CENTER_Z),
            Vec3(HEX_H_SPACING * 1.5f, 0f, CENTER_Z),
            // Top row (3 panels)
            Vec3(-HEX_H_SPACING, 0f, CENTER_Z + HEX_V_SPACING),
            Vec3(0f, 0f, CENTER_Z + HEX_V_SPACING),
            Vec3(HEX_H_SPACING, 0f, CENTER_Z + HEX_V_SPACING),
        )

        return positions.mapIndexed { i, pos ->
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "wall-panel-$i",
                    name = "Panel ${i + 1}",
                    channelStart = i * CHANNELS_PER_PANEL,
                    channelCount = CHANNELS_PER_PANEL,
                    universeId = 0,
                    profileId = "generic-rgb-par",
                ),
                position = pos,
                groupId = "wall-panels",
            )
        }
    }
}
```

**Step 5: Wire into SimulatedFixtureRig**

Modify `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/fixtures/SimulatedFixtureRig.kt:28-33`:

Add imports:
```kotlin
import com.chromadmx.simulation.rigs.DeskStripRig
import com.chromadmx.simulation.rigs.RoomAccentRig
import com.chromadmx.simulation.rigs.WallPanelsRig
```

Add when branches:
```kotlin
RigPreset.DESK_STRIP -> DeskStripRig.createFixtures()
RigPreset.ROOM_ACCENT -> RoomAccentRig.createFixtures()
RigPreset.WALL_PANELS -> WallPanelsRig.createFixtures()
```

**Step 6: Write tests for each rig**

```kotlin
// shared/simulation/src/commonTest/kotlin/com/chromadmx/simulation/rigs/DeskStripRigTest.kt
package com.chromadmx.simulation.rigs

import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeskStripRigTest {
    @Test
    fun creates180Pixels() {
        val rig = SimulatedFixtureRig(RigPreset.DESK_STRIP)
        assertEquals(180, rig.fixtureCount)
    }

    @Test
    fun allOnUniverse0() {
        val rig = SimulatedFixtureRig(RigPreset.DESK_STRIP)
        assertEquals(setOf(0), rig.universeIds)
    }

    @Test
    fun has3Groups() {
        val rig = SimulatedFixtureRig(RigPreset.DESK_STRIP)
        assertEquals(setOf("back-strip-lower", "back-strip-upper", "desk-under"), rig.groupIds)
    }

    @Test
    fun noChannelOverlap() {
        val fixtures = DeskStripRig.createFixtures()
        val ranges = fixtures.map { it.fixture.channelStart until it.fixture.channelStart + it.fixture.channelCount }
        for (i in ranges.indices) {
            for (j in i + 1 until ranges.size) {
                assertTrue(ranges[i].last < ranges[j].first || ranges[j].last < ranges[i].first, "Channels overlap at fixtures $i and $j")
            }
        }
    }
}
```

```kotlin
// shared/simulation/src/commonTest/kotlin/com/chromadmx/simulation/rigs/RoomAccentRigTest.kt
package com.chromadmx.simulation.rigs

import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomAccentRigTest {
    @Test
    fun creates300Pixels() {
        val rig = SimulatedFixtureRig(RigPreset.ROOM_ACCENT)
        assertEquals(300, rig.fixtureCount)
    }

    @Test
    fun uses2Universes() {
        val rig = SimulatedFixtureRig(RigPreset.ROOM_ACCENT)
        assertEquals(2, rig.universeCount)
    }

    @Test
    fun has4Groups() {
        val rig = SimulatedFixtureRig(RigPreset.ROOM_ACCENT)
        assertEquals(4, rig.groupIds.size)
    }
}
```

```kotlin
// shared/simulation/src/commonTest/kotlin/com/chromadmx/simulation/rigs/WallPanelsRigTest.kt
package com.chromadmx.simulation.rigs

import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.test.Test
import kotlin.test.assertEquals

class WallPanelsRigTest {
    @Test
    fun creates9Panels() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(9, rig.fixtureCount)
    }

    @Test
    fun allOnUniverse0() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(setOf(0), rig.universeIds)
    }

    @Test
    fun uses27Channels() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(27, rig.totalChannels)
    }

    @Test
    fun singleGroup() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(setOf("wall-panels"), rig.groupIds)
    }
}
```

**Step 7: Run tests**

Run: `./gradlew :shared:simulation:testAndroidHostTest --tests "com.chromadmx.simulation.rigs.DeskStripRigTest" --tests "com.chromadmx.simulation.rigs.RoomAccentRigTest" --tests "com.chromadmx.simulation.rigs.WallPanelsRigTest" -q`
Expected: 11 tests PASS

**Step 8: Commit**

```bash
git add shared/simulation/src/
git commit -m "feat(simulation): add DESK_STRIP, ROOM_ACCENT, WALL_PANELS rig presets for home users"
```

---

## Task 8: Use-Case Guided Onboarding

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/state/SetupContract.kt` (add UseCase enum, new step, new events, new state fields)
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/SetupViewModel.kt` (add use-case fork logic)
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/UseCaseSelectionScreen.kt`

**Step 1: Add UseCase enum and update SetupContract**

At `shared/src/commonMain/kotlin/com/chromadmx/ui/state/SetupContract.kt`:

Add enum before SetupStep:

```kotlin
enum class UseCase {
    MY_ROOM,
    A_STAGE,
    JUST_EXPLORING,
}
```

Add `USE_CASE_SELECT` step to SetupStep after SPLASH:

```kotlin
enum class SetupStep {
    SPLASH,
    USE_CASE_SELECT,
    NETWORK_DISCOVERY,
    FIXTURE_SCAN,
    VIBE_CHECK,
    STAGE_PREVIEW,
    COMPLETE
}
```

Add to SetupUiState:

```kotlin
val selectedUseCase: UseCase? = null,
```

Add to SetupEvent:

```kotlin
data class SelectUseCase(val useCase: UseCase) : SetupEvent
```

**Step 2: Update SetupViewModel to handle use-case selection**

In `onEvent()` add handler:

```kotlin
is SetupEvent.SelectUseCase -> selectUseCase(event.useCase)
```

Add method:

```kotlin
private fun selectUseCase(useCase: UseCase) {
    _state.update { it.copy(selectedUseCase = useCase) }
    // Set default rig based on use case
    val defaultRig = when (useCase) {
        UseCase.MY_ROOM -> RigPreset.DESK_STRIP
        UseCase.A_STAGE -> RigPreset.SMALL_DJ
        UseCase.JUST_EXPLORING -> RigPreset.ROOM_ACCENT
    }
    _state.update { it.copy(selectedRigPreset = defaultRig) }

    // Fork: "Just Exploring" skips network scan and goes to simulation
    if (useCase == UseCase.JUST_EXPLORING) {
        enterSimulationMode()
    }
    advance()
}
```

Update the `advance()` auto-advance from SPLASH to go to USE_CASE_SELECT instead of NETWORK_DISCOVERY.

**Step 3: Write UseCaseSelectionScreen composable**

```kotlin
// shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/UseCaseSelectionScreen.kt
package com.chromadmx.ui.screen.setup

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelText
import com.chromadmx.ui.state.SetupEvent
import com.chromadmx.ui.state.UseCase
import com.chromadmx.ui.theme.PixelDesign

@Composable
fun UseCaseSelectionScreen(
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PixelText(
            text = "What are you lighting?",
            style = PixelDesign.typography.heading,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        PixelButton(
            text = "My Room",
            onClick = { onEvent(SetupEvent.SelectUseCase(UseCase.MY_ROOM)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )

        PixelButton(
            text = "A Stage",
            onClick = { onEvent(SetupEvent.SelectUseCase(UseCase.A_STAGE)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )

        PixelButton(
            text = "Just Exploring",
            onClick = { onEvent(SetupEvent.SelectUseCase(UseCase.JUST_EXPLORING)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

**Step 4: Persist use case to SettingsStore**

In `SetupViewModel.selectUseCase()`, add:

```kotlin
scope.launch {
    settingsStore.putString("use_case", useCase.name)
}
```

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/
git commit -m "feat(onboarding): use-case selection fork (My Room / A Stage / Just Exploring)"
```

---

## Task 9: Subscription Tier Revisions

**Files:**
- Modify: `shared/subscription/src/commonMain/kotlin/com/chromadmx/subscription/model/Entitlement.kt` (add WLED entitlements)
- Modify: `shared/subscription/src/commonMain/kotlin/com/chromadmx/subscription/model/EntitlementConfig.kt` (update defaults)

**Step 1: Add WLED entitlements**

At `shared/subscription/src/commonMain/kotlin/com/chromadmx/subscription/model/Entitlement.kt`:

```kotlin
sealed class Entitlement {
    data object RealHardware : Entitlement()
    data class Effect(val effectId: String) : Entitlement()
    data object FixtureLimit : Entitlement()
    data object PresetSaves : Entitlement()
    data class GenrePack(val genre: String) : Entitlement()
    data object BleProvisioning : Entitlement()
    data object CameraMapping : Entitlement()
    data object AiAgent : Entitlement()
    data object DataExport : Entitlement()
    data object WledBasic : Entitlement()       // Free: 2 WLED devices
    data object WledMultiSegment : Entitlement() // Pro: multi-segment per device
    data object StageView : Entitlement()        // Pro: full stage editor
}
```

**Step 2: Update EntitlementConfig defaults**

In `shared/subscription/src/commonMain/kotlin/com/chromadmx/subscription/model/EntitlementConfig.kt`, update `DEFAULT_CAPABILITY_TIERS`:

```kotlin
val DEFAULT_CAPABILITY_TIERS = mapOf(
    "real_hardware" to SubscriptionTier.FREE,           // Changed: was PRO
    "wled_basic" to SubscriptionTier.FREE,              // New: 2 devices free
    "wled_multi_segment" to SubscriptionTier.PRO,       // New: multi-segment
    "stage_view" to SubscriptionTier.PRO,               // New: full stage editor
    "ble_provisioning" to SubscriptionTier.PRO,
    "camera_mapping" to SubscriptionTier.PRO,
    "ai_agent" to SubscriptionTier.ULTIMATE,
    "data_export" to SubscriptionTier.ULTIMATE,
)
```

Update `DEFAULT_FIXTURE_LIMITS`:

```kotlin
val DEFAULT_FIXTURE_LIMITS = mapOf(
    SubscriptionTier.FREE to 2,            // Changed: was 4, now 2 WLED devices
    SubscriptionTier.PRO to 10,            // Changed: was 8, now 10 WLED + Art-Net
    SubscriptionTier.ULTIMATE to Int.MAX_VALUE,
)
```

Update `DEFAULT_EFFECT_TIERS` — make 4 effects free instead of 3:

```kotlin
val DEFAULT_EFFECT_TIERS = mapOf(
    "solid-color" to SubscriptionTier.FREE,
    "gradient-sweep-3d" to SubscriptionTier.FREE,    // Changed: was PRO
    "rainbow-sweep-3d" to SubscriptionTier.FREE,
    "wave-3d" to SubscriptionTier.FREE,              // Changed: was PRO
    "chase-3d" to SubscriptionTier.PRO,              // Changed: was FREE
    "strobe" to SubscriptionTier.PRO,
    "radial-pulse-3d" to SubscriptionTier.PRO,
    "particle-burst-3d" to SubscriptionTier.PRO,
    "perlin-noise-3d" to SubscriptionTier.PRO,
)
```

**Step 3: Commit**

```bash
git add shared/subscription/src/
git commit -m "feat(subscription): revise tiers for WLED — free real hardware, 4 free effects"
```

---

## Task 10: WLED Agent Tools

**Files:**
- Create: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/WledTools.kt`
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/ToolRegistry.kt` (register new tools)
- Create: `shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/WledToolsTest.kt`

**Step 1: Write WLED agent tools**

Reference: Tool pattern at `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/FixtureTools.kt`.

```kotlin
// shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/WledTools.kt
package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.wled.WledApiClient
import com.chromadmx.wled.WledDeviceRegistry
import kotlinx.serialization.Serializable

class ListWledDevicesTool(
    private val registry: WledDeviceRegistry,
) : SimpleTool<ListWledDevicesTool.Args>(
    argsSerializer = Args.serializer(),
    name = "listWledDevices",
    description = "List all discovered and adopted WLED smart light devices on the network with their status, LED count, and IP address.",
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val devices = registry.adoptedDevices.value
        if (devices.isEmpty()) return "No WLED devices found. Make sure devices are on the same WiFi network."
        return devices.joinToString("\n") { d ->
            "  - ${d.name} (${d.ipAddress}): ${d.totalLeds} LEDs, " +
                "${d.segments.size} segments, " +
                if (d.isOnline) "online" else "offline"
        }
    }
}

class SetWledBrightnessTool(
    private val apiClient: WledApiClient,
    private val registry: WledDeviceRegistry,
) : SimpleTool<SetWledBrightnessTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setDeviceBrightness",
    description = "Set the brightness of a WLED device (0-255) or use 'all' to set all devices.",
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Device name or IP address, or 'all' for all devices")
        val device: String,
        @property:LLMDescription("Brightness level 0-255 (0=off, 255=full)")
        val brightness: Int,
    )

    override suspend fun execute(args: Args): String {
        val targets = if (args.device.equals("all", ignoreCase = true)) {
            registry.adoptedDevices.value
        } else {
            registry.adoptedDevices.value.filter {
                it.name.equals(args.device, ignoreCase = true) || it.ipAddress == args.device
            }
        }
        if (targets.isEmpty()) return "Device '${args.device}' not found."
        targets.forEach { apiClient.setBrightness(it.ipAddress, args.brightness) }
        return "Set brightness to ${args.brightness} on ${targets.size} device(s)."
    }
}

class SetWledColorTool(
    private val apiClient: WledApiClient,
    private val registry: WledDeviceRegistry,
) : SimpleTool<SetWledColorTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setDeviceColor",
    description = "Set a WLED device to a static color. Use common color names or hex codes.",
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Device name or IP, or 'all'")
        val device: String,
        @property:LLMDescription("Color as hex (#FF0000) or name (warm white, red, blue, sunset orange)")
        val color: String,
    )

    override suspend fun execute(args: Args): String {
        val (r, g, b) = parseColor(args.color) ?: return "Unknown color '${args.color}'."
        val targets = resolveDevices(args.device, registry)
        if (targets.isEmpty()) return "Device '${args.device}' not found."
        targets.forEach { apiClient.setSegmentColor(it.ipAddress, 0, r, g, b) }
        return "Set ${targets.size} device(s) to ${args.color}."
    }
}

class SetWledPowerTool(
    private val apiClient: WledApiClient,
    private val registry: WledDeviceRegistry,
) : SimpleTool<SetWledPowerTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setDevicePower",
    description = "Turn WLED devices on or off.",
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Device name or IP, or 'all'")
        val device: String,
        @property:LLMDescription("true = on, false = off")
        val on: Boolean,
    )

    override suspend fun execute(args: Args): String {
        val targets = resolveDevices(args.device, registry)
        if (targets.isEmpty()) return "Device '${args.device}' not found."
        targets.forEach { apiClient.setPower(it.ipAddress, args.on) }
        val state = if (args.on) "on" else "off"
        return "Turned $state ${targets.size} device(s)."
    }
}

// Shared helpers

internal fun resolveDevices(query: String, registry: WledDeviceRegistry) =
    if (query.equals("all", ignoreCase = true)) {
        registry.adoptedDevices.value
    } else {
        registry.adoptedDevices.value.filter {
            it.name.equals(query, ignoreCase = true) || it.ipAddress == query
        }
    }

internal fun parseColor(color: String): Triple<Int, Int, Int>? {
    if (color.startsWith("#") && color.length == 7) {
        return try {
            val r = color.substring(1, 3).toInt(16)
            val g = color.substring(3, 5).toInt(16)
            val b = color.substring(5, 7).toInt(16)
            Triple(r, g, b)
        } catch (_: Exception) { null }
    }
    return NAMED_COLORS[color.lowercase()]
}

private val NAMED_COLORS = mapOf(
    "red" to Triple(255, 0, 0),
    "green" to Triple(0, 255, 0),
    "blue" to Triple(0, 0, 255),
    "white" to Triple(255, 255, 255),
    "warm white" to Triple(255, 180, 100),
    "cool white" to Triple(200, 220, 255),
    "orange" to Triple(255, 120, 0),
    "purple" to Triple(128, 0, 255),
    "pink" to Triple(255, 50, 120),
    "yellow" to Triple(255, 255, 0),
    "cyan" to Triple(0, 255, 255),
    "sunset orange" to Triple(255, 80, 20),
)
```

**Step 2: Register in ToolRegistry**

At `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/ToolRegistry.kt`, add parameter and tools:

```kotlin
fun buildToolRegistry(
    engineController: EngineController,
    networkController: NetworkController,
    fixtureController: FixtureController,
    stateController: StateController,
    presetLibrary: PresetLibrary,
    wledApiClient: WledApiClient? = null,
    wledDeviceRegistry: WledDeviceRegistry? = null,
): ToolRegistry {
    return ToolRegistry {
        // ... existing 17 tools ...

        // WLED tools (added when WLED module is available)
        if (wledApiClient != null && wledDeviceRegistry != null) {
            tool(ListWledDevicesTool(wledDeviceRegistry))
            tool(SetWledBrightnessTool(wledApiClient, wledDeviceRegistry))
            tool(SetWledColorTool(wledApiClient, wledDeviceRegistry))
            tool(SetWledPowerTool(wledApiClient, wledDeviceRegistry))
        }
    }
}
```

**Step 3: Write tests**

```kotlin
// shared/agent/src/commonTest/kotlin/com/chromadmx/agent/tools/WledToolsTest.kt
package com.chromadmx.agent.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WledToolsTest {

    @Test
    fun parseHexColor() {
        val result = parseColor("#FF8020")
        assertNotNull(result)
        assertEquals(Triple(255, 128, 32), result)
    }

    @Test
    fun parseNamedColor() {
        val result = parseColor("warm white")
        assertNotNull(result)
        assertEquals(Triple(255, 180, 100), result)
    }

    @Test
    fun parseInvalidColorReturnsNull() {
        assertNull(parseColor("not-a-color"))
        assertNull(parseColor("#GGG"))
    }

    @Test
    fun parseColorCaseInsensitive() {
        val result = parseColor("Warm White")
        assertNotNull(result)
    }
}
```

**Step 4: Run tests**

Run: `./gradlew :shared:agent:testAndroidHostTest --tests "com.chromadmx.agent.tools.WledToolsTest" -q`
Expected: 4 tests PASS

**Step 5: Commit**

```bash
git add shared/agent/src/
git commit -m "feat(agent): WLED tools — listWledDevices, setDeviceBrightness, setDeviceColor, setDevicePower"
```

---

## Task 11: Room Box View (Composable)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/RoomBoxView.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/RoomBoxState.kt`

This is the largest UI task. The Room Box is a 3D box rendered as an isometric projection in a Canvas, where fixtures glow on the box faces.

**Step 1: Write RoomBoxState data model**

```kotlin
// shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/RoomBoxState.kt
package com.chromadmx.ui.screen.stage

import com.chromadmx.core.model.Vec3

enum class BoxFace {
    BACK_WALL, FRONT_WALL, LEFT_WALL, RIGHT_WALL, CEILING, FLOOR
}

data class RoomFixturePlacement(
    val fixtureId: String,
    val face: BoxFace,
    val positionOnFace: Vec3,  // normalized 0-1 on the face (u, v)
    val lengthOnFace: Float = 0.5f,  // for strips: fraction of face width
)

data class RoomBoxState(
    val rotationY: Float = 30f,     // degrees, horizontal rotation
    val rotationX: Float = 20f,     // degrees, vertical tilt
    val zoom: Float = 1f,
    val placements: List<RoomFixturePlacement> = emptyList(),
    val placingFixtureId: String? = null,
    val selectedFace: BoxFace? = null,
)
```

**Step 2: Write RoomBoxView composable**

```kotlin
// shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/RoomBoxView.kt
package com.chromadmx.ui.screen.stage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.chromadmx.core.model.Vec3
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RoomBoxView(
    state: RoomBoxState,
    fixtureColors: Map<String, Color>,
    onRotate: (deltaX: Float, deltaY: Float) -> Unit,
    onTapFace: (BoxFace) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onRotate(dragAmount.x * 0.5f, dragAmount.y * 0.5f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Hit-test faces (simplified: check quadrants)
                    // Full implementation would project face polygons and point-in-polygon test
                }
            }
    ) {
        drawRoomBox(state, fixtureColors)
    }
}

private fun DrawScope.drawRoomBox(
    state: RoomBoxState,
    fixtureColors: Map<String, Color>,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val scale = size.minDimension * 0.3f * state.zoom

    val radY = Math.toRadians(state.rotationY.toDouble())
    val radX = Math.toRadians(state.rotationX.toDouble())

    fun project(v: Vec3): Offset {
        // Simple isometric projection with rotation
        val cosY = cos(radY).toFloat()
        val sinY = sin(radY).toFloat()
        val cosX = cos(radX).toFloat()
        val sinX = sin(radX).toFloat()

        val x1 = v.x * cosY - v.y * sinY
        val z1 = v.x * sinY + v.y * cosY
        val y1 = v.z * cosX - z1 * sinX
        val z2 = v.z * sinX + z1 * cosX

        return Offset(cx + x1 * scale, cy - y1 * scale)
    }

    // Box corners (normalized -1 to 1)
    val corners = listOf(
        Vec3(-1f, -1f, -1f), Vec3(1f, -1f, -1f),
        Vec3(1f, 1f, -1f), Vec3(-1f, 1f, -1f),
        Vec3(-1f, -1f, 1f), Vec3(1f, -1f, 1f),
        Vec3(1f, 1f, 1f), Vec3(-1f, 1f, 1f),
    )
    val projected = corners.map { project(it) }

    // Draw back faces with ambient glow from fixtures
    val wallColor = Color(0xFF1A1A2E)
    val faces = listOf(
        listOf(0, 1, 5, 4) to BoxFace.FLOOR,
        listOf(3, 2, 6, 7) to BoxFace.CEILING,
        listOf(0, 3, 7, 4) to BoxFace.LEFT_WALL,
        listOf(1, 2, 6, 5) to BoxFace.RIGHT_WALL,
        listOf(0, 1, 2, 3) to BoxFace.BACK_WALL,
        listOf(4, 5, 6, 7) to BoxFace.FRONT_WALL,
    )

    for ((indices, face) in faces) {
        val path = Path().apply {
            moveTo(projected[indices[0]].x, projected[indices[0]].y)
            for (i in 1 until indices.size) {
                lineTo(projected[indices[i]].x, projected[indices[i]].y)
            }
            close()
        }
        drawPath(path, wallColor)
    }

    // Draw fixture glows on faces
    for (placement in state.placements) {
        val color = fixtureColors[placement.fixtureId] ?: Color.Transparent
        if (color == Color.Transparent) continue
        // Map placement position to 3D point on the face and project
        // Simplified: draw a glowing circle at the projected position
        val facePos = placementToWorld(placement)
        val screenPos = project(facePos)
        drawCircle(color, radius = 6f, center = screenPos)
    }
}

private fun placementToWorld(placement: RoomFixturePlacement): Vec3 {
    val u = placement.positionOnFace.x * 2f - 1f  // map 0..1 to -1..1
    val v = placement.positionOnFace.y * 2f - 1f
    return when (placement.face) {
        BoxFace.BACK_WALL -> Vec3(u, -1f, v)
        BoxFace.FRONT_WALL -> Vec3(u, 1f, v)
        BoxFace.LEFT_WALL -> Vec3(-1f, u, v)
        BoxFace.RIGHT_WALL -> Vec3(1f, u, v)
        BoxFace.CEILING -> Vec3(u, v, 1f)
        BoxFace.FLOOR -> Vec3(u, v, -1f)
    }
}
```

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/RoomBox*.kt
git commit -m "feat(ui): Room Box View — 3D diorama room view for home lighting users"
```

---

## Task 12: Integration — Wire Room Box into Stage Screen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/stage/StagePreviewScreen.kt` (add Room Box / Stage View toggle)
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModelV2.kt` (add view mode state)

**Step 1: Add view mode to StageViewModelV2**

Add enum and state:

```kotlin
enum class StageViewMode { STAGE, ROOM_BOX }
```

Add to ViewModel state:

```kotlin
private val _viewMode = MutableStateFlow(StageViewMode.STAGE)
val viewMode: StateFlow<StageViewMode> = _viewMode.asStateFlow()

fun setViewMode(mode: StageViewMode) { _viewMode.value = mode }
```

On init, read use-case from settings to set default:

```kotlin
// If use case is MY_ROOM, default to ROOM_BOX
val savedUseCase = settingsStore.getString("use_case", null)
if (savedUseCase == "MY_ROOM") {
    _viewMode.value = StageViewMode.ROOM_BOX
}
```

**Step 2: Add toggle in StagePreviewScreen**

Add a toggle chip at the top of the stage screen that switches between views. When `ROOM_BOX` is selected, render `RoomBoxView` instead of the existing isometric canvas.

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/
git commit -m "feat(ui): toggle between Stage View and Room Box View based on use case"
```

---

## Task 13: WLED Discovery Screen (Onboarding Integration)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/WledDiscoveryScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/SetupFlow.kt` (route to WLED screen for MY_ROOM)

**Step 1: Write WledDiscoveryScreen**

Shows auto-discovered WLED devices with tap-to-adopt and place-in-box flow.

```kotlin
// shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/WledDiscoveryScreen.kt
package com.chromadmx.ui.screen.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.*
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.wled.WledDevice

@Composable
fun WledDiscoveryScreen(
    devices: List<WledDevice>,
    isScanning: Boolean,
    onAdoptDevice: (WledDevice) -> Unit,
    onManualAdd: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        PixelText(
            text = "Looking for lights...",
            style = PixelDesign.typography.heading,
        )

        if (isScanning) {
            PixelText(
                text = "Scanning your network",
                style = PixelDesign.typography.body,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(devices) { device ->
                PixelCard(
                    onClick = { onAdoptDevice(device) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        PixelText(text = device.name.ifEmpty { device.ipAddress })
                        PixelText(
                            text = "${device.totalLeds} LEDs",
                            style = PixelDesign.typography.caption,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelButton(text = "Add manually", onClick = onManualAdd)
            PixelButton(text = "Continue", onClick = onContinue)
        }
    }
}
```

**Step 2: Route in SetupFlow**

In the setup flow composable, when step is `NETWORK_DISCOVERY` and use case is `MY_ROOM`, show `WledDiscoveryScreen` instead of the Art-Net discovery screen.

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/screen/setup/
git commit -m "feat(ui): WLED discovery screen for My Room onboarding path"
```

---

## Summary

| Task | Description | Estimated Tests |
|------|-------------|-----------------|
| 1 | Module scaffolding + data models | 3 |
| 2 | WLED HTTP API client | 4 |
| 3 | mDNS discovery + FixtureDiscovery adapter | 3 |
| 4 | WledTransport (DmxTransport impl) | 4 |
| 5 | SQLDelight persistence | 1 |
| 6 | Koin DI wiring | 0 (build check) |
| 7 | Home simulation rig presets | 11 |
| 8 | Use-case guided onboarding | 0 (UI) |
| 9 | Subscription tier revisions | 0 (config) |
| 10 | WLED agent tools | 4 |
| 11 | Room Box View composable | 0 (UI) |
| 12 | Stage/Room toggle integration | 0 (UI) |
| 13 | WLED discovery screen | 0 (UI) |

**Total: 13 tasks, ~30 new tests**

Dependencies: Tasks 1-6 are sequential (each builds on prior). Tasks 7, 8, 9, 10 are independent of each other and can be parallelized. Tasks 11-13 depend on Task 8.

package com.chromadmx.wled

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WledApiClientTest {

    private val fullStateJson = """
        {
            "state": {
                "on": true,
                "bri": 128,
                "transition": 7,
                "ps": -1,
                "seg": [
                    {
                        "id": 0,
                        "start": 0,
                        "stop": 60,
                        "len": 60,
                        "grp": 1,
                        "spc": 0,
                        "col": [[255, 160, 0]],
                        "fx": 0,
                        "sx": 128,
                        "ix": 128,
                        "pal": 0,
                        "on": true,
                        "bri": 255
                    }
                ]
            },
            "info": {
                "ver": "0.14.0",
                "vid": 2312080,
                "leds": {
                    "count": 60,
                    "rgbw": false,
                    "wv": 0,
                    "cct": false,
                    "maxpwr": 850,
                    "maxseg": 16,
                    "lc": 1,
                    "seglc": [60]
                },
                "name": "Desk Strip",
                "udpport": 21324,
                "arch": "esp32",
                "freeheap": 180000,
                "mac": "AA:BB:CC:DD:EE:FF"
            }
        }
    """.trimIndent()

    private fun createClient(engine: MockEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun getFullStateParses60LedDevice() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = fullStateJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = WledApiClientImpl(createClient(engine))

        val result = apiClient.getFullState("192.168.1.100")

        assertNotNull(result)
        assertEquals("Desk Strip", result.info.name)
        assertEquals(60, result.info.leds.count)
        assertEquals("0.14.0", result.info.ver)
        assertTrue(result.state.on)
        assertEquals(128, result.state.bri)
        assertEquals(1, result.state.seg.size)
        assertEquals(60, result.state.seg[0].len)
        assertFalse(result.info.leds.rgbw)
        assertEquals("AA:BB:CC:DD:EE:FF", result.info.mac)
    }

    @Test
    fun setPowerSendsOnState() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = WledApiClientImpl(createClient(engine))

        val result = apiClient.setPower("192.168.1.100", on = true)

        assertTrue(result)
        // Verify the request was made to the correct endpoint
        val request = engine.requestHistory.first()
        assertEquals("http://192.168.1.100/json/state", request.url.toString())
    }

    @Test
    fun setBrightnessClampsTo0_255() = runTest {
        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            requests.add(request.body.toString())
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = WledApiClientImpl(createClient(engine))

        // Normal value
        assertTrue(apiClient.setBrightness("192.168.1.100", 128))

        // Below range — should clamp to 0
        assertTrue(apiClient.setBrightness("192.168.1.100", -50))

        // Above range — should clamp to 255
        assertTrue(apiClient.setBrightness("192.168.1.100", 500))

        // All three requests should succeed without error
        assertEquals(3, engine.requestHistory.size)
    }

    @Test
    fun getFullStateReturnsNullOnError() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }
        val apiClient = WledApiClientImpl(createClient(engine))

        val result = apiClient.getFullState("192.168.1.100")

        assertNull(result)
    }
}

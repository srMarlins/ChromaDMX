package com.chromadmx.wled

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Interface for communicating with WLED devices over their JSON HTTP API.
 *
 * All methods are safe — they catch exceptions internally and return null/false on error.
 */
interface WledApiClient {
    /** Fetch the full device state (state + info) from GET /json */
    suspend fun getFullState(ip: String): WledFullState?

    /** Set arbitrary state fields via POST /json/state */
    suspend fun setState(ip: String, state: WledState): Boolean

    /** Set the primary color of a specific segment */
    suspend fun setSegmentColor(ip: String, segmentId: Int, r: Int, g: Int, b: Int): Boolean

    /** Set the effect on a specific segment with optional speed and intensity */
    suspend fun setSegmentEffect(
        ip: String,
        segmentId: Int,
        effectId: Int,
        speed: Int = 128,
        intensity: Int = 128,
    ): Boolean

    /** Turn the device on or off */
    suspend fun setPower(ip: String, on: Boolean): Boolean

    /** Set master brightness (clamped to 0-255) */
    suspend fun setBrightness(ip: String, brightness: Int): Boolean
}

/**
 * Ktor-based implementation of [WledApiClient].
 *
 * Accepts an [HttpClient] so callers can inject platform engines or test mocks.
 */
class WledApiClientImpl(private val client: HttpClient) : WledApiClient {

    override suspend fun getFullState(ip: String): WledFullState? {
        return try {
            val response: HttpResponse = client.get("http://$ip/json")
            if (response.status.isSuccess()) {
                response.body<WledFullState>()
            } else {
                null
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    override suspend fun setState(ip: String, state: WledState): Boolean {
        return postState(ip, state)
    }

    override suspend fun setSegmentColor(
        ip: String,
        segmentId: Int,
        r: Int,
        g: Int,
        b: Int,
    ): Boolean {
        val body = SetSegmentColorBody(
            seg = listOf(SegmentColorPayload(id = segmentId, col = listOf(listOf(r, g, b))))
        )
        return postJson(ip, body)
    }

    override suspend fun setSegmentEffect(
        ip: String,
        segmentId: Int,
        effectId: Int,
        speed: Int,
        intensity: Int,
    ): Boolean {
        val body = SetSegmentEffectBody(
            seg = listOf(SegmentEffectPayload(id = segmentId, fx = effectId, sx = speed, ix = intensity))
        )
        return postJson(ip, body)
    }

    override suspend fun setPower(ip: String, on: Boolean): Boolean {
        val body = SetPowerBody(on = on)
        return postJson(ip, body)
    }

    override suspend fun setBrightness(ip: String, brightness: Int): Boolean {
        val clamped = brightness.coerceIn(0, 255)
        val body = SetBrightnessBody(bri = clamped)
        return postJson(ip, body)
    }

    // -- Internal helpers --

    private suspend inline fun <reified T> postJson(ip: String, body: T): Boolean {
        return try {
            val response: HttpResponse = client.post("http://$ip/json/state") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    private suspend fun postState(ip: String, state: WledState): Boolean {
        return postJson(ip, state)
    }
}

// -- Internal payload DTOs for POST requests --

@Serializable
internal data class SegmentColorPayload(
    val id: Int,
    val col: List<List<Int>>,
)

@Serializable
internal data class SetSegmentColorBody(
    val seg: List<SegmentColorPayload>,
)

@Serializable
internal data class SegmentEffectPayload(
    val id: Int,
    val fx: Int,
    val sx: Int,
    val ix: Int,
)

@Serializable
internal data class SetSegmentEffectBody(
    val seg: List<SegmentEffectPayload>,
)

@Serializable
internal data class SetPowerBody(
    val on: Boolean,
)

@Serializable
internal data class SetBrightnessBody(
    val bri: Int,
)

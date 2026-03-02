package com.chromadmx.wled.di

import com.chromadmx.networking.DmxTransport
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.wled.WledApiClient
import com.chromadmx.wled.WledApiClientImpl
import com.chromadmx.wled.WledDiscovery
import com.chromadmx.wled.WledRepository
import com.chromadmx.wled.WledTransport
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin DI module for WLED integration.
 *
 * Provides:
 * - [WledApiClient] backed by a Ktor [HttpClient] with JSON content negotiation
 * - [WledRepository] for persistent device storage
 * - [WledDiscovery] (named "wled") as a [FixtureDiscovery] for mDNS browsing
 * - [WledTransport] (named "wled") as a [DmxTransport] bridging DMX frames to WLED HTTP API
 *
 * Note: [com.chromadmx.wled.WledMdnsBrowser] and [com.chromadmx.wled.WledDeviceRegistry]
 * are platform-specific or higher-level dependencies that must be provided by the
 * platform module (Android/iOS) or composed at the app level.
 */
val wledModule = module {

    // --- HTTP client for WLED JSON API ---
    single(named("wled")) {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    // --- API client ---
    single<WledApiClient> { WledApiClientImpl(client = get(named("wled"))) }

    // --- Persistence ---
    single { WledRepository(db = get()) }

    // --- Discovery (requires platform WledMdnsBrowser) ---
    single(named("wled")) {
        WledDiscovery(
            browser = get(),
            apiClient = get(),
            scope = get(),
        )
    } bind FixtureDiscovery::class

    // --- Transport (requires WledDeviceRegistry from app layer) ---
    single(named("wled")) {
        WledTransport(
            apiClient = get(),
            registry = get(),
            scope = get(),
        )
    } bind DmxTransport::class
}

package com.chromadmx.agent.di

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.agent.config.ApiKeyProvider
import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.controller.NetworkController
import com.chromadmx.agent.controller.RealEngineController
import com.chromadmx.agent.controller.RealFixtureController
import com.chromadmx.agent.controller.RealNetworkController
import com.chromadmx.agent.controller.RealStateController
import com.chromadmx.agent.controller.StateController
import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.agent.scene.SceneStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin DI module for the AI lighting agent.
 *
 * Provides:
 * - [ApiKeyProvider] — platform-specific API key lookup
 * - [AgentConfig] — agent configuration (API key, temperature, etc.)
 * - [SceneStore] — in-memory scene persistence
 * - [EngineController] — bridges to the effect engine
 * - [NetworkController] — bridges to node discovery
 * - [FixtureController] — bridges to fixture management
 * - [StateController] — bridges to engine/tempo/network state
 * - [LightingAgent] — the main agent with tool registry
 * - [PreGenerationService] — batch scene generation
 *
 * Dependencies from other modules (must be provided):
 * - EffectStack, EffectRegistry (from :shared:engine)
 * - NodeDiscovery, DmxOutputService (from :shared:networking)
 * - BeatClock (from :shared:tempo)
 * - EffectEngine (from :shared:engine)
 * - Fixture list provider
 *
 * Usage:
 * ```
 * startKoin {
 *     modules(engineModule, networkingModule, tempoModule, agentModule)
 * }
 * ```
 */
val agentModule: Module = module {
    single { ApiKeyProvider() }
    single { AgentConfig(apiKey = get<ApiKeyProvider>().getAnthropicKey() ?: "") }
    single { SceneStore() }
    single<EngineController> { RealEngineController(get(), get()) }
    single<NetworkController> { RealNetworkController(get()) }
    single<FixtureController> { RealFixtureController(fixturesProvider = { emptyList() }) }
    single<StateController> { RealStateController(get(), get(), get(), get()) }
    single { LightingAgent(get(), get(), get(), get(), get(), get()) }
    single { PreGenerationService(get()) }
}

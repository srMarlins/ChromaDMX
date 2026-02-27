package com.chromadmx.agent.di

import ai.koog.agents.core.tools.ToolRegistry
import com.chromadmx.agent.LightingAgentInterface
import com.chromadmx.agent.LightingAgentService
import com.chromadmx.agent.SimulatedLightingAgent
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
import com.chromadmx.agent.tools.buildToolRegistry
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.engine.effect.EffectRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

val agentModule: Module = module {
    single { ApiKeyProvider() }
    single {
        val provider = get<ApiKeyProvider>()
        val googleKey = provider.getGoogleKey()
        val anthropicKey = provider.getAnthropicKey()
        when {
            !googleKey.isNullOrBlank() -> AgentConfig(apiKey = googleKey, modelId = "gemini_2_5_flash")
            !anthropicKey.isNullOrBlank() -> AgentConfig(apiKey = anthropicKey, modelId = "sonnet_4_5")
            else -> AgentConfig()
        }
    }
    single<EngineController> { RealEngineController(get(), get()) }
    single<NetworkController> { RealNetworkController(get()) }
    single<FixtureController> {
        val fixturesProvider: () -> List<Fixture3D> = getOrNull() ?: { emptyList() }
        RealFixtureController(fixturesProvider = fixturesProvider)
    }
    single<StateController> {
        val fixturesProvider: () -> List<Fixture3D> = getOrNull() ?: { emptyList() }
        RealStateController(get(), get(), get(), get(), fixturesProvider)
    }
    single<ToolRegistry> {
        buildToolRegistry(
            engineController = get(),
            networkController = get(),
            fixtureController = get(),
            stateController = get(),
            presetLibrary = get()
        )
    }

    // LightingAgentInterface — correct per-message lifecycle when API key is available.
    // When no key is configured, provides SimulatedLightingAgent (keyword-matching,
    // executes real controller operations without an LLM).
    single<LightingAgentInterface> {
        val config = get<AgentConfig>()
        if (config.isAvailable) {
            LightingAgentService(config, get())
        } else {
            SimulatedLightingAgent(
                engineController = get(),
                stateController = get(),
                presetLibrary = get(),
                effectRegistry = get<EffectRegistry>(),
            )
        }
    }

    single { PreGenerationService(get()) }
}

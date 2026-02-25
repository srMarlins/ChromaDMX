package com.chromadmx.agent.di

import ai.koog.agents.core.tools.ToolRegistry
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
import com.chromadmx.agent.tools.buildToolRegistry
import com.chromadmx.core.model.Fixture3D
import org.koin.core.module.Module
import org.koin.dsl.module

val agentModule: Module = module {
    single { ApiKeyProvider() }
    single { AgentConfig(apiKey = get<ApiKeyProvider>().getAnthropicKey() ?: "") }
    single { SceneStore() }
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
            sceneStore = get()
        )
    }
    single { LightingAgent(get(), get()) }
    single { PreGenerationService(get()) }
}

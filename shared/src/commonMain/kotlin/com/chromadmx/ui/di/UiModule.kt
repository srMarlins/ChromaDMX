package com.chromadmx.ui.di

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.ui.viewmodel.AgentViewModel
import com.chromadmx.ui.viewmodel.MapViewModel
import com.chromadmx.ui.viewmodel.NetworkViewModel
import com.chromadmx.ui.viewmodel.PerformViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Koin module for UI ViewModels.
 *
 * Each ViewModel is scoped as a factory so a new instance is created
 * per screen composition. The CoroutineScope uses SupervisorJob + Dispatchers.Default
 * and is cancelled when the Koin application closes.
 *
 * Dependencies:
 * - PerformViewModel requires: EffectEngine, EffectRegistry, BeatClock
 * - NetworkViewModel requires: NodeDiscovery
 * - MapViewModel: standalone
 * - AgentViewModel: requires LightingAgent, PreGenerationService
 */
val uiModule = module {
    // Shared UI coroutine scope — uses SupervisorJob so child failures don't cancel siblings
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    factory {
        PerformViewModel(
            engine = get(),
            effectRegistry = get(),
            beatClock = get(),
            scope = get(),
        )
    }

    factory {
        NetworkViewModel(
            nodeDiscovery = get(),
            scope = get(),
        )
    }

    factory {
        MapViewModel(
            scope = get(),
        )
    }

    factory {
        AgentViewModel(
            agent = get(),
            preGenService = get(),
            scope = get(),
        )
    }
}

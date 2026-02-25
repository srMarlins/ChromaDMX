package com.chromadmx.ui.di

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.ui.viewmodel.AgentViewModel
import com.chromadmx.ui.viewmodel.MapViewModel
import com.chromadmx.ui.viewmodel.NetworkViewModel
import com.chromadmx.ui.viewmodel.PerformViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Koin module for UI ViewModels.
 *
 * Each ViewModel is scoped as a factory so a new instance is created
 * per screen composition. A child [SupervisorJob] is created per ViewModel
 * so its coroutines can be cancelled independently via [onCleared].
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
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(childJob + Dispatchers.Default)
        PerformViewModel(
            engine = get(),
            effectRegistry = get(),
            beatClock = get(),
            scope = vmScope,
        )
    }

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(childJob + Dispatchers.Default)
        NetworkViewModel(
            nodeDiscovery = get(),
            scope = vmScope,
        )
    }

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(childJob + Dispatchers.Default)
        MapViewModel(
            scope = vmScope,
        )
    }

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(childJob + Dispatchers.Default)
        AgentViewModel(
            agent = get(),
            preGenService = get(),
            scope = vmScope,
        )
    }
}

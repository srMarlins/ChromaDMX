package com.chromadmx.ui.di

import com.chromadmx.ui.components.network.NetworkHealthViewModel
import com.chromadmx.ui.viewmodel.AgentViewModel
import com.chromadmx.ui.viewmodel.MascotViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModel
import com.chromadmx.ui.viewmodel.StageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Koin module for UI ViewModels.
 *
 * [StageViewModel], [SettingsViewModel], and [AgentViewModel] are scoped as
 * singletons so they survive navigation round-trips and panel open/close cycles.
 * [MascotViewModel] is a factory -- each composition gets its own instance.
 *
 * A child [SupervisorJob] is created per ViewModel so its coroutines can be
 * cancelled independently via [onCleared].
 *
 * Dependencies:
 * - StageViewModel requires: EffectEngine, EffectRegistry, PresetLibrary, BeatClock
 * - SettingsViewModel requires: NodeDiscovery
 * - AgentViewModel requires: LightingAgent, PreGenerationService
 * - MascotViewModel requires: BeatClock
 * - NetworkHealthViewModel requires: NodeDiscovery, MascotViewModel (optional)
 */
val uiModule = module {
    // CoroutineScope provided by chromaDiModule

    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        StageViewModel(
            engine = get(),
            effectRegistry = get(),
            presetLibrary = get(),
            beatClock = get(),
            scope = vmScope,
        )
    }

    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        SettingsViewModel(
            nodeDiscovery = get(),
            scope = vmScope,
        )
    }

    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        AgentViewModel(
            agent = get(),
            preGenService = get(),
            scope = vmScope,
        )
    }

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        MascotViewModel(
            scope = vmScope,
            beatClock = get(),
        )
    }

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        NetworkHealthViewModel(
            nodeDiscovery = get(),
            mascotViewModel = getOrNull(),
            scope = vmScope,
        )
    }
}

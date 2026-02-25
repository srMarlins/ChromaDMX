package com.chromadmx.ui.di

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
 * [StageViewModel] and [SettingsViewModel] are scoped as singletons so
 * they survive navigation round-trips (e.g., StagePreview -> Settings -> back).
 * [AgentViewModel] remains a factory -- each chat session can be independent.
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

    factory {
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
}

package com.chromadmx.ui.di

import com.chromadmx.ui.viewmodel.AgentViewModel
import com.chromadmx.ui.viewmodel.MapViewModel
import com.chromadmx.ui.viewmodel.NetworkViewModel
import com.chromadmx.ui.viewmodel.PerformViewModel
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
 * Each ViewModel is scoped as a factory so a new instance is created
 * per screen composition. A child [SupervisorJob] is created per ViewModel
 * so its coroutines can be cancelled independently via [onCleared].
 *
 * Dependencies:
 * - StageViewModel requires: EffectEngine, EffectRegistry, BeatClock
 * - SettingsViewModel: standalone
 * - AgentViewModel: requires LightingAgent, PreGenerationService
 * - PerformViewModel (legacy): EffectEngine, EffectRegistry, BeatClock
 * - NetworkViewModel (legacy): NodeDiscovery
 * - MapViewModel (legacy): standalone
 */
val uiModule = module {
    // CoroutineScope provided by chromaDiModule

    // --- New ViewModels (Screen Architecture Overhaul #20) ---

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        StageViewModel(
            engine = get(),
            effectRegistry = get(),
            beatClock = get(),
            scope = vmScope,
        )
    }

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        SettingsViewModel(scope = vmScope)
    }

    // --- Kept ViewModels ---

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

    // --- Legacy ViewModels (will be removed after full migration) ---

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
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
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        NetworkViewModel(
            nodeDiscovery = get(),
            scope = vmScope,
        )
    }

    factory {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        MapViewModel(
            scope = vmScope,
        )
    }
}

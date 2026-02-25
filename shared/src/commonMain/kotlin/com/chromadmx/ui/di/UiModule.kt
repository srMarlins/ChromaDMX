package com.chromadmx.ui.di

import com.chromadmx.ui.viewmodel.AgentViewModel
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
 */
val uiModule = module {
    // CoroutineScope provided by chromaDiModule

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
}

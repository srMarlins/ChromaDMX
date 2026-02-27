package com.chromadmx.ui.di

import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.core.persistence.FixtureRepository
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.NetworkStateStore
import com.chromadmx.networking.ble.BleProvisioningService
import com.chromadmx.ui.navigation.AppStateManager
import com.chromadmx.ui.viewmodel.MascotViewModelV2
import com.chromadmx.ui.viewmodel.ProvisioningViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModelV2
import com.chromadmx.ui.viewmodel.SetupViewModel
import com.chromadmx.ui.viewmodel.StageViewModelV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.module

/**
 * Koin module for UI ViewModels and navigation.
 *
 * All ViewModels follow the UDF pattern with a single [state] flow
 * and a single [onEvent] entry point. [AppStateManager] controls the
 * 4-screen navigation (Setup -> Stage <-> Settings <-> Provisioning).
 *
 * A child [SupervisorJob] is created per ViewModel so its coroutines
 * can be cancelled independently via [onCleared].
 *
 * Dependencies:
 * - AppStateManager requires: SettingsRepository, FixtureRepository
 * - SetupViewModel requires: FixtureDiscovery, FixtureStore, SettingsStore
 * - StageViewModelV2 requires: EffectEngine, EffectRegistry, PresetLibrary,
 *     BeatClock, FixtureDiscovery, NodeDiscovery (optional)
 * - SettingsViewModelV2 requires: SettingsStore, DmxTransportRouter, FixtureDiscovery
 * - MascotViewModelV2 requires: BeatClock, knownNodesFlow, LightingAgent (optional)
 * - ProvisioningViewModel requires: BleProvisioningService (optional)
 */
val uiModule = module {
    // CoroutineScope provided by chromaDiModule

    // --- Navigation ---
    single {
        AppStateManager(
            settingsRepository = get(),
            fixtureRepository = get(),
            scope = get(),
        )
    }

    // --- Setup ---
    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        SetupViewModel(
            fixtureDiscovery = get(),
            fixtureStore = get(),
            settingsStore = get(),
            networkStateRepository = getOrNull<NetworkStateStore>(),
            preGenerationService = getOrNull<PreGenerationService>(),
            scope = vmScope,
        )
    }

    // --- Stage ---
    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        StageViewModelV2(
            engine = get(),
            effectRegistry = get(),
            presetLibrary = get(),
            beatClock = get(),
            fixtureDiscovery = get(),
            nodeDiscovery = getOrNull(),
            scope = vmScope,
            fixtureRepository = getOrNull<FixtureRepository>(),
            fixtureController = getOrNull<FixtureController>(),
        )
    }

    // --- Settings ---
    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        SettingsViewModelV2(
            settingsRepository = get(),
            transportRouter = get(),
            fixtureDiscovery = get(),
            scope = vmScope,
            fixtureStore = getOrNull<FixtureStore>(),
        )
    }

    // --- Mascot ---
    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        val networkStateStore = getOrNull<NetworkStateStore>()
        MascotViewModelV2(
            beatClock = get(),
            knownNodesFlow = networkStateStore?.knownNodes() ?: flowOf(emptyList()),
            lightingAgent = getOrNull<LightingAgent>(),
            scope = vmScope,
        )
    }

    // --- BLE Provisioning ---
    single {
        BleProvisioningService(scanner = get(), provisioner = get())
    }

    single {
        val parentScope: CoroutineScope = get()
        val childJob = SupervisorJob(parentScope.coroutineContext[Job])
        val vmScope = CoroutineScope(Dispatchers.Default + childJob)
        ProvisioningViewModel(
            service = getOrNull<BleProvisioningService>(),
            scope = vmScope,
        )
    }
}

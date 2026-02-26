package com.chromadmx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.ui.mascot.MascotOverlay
import com.chromadmx.ui.util.presetDisplayName
import com.chromadmx.ui.navigation.AppState
import com.chromadmx.ui.navigation.AppStateManager
import com.chromadmx.ui.screen.chat.ChatPanel
import com.chromadmx.ui.screen.onboarding.OnboardingFlow
import com.chromadmx.ui.screen.settings.ProvisioningScreen
import com.chromadmx.ui.screen.settings.SettingsScreen
import com.chromadmx.ui.screen.simulation.RigPresetSelector
import com.chromadmx.ui.screen.stage.StagePreviewScreen
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.viewmodel.AgentViewModel
import com.chromadmx.ui.viewmodel.MascotViewModel
import com.chromadmx.ui.viewmodel.OnboardingViewModel
import com.chromadmx.ui.viewmodel.ProvisioningViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModelV2
import com.chromadmx.ui.viewmodel.StageViewModel
import org.koin.compose.getKoin

/**
 * Root composable for the ChromaDMX application.
 *
 * Uses [AppStateManager] for navigation: Onboarding -> StagePreview <-> Settings.
 * No tab bar. Single main screen with contextual overlays.
 *
 * On first launch the [OnboardingViewModel] drives a 6-step flow
 * (Splash -> NetworkDiscovery -> FixtureScan -> VibeCheck -> StagePreview -> Complete).
 * On repeat launches the app starts directly at StagePreview, performs a quick
 * network health check, and the mascot alerts if the network topology changed.
 *
 * Simulation mode is coordinated between [SettingsViewModel] (toggle/preset) and
 * [StageViewModel] (badge visibility, fixture count). The rig selector is accessible
 * from both the onboarding flow and the settings screen.
 */
@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        val onboardingVm = resolveOrNull<OnboardingViewModel>()
        val isFirstLaunch = remember { onboardingVm?.isFirstLaunch() ?: false }
        val appStateManager = remember { AppStateManager(isFirstLaunch = isFirstLaunch) }
        val currentState by appStateManager.currentState.collectAsState()

        val settingsVm = resolveOrNull<SettingsViewModel>()
        val settingsVmV2 = resolveOrNull<SettingsViewModelV2>()
        val stageVm = resolveOrNull<StageViewModel>()
        val mascotVm = resolveOrNull<MascotViewModel>()

        // Repeat launch: quick network health check + mascot alert
        if (!isFirstLaunch && onboardingVm != null) {
            LaunchedEffect(Unit) {
                onboardingVm.performRepeatLaunchCheck()
            }

            val networkChanged by onboardingVm.networkChanged.collectAsState()
            val repeatCheckDone by onboardingVm.repeatLaunchComplete.collectAsState()

            if (repeatCheckDone && networkChanged && mascotVm != null) {
                LaunchedEffect(Unit) {
                    mascotVm.triggerAlert("Network has changed since last session!")
                }
            }
        }

        // Read simulation state from SettingsViewModel
        val simulationEnabled = settingsVm?.simulationEnabled?.collectAsState()?.value ?: false
        val selectedRigPreset = settingsVm?.selectedRigPreset?.collectAsState()?.value ?: RigPreset.SMALL_DJ

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = currentState) {
                    is AppState.Onboarding -> {
                        if (onboardingVm != null) {
                            // Start the onboarding flow
                            DisposableEffect(onboardingVm) {
                                onboardingVm.start()
                                onDispose { onboardingVm.onCleared() }
                            }

                            OnboardingFlow(
                                viewModel = onboardingVm,
                                onComplete = {
                                    // Sync simulation state from onboarding to settings/stage
                                    val isSimMode = onboardingVm.isSimulationMode.value
                                    if (isSimMode) {
                                        val preset = onboardingVm.selectedRigPreset.value
                                        settingsVm?.toggleSimulation(true)
                                        settingsVm?.setRigPreset(preset)
                                        val rig = SimulatedFixtureRig(preset)
                                        stageVm?.enableSimulation(
                                            presetName = preset.presetDisplayName(),
                                            fixtureCount = rig.fixtureCount,
                                        )
                                    }
                                    appStateManager.completeOnboarding()
                                },
                            )
                        } else {
                            // Fallback if OnboardingViewModel is not in DI
                            ScreenPlaceholder(
                                "Onboarding",
                                "OnboardingViewModel not registered in DI.",
                            )
                        }
                    }
                    is AppState.StagePreview -> {
                        if (stageVm != null) {
                            StagePreviewScreen(
                                viewModel = stageVm,
                                onSettingsClick = { appStateManager.navigateTo(AppState.Settings) },
                            )
                        } else {
                            ScreenPlaceholder("Stage Preview", "Engine services not yet registered in DI.")
                        }
                    }
                    is AppState.Settings -> {
                        if (settingsVmV2 != null) {
                            SettingsScreen(
                                viewModel = settingsVmV2,
                                onBack = { appStateManager.navigateBack() },
                                onProvisioning = {
                                    appStateManager.navigateTo(AppState.BleProvisioning)
                                },
                            )
                        } else {
                            ScreenPlaceholder("Settings", "Services not registered.")
                        }
                    }
                    is AppState.BleProvisioning -> {
                        val provisioningVm = resolveOrNull<ProvisioningViewModel>()
                        if (provisioningVm != null) {
                            DisposableEffect(provisioningVm) {
                                onDispose { provisioningVm.onCleared() }
                            }
                            ProvisioningScreen(
                                viewModel = provisioningVm,
                                onClose = { appStateManager.navigateBack() },
                            )
                        } else {
                            ScreenPlaceholder("BLE Provisioning", "BLE services not registered.")
                        }
                    }
                    is AppState.RigSelection -> {
                        RigPresetSelector(
                            selectedPreset = selectedRigPreset,
                            onSelectPreset = { preset ->
                                settingsVm?.setRigPreset(preset)
                            },
                            onConfirm = {
                                // Enable simulation with the selected preset
                                settingsVm?.toggleSimulation(true)

                                val rig = SimulatedFixtureRig(selectedRigPreset)
                                stageVm?.enableSimulation(
                                    presetName = selectedRigPreset.presetDisplayName(),
                                    fixtureCount = rig.fixtureCount,
                                )

                                if (state.returnToOnboarding) {
                                    appStateManager.navigateTo(
                                        AppState.Onboarding
                                    )
                                } else {
                                    appStateManager.navigateBack()
                                }
                            },
                        )
                    }
                }

                // Pixel mascot overlay -- always visible on top of all screens
                if (mascotVm != null) {
                    DisposableEffect(mascotVm) {
                        onDispose { mascotVm.onCleared() }
                    }
                    MascotOverlay(
                        viewModel = mascotVm,
                        onMascotTap = { mascotVm.toggleChat() },
                    )
                }

                // Chat panel overlay -- slides up when mascot is tapped
                val agentVm = resolveOrNull<AgentViewModel>()
                if (mascotVm != null && agentVm != null) {
                    val isChatOpen by mascotVm.isChatOpen.collectAsState()
                    ChatPanel(
                        isOpen = isChatOpen,
                        agentViewModel = agentVm,
                        onDismiss = { mascotVm.toggleChat() },
                    )
                }
            }
        }
    }
}

/**
 * Safely resolve a dependency from Koin, returning null if not available.
 * The result is remembered so the same ViewModel instance is reused
 * across recompositions within the same composition.
 */
@Composable
private inline fun <reified T : Any> resolveOrNull(): T? {
    val koin = getKoin()
    return remember { runCatching { koin.get<T>() }.getOrNull() }
}

@Composable
private fun ScreenPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

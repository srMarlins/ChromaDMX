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
import androidx.compose.ui.Modifier
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.ui.mascot.MascotOverlay
import com.chromadmx.ui.util.presetDisplayName
import com.chromadmx.ui.navigation.AppState
import com.chromadmx.ui.navigation.AppStateManager
import com.chromadmx.ui.screen.onboarding.OnboardingScreen
import com.chromadmx.ui.screen.settings.SettingsScreen
import com.chromadmx.ui.screen.simulation.RigPresetSelector
import com.chromadmx.ui.screen.stage.StagePreviewScreen
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.viewmodel.MascotViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModel
import com.chromadmx.ui.viewmodel.StageViewModel
import org.koin.compose.getKoin

/**
 * Root composable for the ChromaDMX application.
 *
 * Uses [AppStateManager] for navigation: Onboarding -> StagePreview <-> Settings.
 * No tab bar. Single main screen with contextual overlays.
 *
 * Simulation mode is coordinated between [SettingsViewModel] (toggle/preset) and
 * [StageViewModel] (badge visibility, fixture count). The rig selector is accessible
 * from both the onboarding flow and the settings screen.
 */
@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        // TODO: Read isFirstLaunch from persistent storage
        val appStateManager = remember { AppStateManager(isFirstLaunch = false) }
        val currentState by appStateManager.currentState.collectAsState()

        val settingsVm = resolveOrNull<SettingsViewModel>()
        val stageVm = resolveOrNull<StageViewModel>()

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
                        val simFixtureCount = remember(simulationEnabled, selectedRigPreset) {
                            if (simulationEnabled) {
                                SimulatedFixtureRig(selectedRigPreset).fixtureCount
                            } else {
                                0
                            }
                        }

                        OnboardingScreen(
                            step = state.step,
                            onAdvance = { appStateManager.advanceOnboarding() },
                            onVirtualStage = {
                                // Navigate to rig selector, returning to onboarding afterward
                                appStateManager.navigateTo(AppState.RigSelection(returnToOnboarding = true))
                            },
                            isSimulationMode = simulationEnabled,
                            simulationFixtureCount = simFixtureCount,
                        )
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
                        if (settingsVm != null) {
                            SettingsScreen(
                                viewModel = settingsVm,
                                onClose = { appStateManager.navigateBack() },
                            )
                        } else {
                            ScreenPlaceholder("Settings", "Services not registered.")
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
                                    // Return to onboarding at FIXTURE_SCAN step
                                    appStateManager.navigateTo(
                                        AppState.Onboarding(
                                            com.chromadmx.ui.navigation.OnboardingStep.FIXTURE_SCAN
                                        )
                                    )
                                } else {
                                    appStateManager.navigateBack()
                                }
                            },
                        )
                    }
                }

                // Pixel mascot overlay -- always visible on top of all screens
                val mascotVm = resolveOrNull<MascotViewModel>()
                if (mascotVm != null) {
                    DisposableEffect(mascotVm) {
                        onDispose { mascotVm.onCleared() }
                    }
                    MascotOverlay(
                        viewModel = mascotVm,
                        onMascotTap = { mascotVm.toggleChat() },
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

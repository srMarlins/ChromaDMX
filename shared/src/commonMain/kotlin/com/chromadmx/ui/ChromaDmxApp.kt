package com.chromadmx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromadmx.ui.mascot.MascotOverlay
import com.chromadmx.ui.navigation.AppScreen
import com.chromadmx.ui.navigation.AppStateManager
import com.chromadmx.ui.screen.chat.ChatPanel
import com.chromadmx.ui.screen.settings.ProvisioningScreen
import com.chromadmx.ui.screen.settings.SettingsScreen
import com.chromadmx.ui.screen.setup.SetupScreen
import com.chromadmx.ui.screen.stage.StageScreen
import com.chromadmx.ui.state.MascotEvent
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.viewmodel.MascotViewModelV2
import com.chromadmx.ui.viewmodel.ProvisioningViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModelV2
import com.chromadmx.ui.viewmodel.SetupViewModel
import com.chromadmx.ui.viewmodel.StageViewModelV2
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.getKoin

/**
 * Root composable for the ChromaDMX application.
 *
 * Uses [AppStateManager] for 4-screen navigation:
 * Setup -> Stage <-> Settings <-> Provisioning.
 *
 * All ViewModels follow UDF — single [state] flow, single [onEvent] entry.
 * The mascot overlay and chat panel are global layers above all screens.
 */
@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        val appStateManager = resolveOrNull<AppStateManager>()
        val currentScreen by appStateManager?.currentScreen?.collectAsState()
            ?: remember { MutableStateFlow(AppScreen.Setup) }.collectAsState()

        val setupVm = resolveOrNull<SetupViewModel>()
        val stageVm = resolveOrNull<StageViewModelV2>()
        val settingsVm = resolveOrNull<SettingsViewModelV2>()
        val mascotVm = resolveOrNull<MascotViewModelV2>()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.Setup -> {
                        if (setupVm != null) {
                            SetupScreen(
                                viewModel = setupVm,
                                onComplete = {
                                    appStateManager?.completeSetup()
                                },
                            )
                        } else {
                            ScreenPlaceholder(
                                "Setup",
                                "SetupViewModel not registered in DI.",
                            )
                        }
                    }
                    AppScreen.Stage -> {
                        if (stageVm != null) {
                            StageScreen(
                                viewModel = stageVm,
                                onSettings = {
                                    appStateManager?.navigateTo(AppScreen.Settings)
                                },
                            )
                        } else {
                            ScreenPlaceholder(
                                "Stage",
                                "Engine services not registered in DI.",
                            )
                        }
                    }
                    AppScreen.Settings -> {
                        if (settingsVm != null) {
                            SettingsScreen(
                                viewModel = settingsVm,
                                onBack = { appStateManager?.navigateBack() },
                                onProvisioning = {
                                    appStateManager?.navigateTo(AppScreen.Provisioning)
                                },
                            )
                        } else {
                            ScreenPlaceholder("Settings", "Services not registered.")
                        }
                    }
                    AppScreen.Provisioning -> {
                        val provisioningVm = resolveOrNull<ProvisioningViewModel>()
                        if (provisioningVm != null) {
                            DisposableEffect(provisioningVm) {
                                onDispose { provisioningVm.onCleared() }
                            }
                            ProvisioningScreen(
                                viewModel = provisioningVm,
                                onClose = { appStateManager?.navigateBack() },
                            )
                        } else {
                            ScreenPlaceholder(
                                "BLE Provisioning",
                                "BLE services not registered.",
                            )
                        }
                    }
                }

                // Pixel mascot overlay — always visible on top of all screens
                if (mascotVm != null) {
                    MascotOverlay(
                        viewModel = mascotVm,
                        onMascotTap = { mascotVm.onEvent(MascotEvent.ToggleChat) },
                    )
                }

                // Chat panel overlay — slides up when mascot is tapped
                if (mascotVm != null) {
                    ChatPanel(viewModel = mascotVm)
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

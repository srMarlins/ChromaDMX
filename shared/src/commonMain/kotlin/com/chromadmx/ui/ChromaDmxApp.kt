package com.chromadmx.ui

import androidx.compose.foundation.layout.Arrangement
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
import com.chromadmx.ui.navigation.AppState
import com.chromadmx.ui.navigation.AppStateManager
import com.chromadmx.ui.screen.onboarding.OnboardingScreen
import com.chromadmx.ui.screen.settings.SettingsScreen
import com.chromadmx.ui.screen.stage.StagePreviewScreen
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.viewmodel.SettingsViewModel
import com.chromadmx.ui.viewmodel.StageViewModel
import org.koin.compose.getKoin

/**
 * Root composable for the ChromaDMX application.
 *
 * Uses [AppStateManager] for navigation: Onboarding -> StagePreview <-> Settings.
 * No tab bar. Single main screen with contextual overlays.
 */
@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        // TODO: Read isFirstLaunch from persistent storage
        val appStateManager = remember { AppStateManager(isFirstLaunch = false) }
        val currentState by appStateManager.currentState.collectAsState()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val state = currentState) {
                is AppState.Onboarding -> {
                    OnboardingScreen(
                        step = state.step,
                        onAdvance = { appStateManager.advanceOnboarding() },
                    )
                }
                is AppState.StagePreview -> {
                    val stageVm = resolveOrNull<StageViewModel>()
                    if (stageVm != null) {
                        DisposableEffect(stageVm) {
                            onDispose { stageVm.onCleared() }
                        }
                        StagePreviewScreen(
                            viewModel = stageVm,
                            onSettingsClick = { appStateManager.navigateTo(AppState.Settings) },
                        )
                    } else {
                        ScreenPlaceholder("Stage Preview", "Engine services not yet registered in DI.")
                    }
                }
                is AppState.Settings -> {
                    val settingsVm = resolveOrNull<SettingsViewModel>()
                    if (settingsVm != null) {
                        DisposableEffect(settingsVm) {
                            onDispose { settingsVm.onCleared() }
                        }
                        SettingsScreen(
                            viewModel = settingsVm,
                            onClose = { appStateManager.navigateBack() },
                        )
                    } else {
                        ScreenPlaceholder("Settings", "Services not registered.")
                    }
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

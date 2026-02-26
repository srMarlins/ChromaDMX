package com.chromadmx.ui.screen.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromadmx.ui.onboarding.OnboardingStep
import com.chromadmx.ui.viewmodel.OnboardingViewModel

/**
 * Root onboarding composable that delegates to step-specific screens.
 *
 * Reads the current step from [OnboardingViewModel] and renders the
 * appropriate screen. Each screen communicates back through the ViewModel
 * (advance, retry, select genre, etc.).
 *
 * The [onComplete] callback is invoked when the flow reaches
 * [OnboardingStep.Complete], signaling the parent to transition to
 * the main StagePreview screen.
 *
 * @param viewModel The OnboardingViewModel managing step state.
 * @param onComplete Called when onboarding finishes (step == Complete).
 */
@Composable
fun OnboardingFlow(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStep by viewModel.currentStep.collectAsState()

    // When we reach Complete, notify parent
    if (currentStep is OnboardingStep.Complete) {
        onComplete()
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (currentStep) {
            is OnboardingStep.Splash -> {
                SplashScreen()
            }

            is OnboardingStep.NetworkDiscovery -> {
                val isScanning by viewModel.isScanning.collectAsState()
                val discoveredNodes by viewModel.discoveredNodes.collectAsState()

                NetworkDiscoveryScreen(
                    isScanning = isScanning,
                    discoveredNodes = discoveredNodes,
                    onRetry = { viewModel.retryNetworkScan() },
                    onSimulation = { viewModel.enterSimulationMode() },
                )
            }

            is OnboardingStep.FixtureScan -> {
                val isSimMode by viewModel.isSimulationMode.collectAsState()
                val selectedPreset by viewModel.selectedRigPreset.collectAsState()
                val fixturesLoaded by viewModel.fixturesLoaded.collectAsState()
                val totalFixtures by viewModel.simulationFixtureCount.collectAsState()

                FixtureScanScreen(
                    isSimulationMode = isSimMode,
                    selectedPreset = selectedPreset,
                    fixturesLoaded = fixturesLoaded,
                    totalFixtures = totalFixtures,
                    onSelectPreset = { viewModel.selectRigPreset(it) },
                    onContinue = { viewModel.advance() },
                )
            }

            is OnboardingStep.VibeCheck -> {
                val selectedGenre by viewModel.selectedGenre.collectAsState()

                VibeCheckScreen(
                    genres = viewModel.genres,
                    selectedGenre = selectedGenre,
                    onSelectGenre = { viewModel.selectGenre(it) },
                    onConfirm = { viewModel.confirmGenre() },
                    onSkip = { viewModel.skipToComplete() },
                )
            }

            is OnboardingStep.StagePreview -> {
                val isSimMode by viewModel.isSimulationMode.collectAsState()
                val selectedGenre by viewModel.selectedGenre.collectAsState()

                StagePreviewOnboardingScreen(
                    isSimulationMode = isSimMode,
                    selectedGenreName = selectedGenre?.displayName,
                    onSkip = { viewModel.skipStagePreview() },
                )
            }

            is OnboardingStep.Complete -> {
                // Handled above
            }
        }
    }
}

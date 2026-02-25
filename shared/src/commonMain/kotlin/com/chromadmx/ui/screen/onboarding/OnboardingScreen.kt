package com.chromadmx.ui.screen.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.navigation.OnboardingStep

/**
 * Onboarding flow screen. Placeholder -- will be fully implemented in #19.
 */
@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (step) {
                    OnboardingStep.SPLASH -> "ChromaDMX"
                    OnboardingStep.NETWORK_DISCOVERY -> "Scanning for lights..."
                    OnboardingStep.FIXTURE_SCAN -> "Mapping fixtures..."
                    OnboardingStep.VIBE_CHECK -> "What's tonight's vibe?"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onAdvance) {
                Text("Continue")
            }
        }
    }
}

package com.chromadmx.ui.screen.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.navigation.OnboardingStep
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.NeonYellow
import com.chromadmx.ui.theme.PixelFontFamily
import kotlinx.coroutines.delay

/**
 * Onboarding flow screen with simulation entry point.
 *
 * At the NETWORK_DISCOVERY step, after a simulated scan period, shows
 * "No lights found" and offers the option to launch a virtual stage.
 *
 * At the FIXTURE_SCAN step in simulation mode, shows a simulated camera
 * canvas with fixtures appearing as they are "discovered".
 *
 * @param step The current onboarding step.
 * @param onAdvance Called to advance to the next onboarding step.
 * @param onVirtualStage Called when the user chooses "Virtual Stage" (opens rig selector).
 * @param isSimulationMode Whether simulation mode is active (affects FIXTURE_SCAN display).
 * @param simulationFixtureCount Number of simulated fixtures (for scan animation).
 * @param modifier Layout modifier.
 */
@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    onAdvance: () -> Unit,
    onVirtualStage: () -> Unit = {},
    isSimulationMode: Boolean = false,
    simulationFixtureCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (step) {
            OnboardingStep.SPLASH -> SplashStep(onAdvance = onAdvance)
            OnboardingStep.NETWORK_DISCOVERY -> NetworkDiscoveryStep(
                onAdvance = onAdvance,
                onVirtualStage = onVirtualStage,
            )
            OnboardingStep.FIXTURE_SCAN -> FixtureScanStep(
                onAdvance = onAdvance,
                isSimulationMode = isSimulationMode,
                fixtureCount = simulationFixtureCount,
            )
            OnboardingStep.VIBE_CHECK -> VibeCheckStep(onAdvance = onAdvance)
        }
    }
}

// -- Step implementations --

@Composable
private fun SplashStep(onAdvance: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "ChromaDMX",
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = PixelFontFamily),
            color = NeonCyan,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Light your stage",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        PixelButton(onClick = onAdvance) {
            Text("GET STARTED")
        }
    }
}

/**
 * NETWORK_DISCOVERY step: scans for hardware, then shows "no lights found"
 * with a mascot bubble and options to retry or enter virtual stage.
 */
@Composable
private fun NetworkDiscoveryStep(
    onAdvance: () -> Unit,
    onVirtualStage: () -> Unit,
) {
    // Simulate a scan that takes a few seconds, then shows "no lights found"
    var isScanning by remember { mutableStateOf(true) }
    var scanProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        // Animate the scan progress over ~3 seconds
        val steps = 30
        for (i in 1..steps) {
            delay(100L)
            scanProgress = i.toFloat() / steps.toFloat()
        }
        isScanning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Scanning animation / result
        AnimatedVisibility(
            visible = isScanning,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScanAnimation()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning for lights...",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
                    color = NeonCyan,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PixelProgressBar(
                    progress = scanProgress,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    progressColor = NeonCyan,
                )
            }
        }

        AnimatedVisibility(
            visible = !isScanning,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // "No lights found" state
                Text(
                    text = "No lights found",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
                    color = NeonYellow,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Mascot speech bubble (inline, since MascotOverlay is separate)
                PixelCard(
                    borderColor = NeonMagenta.copy(alpha = 0.5f),
                    glowColor = NeonMagenta.copy(alpha = 0.1f),
                ) {
                    Text(
                        text = "No lights found -- want a virtual stage?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PixelButton(
                        onClick = onAdvance,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text("TRY AGAIN")
                    }
                    PixelButton(
                        onClick = onVirtualStage,
                        modifier = Modifier.weight(1f),
                        backgroundColor = NeonMagenta,
                        contentColor = Color.Black,
                    ) {
                        Text("VIRTUAL STAGE")
                    }
                }
            }
        }
    }
}

/**
 * FIXTURE_SCAN step: In simulation mode, shows a simulated camera canvas
 * with fixtures "popping in" as they are discovered.
 * In normal mode, shows basic mapping text.
 */
@Composable
private fun FixtureScanStep(
    onAdvance: () -> Unit,
    isSimulationMode: Boolean,
    fixtureCount: Int,
) {
    if (isSimulationMode && fixtureCount > 0) {
        SimulatedScanView(
            totalFixtures = fixtureCount,
            onComplete = onAdvance,
        )
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Mapping fixtures...",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = PixelFontFamily),
                color = NeonCyan,
            )
            Spacer(modifier = Modifier.height(32.dp))
            PixelButton(onClick = onAdvance) {
                Text("CONTINUE")
            }
        }
    }
}

/**
 * Simulated camera output during fixture scan.
 *
 * Shows a dark canvas where fixture dots appear one-by-one with a progress
 * indicator, mimicking the simulated camera discovering fixtures.
 */
@Composable
private fun SimulatedScanView(
    totalFixtures: Int,
    onComplete: () -> Unit,
) {
    var discoveredCount by remember { mutableStateOf(0) }
    val scanComplete = discoveredCount >= totalFixtures

    LaunchedEffect(totalFixtures) {
        // Simulate fixtures appearing one-by-one
        for (i in 1..totalFixtures) {
            delay(150L) // staggered discovery
            discoveredCount = i
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "SIMULATED CAMERA",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = PixelFontFamily),
            color = NeonMagenta,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Simulated camera canvas
        SimulatedCameraCanvas(
            discoveredCount = discoveredCount,
            totalFixtures = totalFixtures,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(260.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress
        PixelProgressBar(
            progress = if (totalFixtures > 0) discoveredCount.toFloat() / totalFixtures else 0f,
            modifier = Modifier.fillMaxWidth(0.8f),
            progressColor = NeonCyan,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$discoveredCount / $totalFixtures fixtures found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = scanComplete) {
            PixelButton(
                onClick = onComplete,
                backgroundColor = NeonCyan,
                contentColor = Color.Black,
            ) {
                Text("CONTINUE")
            }
        }
    }
}

/**
 * Canvas that mimics a grayscale camera view with fixture blobs popping in.
 */
@Composable
private fun SimulatedCameraCanvas(
    discoveredCount: Int,
    totalFixtures: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.background(Color(0xFF0A0A1A)),
    ) {
        val padding = 20f
        val canvasW = size.width - 2 * padding
        val canvasH = size.height - 2 * padding
        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        // Draw scan lines (CRT effect)
        val scanLineSpacing = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += scanLineSpacing
        }

        // Distribute fixture dots evenly in a grid pattern
        if (totalFixtures <= 0) return@Canvas

        val cols = when {
            totalFixtures <= 8 -> totalFixtures
            totalFixtures <= 30 -> 10
            else -> 12
        }
        val rows = (totalFixtures + cols - 1) / cols

        for (i in 0 until discoveredCount.coerceAtMost(totalFixtures)) {
            val col = i % cols
            val row = i / cols

            val cx = padding + (col + 0.5f) * canvasW / cols
            val cy = padding + (row + 0.5f) * canvasH / rows.coerceAtLeast(1)

            // Outer glow (simulated camera blob)
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = 14f,
                center = Offset(cx, cy),
            )
            // Inner bright point
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 5f,
                center = Offset(cx, cy),
            )
        }
    }
}

@Composable
private fun VibeCheckStep(onAdvance: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What's tonight's vibe?",
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = PixelFontFamily),
            color = NeonCyan,
        )
        Spacer(modifier = Modifier.height(32.dp))
        PixelButton(onClick = onAdvance) {
            Text("LET'S GO")
        }
    }
}

/**
 * Simple animated dots to indicate a network scan in progress.
 */
@Composable
private fun ScanAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan-dots")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-dot-alpha",
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha = when (index) {
                0 -> dotAlpha
                1 -> dotAlpha * 0.7f
                else -> dotAlpha * 0.4f
            }
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(
                    color = NeonCyan.copy(alpha = alpha),
                    radius = size.minDimension / 2f,
                )
            }
            if (index < 2) Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

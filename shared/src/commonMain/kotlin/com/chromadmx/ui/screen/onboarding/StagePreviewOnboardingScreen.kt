package com.chromadmx.ui.screen.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Brief stage preview screen shown as the final onboarding step before
 * entering the main app.
 *
 * Shows a placeholder stage visualization with animated "fixture" dots
 * and a motivational message. Auto-advances after 2 seconds via the
 * ViewModel timer, but the user can tap "Enter Stage" to skip ahead.
 *
 * @param isSimulationMode Whether the user chose simulation mode.
 * @param selectedGenreName The genre the user selected (null if skipped).
 * @param onSkip Called when the user taps "Enter Stage" to skip the auto-advance.
 */
@Composable
fun StagePreviewOnboardingScreen(
    isSimulationMode: Boolean,
    selectedGenreName: String?,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "stage-preview-alpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "YOUR STAGE IS READY",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = PixelFontFamily),
            color = NeonGreen,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val subtitle = buildString {
            if (isSimulationMode) append("Simulation Mode")
            if (selectedGenreName != null) {
                if (isSimulationMode) append(" / ")
                append(selectedGenreName)
            }
        }
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Placeholder stage canvas with animated fixture dots
        StagePreviewCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Entering in a moment...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PixelButton(
            onClick = onSkip,
            backgroundColor = NeonCyan,
            contentColor = Color.Black,
        ) {
            Text("ENTER STAGE")
        }
    }
}

/**
 * Placeholder canvas showing a stylized stage with fixture dots.
 */
@Composable
private fun StagePreviewCanvas(
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "canvas-alpha",
    )

    Canvas(
        modifier = modifier.background(Color(0xFF0A0A1A)),
    ) {
        val padding = 20f
        val canvasW = size.width - 2 * padding
        val canvasH = size.height - 2 * padding
        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        // CRT scanlines
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

        // Stage outline (bottom rectangle)
        drawLine(
            color = Color.White.copy(alpha = 0.15f * alpha),
            start = Offset(padding, size.height - padding),
            end = Offset(size.width - padding, size.height - padding),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.1f * alpha),
            start = Offset(padding, size.height - padding),
            end = Offset(padding + canvasW * 0.15f, padding + canvasH * 0.3f),
            strokeWidth = 1f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.1f * alpha),
            start = Offset(size.width - padding, size.height - padding),
            end = Offset(size.width - padding - canvasW * 0.15f, padding + canvasH * 0.3f),
            strokeWidth = 1f,
        )

        // Fixture dots on the "truss" (top area)
        val fixtureCount = 8
        val trussY = padding + canvasH * 0.2f
        for (i in 0 until fixtureCount) {
            val cx = padding + (i + 0.5f) * canvasW / fixtureCount
            val fixtureColor = when (i % 3) {
                0 -> NeonCyan
                1 -> NeonMagenta
                else -> NeonGreen
            }

            // Glow
            drawCircle(
                color = fixtureColor.copy(alpha = 0.2f * alpha),
                radius = 16f,
                center = Offset(cx, trussY),
            )
            // Bright center
            drawCircle(
                color = fixtureColor.copy(alpha = 0.8f * alpha),
                radius = 5f,
                center = Offset(cx, trussY),
            )
        }

        // Light beams (cones from fixtures to stage floor)
        for (i in 0 until fixtureCount step 2) {
            val cx = padding + (i + 0.5f) * canvasW / fixtureCount
            val beamColor = when (i % 3) {
                0 -> NeonCyan
                1 -> NeonMagenta
                else -> NeonGreen
            }
            drawLine(
                color = beamColor.copy(alpha = 0.06f * alpha),
                start = Offset(cx, trussY + 10f),
                end = Offset(cx - 15f, size.height - padding),
                strokeWidth = 20f,
            )
            drawLine(
                color = beamColor.copy(alpha = 0.06f * alpha),
                start = Offset(cx, trussY + 10f),
                end = Offset(cx + 15f, size.height - padding),
                strokeWidth = 20f,
            )
        }
    }
}

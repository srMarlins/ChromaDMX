package com.chromadmx.ui.components.beat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.LocalPixelTheme
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Source of the BPM value, used to determine the display color.
 */
enum class BpmSource {
    /** Synced via Ableton Link — shows NeonGreen. */
    LINK,
    /** Derived from tap tempo — shows NeonYellow. */
    TAP,
    /** No active source / idle — shows gray. */
    IDLE
}

/**
 * Displays the current BPM as large pixel-font text.
 *
 * Pulses opacity from 1.0 to 0.6 on each beat downbeat, driven by [beatPhase].
 * Tap anywhere on the component to register a tap-tempo hit via [onTap].
 *
 * @param bpm        Current beats-per-minute value.
 * @param beatPhase  Phase within the current beat (0.0 = downbeat, 1.0 = next downbeat).
 * @param source     Where the BPM value originates (determines display color).
 * @param onTap      Called when the user taps this display (for tap-tempo).
 * @param modifier   Compose modifier.
 * @param pixelSize  Pixel unit for the border.
 */
@Composable
fun BpmDisplay(
    bpm: Float,
    beatPhase: Float,
    source: BpmSource = BpmSource.TAP,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier,
    pixelSize: Dp = LocalPixelTheme.current.pixelSize,
) {
    val colors = PixelDesign.colors
    val bpmColor: Color = when (source) {
        BpmSource.LINK -> colors.success
        BpmSource.TAP -> colors.tertiary
        BpmSource.IDLE -> colors.onSurfaceDim
    }

    // Pulse: opacity is 1.0 at the downbeat (phase=0), fades to 0.6 by mid-beat,
    // then returns toward 1.0. Using a simple ease: alpha = 1 - 0.4 * (1 - phase)^2
    // when phase < 0.3 (the "flash" zone), else 0.6 + 0.4 * ((phase - 0.3) / 0.7)
    // Simpler approach: short flash near 0, then dim.
    val pulseAlpha = if (beatPhase < 0.15f) {
        // Flash zone: bright at 0, ramp down to 0.6 by 0.15
        1.0f - (beatPhase / 0.15f) * 0.4f
    } else {
        // Sustain dim, slowly rise back
        0.6f + (beatPhase - 0.15f) / 0.85f * 0.4f
    }

    val interactionSource = remember { MutableInteractionSource() }

    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
            )
            .clip(shape)
            .border(1.dp, bpmColor.copy(alpha = 0.4f), shape)
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${bpm.toInt()} BPM",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
                color = bpmColor,
            ),
            modifier = Modifier.alpha(pulseAlpha),
        )
    }
}

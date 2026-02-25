package com.chromadmx.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * A pixel-styled "SIMULATION" badge with a subtle pulsing animation.
 *
 * Displayed in the top-left corner of the stage preview when simulation
 * mode is active. Tapping it shows an info tooltip.
 *
 * @param onTap Called when the badge is tapped (shows tooltip).
 * @param modifier Layout modifier.
 * @param pixelSize Size of the pixel border segments.
 */
@Composable
fun SimulationBadge(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    pixelSize: Dp = 2.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sim-badge-pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sim-badge-alpha",
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .clickable(onClick = onTap)
            .pixelBorder(color = NeonMagenta.copy(alpha = 0.6f), pixelSize = pixelSize)
            .background(NeonMagenta.copy(alpha = 0.25f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "SIMULATION",
            style = MaterialTheme.typography.labelSmall.copy(
                color = NeonMagenta,
                fontFamily = PixelFontFamily,
            ),
        )
    }
}

/**
 * A smaller "VIRTUAL" pixel badge for marking simulated nodes in lists.
 *
 * @param modifier Layout modifier.
 * @param pixelSize Size of the pixel border segments.
 */
@Composable
fun VirtualNodeBadge(
    modifier: Modifier = Modifier,
    pixelSize: Dp = 2.dp,
) {
    Box(
        modifier = modifier
            .pixelBorder(
                color = Color.White.copy(alpha = 0.3f),
                pixelSize = pixelSize,
            )
            .background(NeonMagenta.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "VIRTUAL",
            style = MaterialTheme.typography.labelSmall.copy(
                color = NeonMagenta.copy(alpha = 0.8f),
                fontFamily = PixelFontFamily,
            ),
        )
    }
}

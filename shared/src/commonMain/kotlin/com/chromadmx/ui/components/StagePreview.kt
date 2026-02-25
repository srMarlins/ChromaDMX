package com.chromadmx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Color as DmxColor
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Wraps VenueCanvas with a diagnostic top bar.
 */
@Composable
fun StagePreview(
    fixtures: List<Fixture3D>,
    fixtureColors: List<DmxColor>,
    beatState: BeatState,
    nodes: List<DmxNode>,
    currentTimeMs: Long,
    onSettingsClick: () -> Unit,
    onNodeHealthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        VenueCanvas(
            fixtures = fixtures,
            fixtureColors = fixtureColors,
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // BPM & Beat Indicators
            Row(verticalAlignment = Alignment.CenterVertically) {
                BpmDisplay(beatState)
                Spacer(Modifier.width(12.dp))
                BeatPhaseIndicators(beatState)
            }

            // Network Health & Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                NodeHealthCompact(
                    nodes = nodes,
                    currentTimeMs = currentTimeMs,
                    onExpand = onNodeHealthClick
                )

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BpmDisplay(beatState: BeatState) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Text(
        text = "${beatState.bpm.toInt()} BPM",
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = PixelFontFamily,
            fontSize = 12.sp,
            color = NeonCyan.copy(alpha = if (beatState.bpm > 0) alpha else 0.5f)
        )
    )
}

@Composable
private fun BeatPhaseIndicators(beatState: BeatState) {
    val currentBeat = (beatState.barPhase * 4).toInt().coerceIn(0, 3)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 4) {
            val isActive = i == currentBeat
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (isActive) NeonCyan else Color.Gray.copy(alpha = 0.3f))
                    .pixelBorder(color = if (isActive) Color.White else Color.Transparent, pixelSize = 1.dp)
            )
        }
    }
}

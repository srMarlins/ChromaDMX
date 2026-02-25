package com.chromadmx.ui.screen.perform

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.BeatState
import com.chromadmx.ui.theme.BeatActive
import com.chromadmx.ui.theme.BeatInactive

/**
 * Displays the current beat state with 4 beat indicator circles
 * and BPM readout. The active beat is highlighted based on barPhase.
 */
@Composable
fun BeatVisualization(
    beatState: BeatState,
    modifier: Modifier = Modifier,
) {
    val currentBeat = (beatState.barPhase * 4).toInt().coerceIn(0, 3)
    // Pulse intensity: how close to the downbeat within the current beat
    val pulseIntensity = 1f - beatState.beatPhase

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // BPM display
        Text(
            text = "${beatState.bpm.toInt()} BPM",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(12.dp))

        // Beat indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until 4) {
                val isActive = i == currentBeat
                val color = if (isActive) {
                    BeatActive.copy(alpha = 0.4f + 0.6f * pulseIntensity)
                } else {
                    BeatInactive
                }
                val targetSize = if (isActive) 24.dp else 20.dp
                val animatedSize by animateDpAsState(targetValue = targetSize, animationSpec = tween(100))

                Canvas(modifier = Modifier.size(animatedSize)) {
                    drawCircle(
                        color = color,
                        radius = size.minDimension / 2f,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                }
            }
        }
    }
}

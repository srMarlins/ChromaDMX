package com.chromadmx.ui.screen.perform

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.BeatSyncSource
import com.chromadmx.ui.theme.NodeOnline
import com.chromadmx.ui.theme.NodeUnknown
import com.chromadmx.ui.theme.NodeWarning

/**
 * Large pixel-font BPM display that pulses on each beat.
 * Color changes based on the sync source.
 */
@Composable
fun BpmDisplay(
    bpm: Float,
    syncSource: BeatSyncSource,
    beatPhase: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation: strongest at the start of the beat (beatPhase close to 0)
    // We use a simple curve that peaks at 0 and decays.
    val pulseScale = 1.0f + (1f - beatPhase).let { if (it > 0.8f) (it - 0.8f) * 0.5f else 0f }

    val color = when (syncSource) {
        BeatSyncSource.LINK -> NodeOnline
        BeatSyncSource.TAP -> NodeWarning
        BeatSyncSource.NONE -> NodeUnknown
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = bpm.toInt().toString(),
            color = color,
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.scale(pulseScale)
        )
    }
}

/**
 * Small pixel bar showing the continuous beat phase (0.0 -> 1.0).
 */
@Composable
fun BeatPhaseIndicator(
    beatPhase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.height(4.dp).fillMaxWidth()) {
        val barWidth = size.width * beatPhase
        drawRect(
            color = Color.White.copy(alpha = 0.2f),
            size = size
        )
        drawRect(
            color = Color.White.copy(alpha = 0.8f),
            size = size.copy(width = barWidth)
        )
    }
}

/**
 * Bar phase indicator with 4 segments, one lighting up per beat.
 */
@Composable
fun BarPhaseIndicator(
    barPhase: Float,
    modifier: Modifier = Modifier
) {
    val currentBeat = (barPhase * 4).toInt().coerceIn(0, 3)

    Row(
        modifier = modifier.height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until 4) {
            val isActive = i == currentBeat
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isActive) Modifier.background(Color.White)
                        else Modifier.background(Color.White.copy(alpha = 0.2f))
                    )
            )
        }
    }
}

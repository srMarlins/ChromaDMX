package com.chromadmx.ui.components.beat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.BeatActive
import com.chromadmx.ui.theme.BeatInactive
import com.chromadmx.ui.theme.LocalPixelTheme

/**
 * A horizontal pixel bar showing beat phase progress (0.0 to 1.0).
 *
 * Divided into [segments] equal segments. Segments up to the current phase
 * are filled with [activeColor]; the rest use [inactiveColor].
 * The smooth phase value from the BeatClock drives the fill level each frame.
 *
 * @param beatPhase    Current beat phase (0.0 = downbeat, 1.0 = next downbeat).
 * @param modifier     Compose modifier.
 * @param activeColor  Color for filled segments.
 * @param inactiveColor Color for unfilled segments.
 * @param segments     Number of segments in the bar.
 * @param pixelSize    Pixel unit for the border.
 */
@Composable
fun BeatPhaseIndicator(
    beatPhase: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = BeatActive,
    inactiveColor: Color = BeatInactive,
    segments: Int = 16,
    pixelSize: Dp = LocalPixelTheme.current.pixelSize,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(pixelSize * 3)
            .pixelBorder(color = activeColor.copy(alpha = 0.3f), pixelSize = pixelSize)
            .background(inactiveColor)
            .padding(pixelSize),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            val activeSegments = (beatPhase * segments).toInt().coerceIn(0, segments)
            for (i in 0 until segments) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (i < activeSegments) activeColor else Color.Transparent
                        ),
                )
            }
        }
    }
}

package com.chromadmx.ui.components.beat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.LocalPixelTheme
import com.chromadmx.ui.theme.PixelDesign

/**
 * Four pixel segments representing beats within a bar.
 *
 * The current beat segment lights up based on [barPhase]:
 * - 0.00..0.25 = beat 1
 * - 0.25..0.50 = beat 2
 * - 0.50..0.75 = beat 3
 * - 0.75..1.00 = beat 4
 *
 * @param barPhase      Phase within the current bar (0.0 to 1.0).
 * @param modifier      Compose modifier.
 * @param activeColor   Color for the currently active beat segment.
 * @param inactiveColor Color for inactive beat segments.
 * @param beatsPerBar   Number of beats per bar (default 4).
 * @param pixelSize     Pixel unit for the border.
 */
@Composable
fun BarPhaseIndicator(
    barPhase: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = PixelDesign.colors.primary,
    inactiveColor: Color = PixelDesign.colors.surfaceVariant,
    beatsPerBar: Int = 4,
    pixelSize: Dp = LocalPixelTheme.current.pixelSize,
) {
    val currentBeat = (barPhase * beatsPerBar).toInt().coerceIn(0, beatsPerBar - 1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(pixelSize * 4),
        horizontalArrangement = Arrangement.spacedBy(pixelSize),
    ) {
        for (i in 0 until beatsPerBar) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pixelBorder(
                        color = if (i == currentBeat) activeColor.copy(alpha = 0.6f) else inactiveColor.copy(alpha = 0.3f),
                        pixelSize = pixelSize,
                    )
                    .background(if (i == currentBeat) activeColor else inactiveColor)
                    .padding(pixelSize),
            )
        }
    }
}

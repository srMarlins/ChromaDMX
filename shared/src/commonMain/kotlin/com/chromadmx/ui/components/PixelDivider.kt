package com.chromadmx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.LocalPixelTheme

/**
 * A stepped pixel-art divider line.
 */
@Composable
fun PixelDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    pixelSize: Dp = LocalPixelTheme.current.pixelSize,
    stepped: Boolean = true
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(pixelSize * 2)
    ) {
        val ps = pixelSize.toPx()
        if (!stepped) {
            drawRect(color, Offset(0f, ps / 2), Size(size.width, ps))
        } else {
            var x = 0f
            var step = 0
            while (x < size.width) {
                val yOffset = if (step % 2 == 0) 0f else ps
                drawRect(
                    color = color,
                    topLeft = Offset(x, yOffset),
                    size = Size(ps * 2, ps)
                )
                x += ps * 2
                step++
            }
        }
    }
}

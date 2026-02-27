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
import com.chromadmx.ui.theme.PixelDesign

/**
 * A stepped pixel-art divider line.
 *
 * @param stepped When true, draws an alternating stepped pattern. When false, draws a flat line.
 * @param enchanted When true, renders the [PixelEnchantedDivider] shimmer variant instead.
 */
@Composable
fun PixelDivider(
    modifier: Modifier = Modifier,
    color: Color = PixelDesign.colors.outlineVariant,
    pixelSize: Dp = PixelDesign.spacing.pixelSize,
    stepped: Boolean = true,
    enchanted: Boolean = false,
) {
    if (enchanted) {
        PixelEnchantedDivider(modifier = modifier)
        return
    }

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

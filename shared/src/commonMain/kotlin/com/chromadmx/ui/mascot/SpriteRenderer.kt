package com.chromadmx.ui.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders a single [SpriteFrame] as pixel art on a Compose Canvas.
 *
 * Each pixel in the 16x16 grid is rendered as a filled square.
 * Transparent pixels (ARGB 0x00000000) are skipped.
 *
 * @param frame The sprite frame to render.
 * @param size Total size of the rendered sprite.
 */
@Composable
fun SpriteRenderer(
    frame: SpriteFrame,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val pixelW = this.size.width / frame.width
        val pixelH = this.size.height / frame.height

        for (row in 0 until frame.height) {
            for (col in 0 until frame.width) {
                val argb = frame.pixels[row][col]
                if (argb == 0) continue // transparent

                val alpha = ((argb shr 24) and 0xFF) / 255f
                val red = ((argb shr 16) and 0xFF) / 255f
                val green = ((argb shr 8) and 0xFF) / 255f
                val blue = (argb and 0xFF) / 255f

                drawRect(
                    color = Color(red, green, blue, alpha),
                    topLeft = Offset(col * pixelW, row * pixelH),
                    size = Size(pixelW, pixelH),
                )
            }
        }
    }
}

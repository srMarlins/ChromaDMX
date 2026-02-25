package com.chromadmx.ui.util

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.sin

/**
 * Pixel-art animation utilities for ChromaDMX.
 * These utilities provide chunky, deliberate movements and transitions.
 *
 * Note: These are non-composable Modifier extensions, so they cannot read from
 * LocalPixelTheme.current. Callers in @Composable contexts should pass
 * LocalPixelTheme.current.pixelSize explicitly if they want theme-driven sizing.
 */
object PixelAnimations {

    /**
     * Pixel dissolve effect using a grid-based mask.
     * @param progress 0.0 (fully dissolved) to 1.0 (fully visible)
     * @param pixelSize size of each "dissolve pixel"
     */
    fun Modifier.pixelDissolve(
        progress: Float,
        pixelSize: Dp = 8.dp
    ): Modifier = this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            if (progress >= 1f) return@drawWithContent

            val sizePx = pixelSize.toPx()
            val cols = (size.width / sizePx).toInt() + 1
            val rows = (size.height / sizePx).toInt() + 1

            for (x in 0 until cols) {
                for (y in 0 until rows) {
                    // Deterministic pseudo-random threshold for this pixel
                    val threshold = ((x * 137 + y * 149) % 1000) / 1000f
                    if (threshold > progress) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * sizePx, y * sizePx),
                            size = Size(sizePx, sizePx),
                            blendMode = BlendMode.Clear
                        )
                    }
                }
            }
        }

    /**
     * Slide effect that moves in discrete pixel steps.
     * @param progress 0.0 to 1.0
     * @param offset distance to slide from
     * @param pixelSize size of the step
     */
    fun Modifier.pixelSlide(
        progress: Float,
        offset: Offset = Offset(0f, 50f),
        pixelSize: Dp = 4.dp
    ): Modifier = graphicsLayer {
        val pixelSizePx = pixelSize.toPx()
        val currentProgress = 1f - progress

        translationX = floor((offset.x * currentProgress) / pixelSizePx) * pixelSizePx
        translationY = floor((offset.y * currentProgress) / pixelSizePx) * pixelSizePx
    }

    /**
     * Assemble effect where elements appear by "snapping" from scattered positions.
     * @param progress 0.0 to 1.0
     * @param scatter range of initial scatter
     */
    fun Modifier.pixelAssemble(
        progress: Float,
        scatter: Float = 100f,
        pixelSize: Dp = 4.dp
    ): Modifier = graphicsLayer {
        if (progress >= 1f) return@graphicsLayer

        val pixelSizePx = pixelSize.toPx()
        val invProgress = 1f - progress

        // Use a simple sine-based scatter that converges to 0
        val offsetX = sin(progress * 10f) * scatter * invProgress
        val offsetY = scatter * invProgress

        translationX = floor(offsetX / pixelSizePx) * pixelSizePx
        translationY = floor(offsetY / pixelSizePx) * pixelSizePx
        alpha = progress
    }

    // Standard transitions using the above modifiers would require custom Enter/Exit transitions
    // which are harder to define with custom modifiers directly.
    // Instead, we provide helper methods to create them.

    fun pixelFadeIn(duration: Int = 300): EnterTransition = fadeIn(tween(duration))

    fun pixelFadeOut(duration: Int = 300): ExitTransition = fadeOut(tween(duration))
}

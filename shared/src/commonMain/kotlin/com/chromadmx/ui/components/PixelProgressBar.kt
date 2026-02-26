package com.chromadmx.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.theme.PixelShape

/**
 * A chunky segmented progress bar with optional shimmer and indeterminate mode.
 */
@Composable
fun PixelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    containerColor: Color = PixelDesign.colors.surfaceVariant,
    progressColor: Color = PixelDesign.colors.primary,
    pixelSize: Dp = PixelDesign.spacing.pixelSize,
    segments: Int = 10,
    indeterminate: Boolean = false,
    showPercentage: Boolean = false,
) {
    val barShape = PixelShape.Small
    val reduceMotion = PixelDesign.reduceMotion

    // Shimmer animation for the fill (static when reduced motion)
    val shimmerOffset = if (reduceMotion) {
        0f
    } else {
        val shimmerConfig = ChromaAnimations.shimmerSweep
        val shimmerTransition = rememberInfiniteTransition(label = "progressShimmer")
        val animated by shimmerTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(shimmerConfig.durationMillis, easing = shimmerConfig.easing),
                repeatMode = shimmerConfig.repeatMode,
            ),
            label = "shimmerOffset",
        )
        animated
    }

    // Indeterminate marching animation (static when reduced motion)
    val marchOffset = if (reduceMotion) {
        0f
    } else {
        val indeterminateTransition = rememberInfiniteTransition(label = "indeterminate")
        val animated by indeterminateTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "marchOffset",
        )
        animated
    }

    val primaryColor = PixelDesign.colors.primary
    val secondaryColor = PixelDesign.colors.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(pixelSize * 4)
            .pixelBorder(chamfer = 6.dp)
            .clip(barShape)
            .background(containerColor, barShape)
            .padding(pixelSize),
        contentAlignment = Alignment.Center,
    ) {
        if (indeterminate) {
            // Indeterminate mode: marching gradient across full width
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val totalWidth = size.width
                        val bandWidth = totalWidth * 0.4f
                        val offset = marchOffset * (totalWidth + bandWidth) - bandWidth
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor,
                                secondaryColor,
                                primaryColor,
                                Color.Transparent,
                            ),
                            start = Offset(offset, 0f),
                            end = Offset(offset + bandWidth, 0f),
                        )
                        onDrawBehind {
                            drawRect(brush)
                        }
                    }
            )
        } else {
            // Determinate mode: segmented fill with shimmer
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(pixelSize)
            ) {
                val activeSegments = (progress * segments).toInt()
                for (i in 0 until segments) {
                    val isActive = i < activeSegments
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .let { mod ->
                                if (isActive) {
                                    mod.drawWithCache {
                                        val totalWidth = size.width
                                        // Shimmer gradient across each active segment
                                        val sweepWidth = totalWidth * 2f
                                        val sweepOffset =
                                            shimmerOffset * sweepWidth - totalWidth
                                        val brush = Brush.linearGradient(
                                            colors = listOf(
                                                primaryColor,
                                                secondaryColor,
                                                primaryColor,
                                            ),
                                            start = Offset(sweepOffset, 0f),
                                            end = Offset(sweepOffset + sweepWidth, 0f),
                                        )
                                        onDrawBehind {
                                            drawRect(brush)
                                        }
                                    }
                                } else {
                                    mod.background(Color.Transparent)
                                }
                            }
                    )
                }
            }
        }

        // Percentage text overlay
        if (showPercentage && !indeterminate) {
            val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    color = PixelDesign.colors.onPrimary,
                ),
            )
        }
    }
}

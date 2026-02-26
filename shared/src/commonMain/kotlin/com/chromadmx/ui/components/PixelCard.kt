package com.chromadmx.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelShape

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelDesign.colors.surface,
    borderColor: Color = PixelDesign.colors.outline,
    elevation: Dp = 4.dp,
    glowing: Boolean = false,
    onClick: (() -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // Press animation using ChromaAnimations.cardExpand spring
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardExpandSpring = ChromaAnimations.cardExpand
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = cardExpandSpring.dampingRatio,
            stiffness = cardExpandSpring.stiffness,
        )
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (title != null) {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                title()
            }
        }

        Box(
            modifier = Modifier
                .let { mod ->
                    if (onClick != null) {
                        mod.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        mod
                    }
                }
        ) {
            // Hard Shadow — 4dp offset, black 20%
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = elevation, y = elevation)
                    .clip(PixelShape.Large)
                    .pixelBorder(chamfer = 9.dp)
                    .background(Color.Black.copy(alpha = 0.2f), PixelShape.Large)
            )

            // Card Body — glowing border when requested
            val bodyModifier = Modifier
                .clip(PixelShape.Large)
                .let { mod ->
                    if (glowing) {
                        mod.pixelBorderGlowing(color = borderColor, chamfer = 9.dp)
                    } else {
                        mod.pixelBorder(color = borderColor, chamfer = 9.dp)
                    }
                }
                .background(backgroundColor, PixelShape.Large)
                .padding(16.dp)

            Box(modifier = bodyModifier) {
                content()
            }
        }
    }
}

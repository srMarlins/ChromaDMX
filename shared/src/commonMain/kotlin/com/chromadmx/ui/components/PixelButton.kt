package com.chromadmx.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily

@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = PixelDesign.colors.primary,
    contentColor: Color = PixelDesign.colors.onPrimary,
    disabledBackgroundColor: Color = PixelDesign.colors.surfaceVariant,
    disabledContentColor: Color = PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.5f),
    borderColor: Color = PixelDesign.colors.onPrimary.copy(alpha = 0.8f), // Darker border for matcha style
    pixelSize: Dp = PixelDesign.spacing.pixelSize,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp), // Chunkier padding
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Bouncy press animation
    // When pressed: 0dp offset (pressed down). When released: 6dp offset (popped up).
    // Wait, physically:
    //  - Normal state: Button is "up" (offset 0, shadow visible below?)
    //  - Pressed state: Button moves "down" (offset +Y)

    // Let's implement:
    // The visual "face" is offset upwards normally. When pressed, it moves down to 0.
    // Shadow is at 0. Face is at -depth.

    val pressDepth = 6.dp

    val currentOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) pressDepth else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val currentBgColor = if (enabled) backgroundColor else disabledBackgroundColor
    val currentContentColor = if (enabled) contentColor else disabledContentColor
    val currentBorderColor = if (enabled) borderColor else disabledBackgroundColor

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(top = pressDepth) // Reserve space for the "up" state
    ) {
        // Shadow / Bottom Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 0.dp) // Aligned at bottom
                .pixelBorder(color = currentBorderColor, pixelSize = pixelSize)
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Face / Top Layer
        Box(
            modifier = Modifier
                .offset { IntOffset(0, (currentOffset - pressDepth).roundToPx()) } // Moves from -6dp (up) to 0dp (down)
                .pixelBorder(color = currentBorderColor, pixelSize = pixelSize)
                .background(currentBgColor)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides currentContentColor) {
                ProvideTextStyle(
                    MaterialTheme.typography.labelLarge.copy(
                        color = currentContentColor,
                        fontFamily = PixelFontFamily
                    )
                ) {
                    content()
                }
            }
        }
    }
}

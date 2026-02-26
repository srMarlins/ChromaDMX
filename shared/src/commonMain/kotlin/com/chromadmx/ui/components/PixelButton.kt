package com.chromadmx.ui.components

import androidx.compose.animation.core.animateDpAsState
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
    borderColor: Color = PixelDesign.colors.outline,
    pixelSize: Dp = PixelDesign.spacing.pixelSize,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // When disabled, don't press down
    val pressedOffset by animateDpAsState(if (isPressed && enabled) 4.dp else 0.dp)

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
            .pixelBorder(color = currentBorderColor, pixelSize = pixelSize)
            .background(Color.Black.copy(alpha = 0.5f)) // Shadow/Depth base
            .padding(bottom = pixelSize) // Reserve space at bottom for the 'unpressed' state 3D effect?
            // Actually, simple offset logic:
            .offset { IntOffset(0, pressedOffset.roundToPx()) }
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

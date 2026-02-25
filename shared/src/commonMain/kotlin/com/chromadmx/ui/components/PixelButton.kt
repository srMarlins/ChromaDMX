package com.chromadmx.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.LocalPixelTheme
import com.chromadmx.ui.theme.PixelFontFamily
import kotlin.math.roundToInt

@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = Color.White,
    pixelSize: Dp = LocalPixelTheme.current.pixelSize,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val pressedOffset by animateFloatAsState(if (isPressed) 4f else 0f)

    // Visual adjustments for state
    val finalBackgroundColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
    val finalBorderColor = if (isFocused) MaterialTheme.colorScheme.secondary else borderColor
    val finalContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onClick = onClick
            )
            .pixelBorder(color = finalBorderColor, pixelSize = pixelSize)
            .background(finalBackgroundColor.copy(alpha = 0.5f)) // Background behind the border
            .padding(pixelSize) // Padding for the border width
            .offset { IntOffset(0, pressedOffset.roundToInt()) }
            .background(finalBackgroundColor)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(color = finalContentColor, fontFamily = PixelFontFamily)) {
            content()
        }
    }
}

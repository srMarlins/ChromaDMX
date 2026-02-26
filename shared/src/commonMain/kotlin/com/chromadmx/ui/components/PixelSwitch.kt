package com.chromadmx.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import com.chromadmx.ui.theme.PixelDesign

@Composable
fun PixelSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pixelSize: Dp = PixelDesign.spacing.pixelSize
) {
    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val thumbSize = 20.dp
    val padding = 6.dp

    val trackColor by animateColorAsState(
        if (checked) PixelDesign.colors.primary else PixelDesign.colors.surfaceVariant
    )

    val thumbOffset by animateDpAsState(
        if (checked) trackWidth - thumbSize - padding else padding
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange?.invoke(!checked) },
                role = Role.Switch
            )
            .pixelBorder(
                color = if (enabled) PixelDesign.colors.outline else PixelDesign.colors.outlineVariant,
                pixelSize = pixelSize
            )
            .background(trackColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(thumbSize)
                .pixelBorder(color = Color.Black, pixelSize = pixelSize)
                .background(Color.White)
        )
    }
}

package com.chromadmx.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelShape

@Composable
fun PixelSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val thumbSize = 20.dp
    val padding = 6.dp

    val trackColor by animateColorAsState(
        if (checked) PixelDesign.colors.primary else PixelDesign.colors.surfaceVariant
    )

    // Use ChromaAnimations.buttonPress spring for thumb animation
    val thumbSpring = ChromaAnimations.buttonPress
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = spring(
            dampingRatio = thumbSpring.dampingRatio,
            stiffness = thumbSpring.stiffness,
        )
    )

    val interactionSource = remember { MutableInteractionSource() }

    // Track uses chamfered shape; glowing when checked, standard when unchecked
    val trackModifier = modifier
        .size(width = trackWidth, height = trackHeight)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { onCheckedChange?.invoke(!checked) },
            role = Role.Switch
        )
        .clip(PixelShape.Small)
        .let { mod ->
            if (checked && enabled) {
                mod.pixelBorderGlowing(
                    color = PixelDesign.colors.outline,
                    glowColor = PixelDesign.colors.primary,
                    chamfer = 6.dp,
                )
            } else {
                mod.pixelBorder(
                    color = if (enabled) PixelDesign.colors.outline else PixelDesign.colors.outlineVariant,
                    chamfer = 6.dp,
                )
            }
        }
        .background(trackColor, PixelShape.Small)

    Box(
        modifier = trackModifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // Square blocky thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(thumbSize)
                .clip(PixelShape(4.dp))
                .pixelBorder(chamfer = 4.dp)
                .background(Color.White, PixelShape(4.dp))
        )
    }
}

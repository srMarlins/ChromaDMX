package com.chromadmx.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.theme.PixelShape

/**
 * Button variant controlling the color scheme.
 */
enum class PixelButtonVariant {
    /** Primary action — primary bg, onPrimary text. */
    Primary,
    /** Secondary action — secondary bg, onSecondary text. */
    Secondary,
    /** Surface-level action — surface bg, onSurface text. */
    Surface,
    /** Destructive action — error bg, onError text. */
    Danger,
}

@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: PixelButtonVariant = PixelButtonVariant.Primary,
    backgroundColor: Color = variantBackgroundColor(variant),
    contentColor: Color = variantContentColor(variant),
    disabledBackgroundColor: Color = PixelDesign.colors.surfaceVariant,
    disabledContentColor: Color = PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.5f),
    borderColor: Color = PixelDesign.colors.outline,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressDepth = 6.dp

    // Use ChromaAnimations.buttonPress spring for the press animation
    val buttonSpring = ChromaAnimations.buttonPress
    val currentOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) pressDepth else 0.dp,
        animationSpec = spring(
            dampingRatio = buttonSpring.dampingRatio,
            stiffness = buttonSpring.stiffness,
        )
    )

    val currentBgColor = if (enabled) backgroundColor else disabledBackgroundColor
    val currentContentColor = if (enabled) contentColor else disabledContentColor
    val currentBorderColor = if (enabled) borderColor else PixelDesign.colors.outlineVariant

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(PixelShape.Large)
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
                .offset(y = 0.dp)
                .pixelBorder(chamfer = 9.dp)
                .clip(PixelShape.Large)
                .background(Color.Black.copy(alpha = 0.3f), PixelShape.Large)
        )

        // Face / Top Layer — uses glowing border when enabled, standard when disabled
        val faceModifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, (currentOffset - pressDepth).roundToPx()) }
            .let { mod ->
                if (enabled) {
                    mod.pixelBorderGlowing(color = currentBorderColor, chamfer = 9.dp)
                } else {
                    mod.pixelBorder(color = currentBorderColor, chamfer = 9.dp)
                }
            }
            .clip(PixelShape.Large)
            .background(currentBgColor, PixelShape.Large)
            .padding(contentPadding)

        Box(
            modifier = faceModifier,
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

// ── Variant color helpers ────────────────────────────────────────────

@Composable
private fun variantBackgroundColor(variant: PixelButtonVariant): Color = when (variant) {
    PixelButtonVariant.Primary -> PixelDesign.colors.primary
    PixelButtonVariant.Secondary -> PixelDesign.colors.secondary
    PixelButtonVariant.Surface -> PixelDesign.colors.surface
    PixelButtonVariant.Danger -> PixelDesign.colors.error
}

@Composable
private fun variantContentColor(variant: PixelButtonVariant): Color = when (variant) {
    PixelButtonVariant.Primary -> PixelDesign.colors.onPrimary
    PixelButtonVariant.Secondary -> PixelDesign.colors.onSecondary
    PixelButtonVariant.Surface -> PixelDesign.colors.onSurface
    PixelButtonVariant.Danger -> PixelDesign.colors.onError
}

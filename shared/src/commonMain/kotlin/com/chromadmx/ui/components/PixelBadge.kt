package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.theme.PixelShape

/**
 * Badge variant controlling the color scheme.
 */
enum class PixelBadgeVariant {
    /** Primary — primary bg, onPrimary text. */
    Primary,
    /** Secondary — secondary bg, onSecondary text. */
    Secondary,
    /** Tertiary — tertiary bg, onTertiary text. */
    Tertiary,
    /** Error — error bg, onError text. */
    Error,
    /** Info — info bg, onBackground text. */
    Info,
}

@Composable
fun PixelBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: PixelBadgeVariant = PixelBadgeVariant.Secondary,
    containerColor: Color = badgeContainerColor(variant),
    contentColor: Color = badgeContentColor(variant),
) {
    val badgeShape = PixelShape(4.dp)
    Box(
        modifier = modifier
            .pixelBorder(chamfer = 4.dp)
            .clip(badgeShape)
            .background(containerColor, badgeShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = contentColor,
                fontFamily = PixelFontFamily
            )
        )
    }
}

// ── Variant color helpers ────────────────────────────────────────────

@Composable
private fun badgeContainerColor(variant: PixelBadgeVariant): Color = when (variant) {
    PixelBadgeVariant.Primary -> PixelDesign.colors.primary
    PixelBadgeVariant.Secondary -> PixelDesign.colors.secondary
    PixelBadgeVariant.Tertiary -> PixelDesign.colors.tertiary
    PixelBadgeVariant.Error -> PixelDesign.colors.error
    PixelBadgeVariant.Info -> PixelDesign.colors.info
}

@Composable
private fun badgeContentColor(variant: PixelBadgeVariant): Color = when (variant) {
    PixelBadgeVariant.Primary -> PixelDesign.colors.onPrimary
    PixelBadgeVariant.Secondary -> PixelDesign.colors.onSecondary
    PixelBadgeVariant.Tertiary -> PixelDesign.colors.onTertiary
    PixelBadgeVariant.Error -> PixelDesign.colors.onError
    PixelBadgeVariant.Info -> PixelDesign.colors.onBackground
}

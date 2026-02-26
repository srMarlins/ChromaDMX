package com.chromadmx.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A pixel-art chamfered corner shape that creates an octagonal outline.
 *
 * Instead of rounded corners, each corner is cut with a diagonal line
 * at 45 degrees, producing the classic pixel-art / retro UI look.
 *
 * Standard sizes:
 * - `PixelShape(6.dp)` for small components (badges, text fields, toggles, chips)
 * - `PixelShape(9.dp)` for large components (buttons, cards, dialogs, bottom sheets)
 *
 * @param chamferSize The length of the diagonal cut at each corner.
 */
class PixelShape(private val chamferSize: Dp = 6.dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val c = with(density) { chamferSize.toPx() }
        // Clamp chamfer so it never exceeds half the smallest dimension
        val clampedC = c.coerceAtMost(minOf(size.width, size.height) / 2f)

        val path = Path().apply {
            // Start at top-left chamfer end (moving clockwise)
            moveTo(clampedC, 0f)

            // Top edge
            lineTo(size.width - clampedC, 0f)

            // Top-right chamfer
            lineTo(size.width, clampedC)

            // Right edge
            lineTo(size.width, size.height - clampedC)

            // Bottom-right chamfer
            lineTo(size.width - clampedC, size.height)

            // Bottom edge
            lineTo(clampedC, size.height)

            // Bottom-left chamfer
            lineTo(0f, size.height - clampedC)

            // Left edge
            lineTo(0f, clampedC)

            // Close back to start (top-left chamfer)
            close()
        }

        return Outline.Generic(path)
    }

    override fun toString(): String = "PixelShape(chamferSize=$chamferSize)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PixelShape) return false
        return chamferSize == other.chamferSize
    }

    override fun hashCode(): Int = chamferSize.hashCode()

    companion object {
        /** Small chamfer for compact components: badges, text fields, toggles, chips. */
        val Small = PixelShape(6.dp)

        /** Large chamfer for prominent components: buttons, cards, dialogs, bottom sheets. */
        val Large = PixelShape(9.dp)
    }
}

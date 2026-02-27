package com.chromadmx.ui.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Shared top-down fixture drawing functions used by both [VenueCanvas][com.chromadmx.ui.components.VenueCanvas]
 * and [TopDownEditor].
 *
 * Each function renders a fixture at the given screen position using the
 * pixel-art aesthetic (radial glow, square housings, CRT-style colors).
 */
object TopDownRenderer {

    /** Standardized dark fixture housing color. */
    val HousingColor = Color(0xFF1A1A2E)

    /** Lighter border for fixture housing — bright enough to contrast against dark backdrops. */
    val HousingBorderColor = Color(0xFF2A2A3E)

    /**
     * Draw a PAR fixture: square housing with colored lens, radial glow, mount brackets.
     */
    fun DrawScope.drawParFixture(
        cx: Float,
        cy: Float,
        color: Color,
        isSelected: Boolean,
        selectionColor: Color,
        scale: Float = 1f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val housingSize = 16f * scale
        val half = housingSize / 2f
        val lensInset = 2f * scale
        val lensSize = housingSize - 2 * lensInset

        // Radial glow (light output)
        val glowRadius = 24f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(cx, cy),
        )
        // Housing border
        drawRect(
            color = housingBorderColor,
            topLeft = Offset(cx - half - 1f, cy - half - 1f),
            size = Size(housingSize + 2f, housingSize + 2f),
        )
        // Housing body
        drawRect(
            color = housingColor,
            topLeft = Offset(cx - half, cy - half),
            size = Size(housingSize, housingSize),
        )
        // Colored lens
        drawRect(
            color = color,
            topLeft = Offset(cx - half + lensInset, cy - half + lensInset),
            size = Size(lensSize, lensSize),
        )
        // Mount brackets (two ticks on top edge)
        drawRect(housingBorderColor, Offset(cx - 4f * scale, cy - half - 3f * scale), Size(2f * scale, 3f * scale))
        drawRect(housingBorderColor, Offset(cx + 2f * scale, cy - half - 3f * scale), Size(2f * scale, 3f * scale))

        if (isSelected) drawSelectionBorder(cx, cy, half + 2f, selectionColor)
    }

    /**
     * Draw a STROBE fixture: wide rectangular flash panel with sharp white glow.
     */
    fun DrawScope.drawStrobeFixture(
        cx: Float,
        cy: Float,
        color: Color,
        isSelected: Boolean,
        selectionColor: Color,
        scale: Float = 1f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val width = 22f * scale
        val height = 10f * scale
        val halfW = width / 2f
        val halfH = height / 2f

        // Sharp flash glow
        val glowRadius = 28f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(cx, cy),
        )
        // Housing border
        drawRect(
            color = housingBorderColor,
            topLeft = Offset(cx - halfW - 1f, cy - halfH - 1f),
            size = Size(width + 2f, height + 2f),
        )
        // Housing body
        drawRect(
            color = housingColor,
            topLeft = Offset(cx - halfW, cy - halfH),
            size = Size(width, height),
        )
        // Flash panel (bright white tinted with color)
        val flashColor = Color(
            red = (color.red + 1f) / 2f,
            green = (color.green + 1f) / 2f,
            blue = (color.blue + 1f) / 2f,
        )
        drawRect(
            color = flashColor,
            topLeft = Offset(cx - halfW + 2f * scale, cy - halfH + 2f * scale),
            size = Size(width - 4f * scale, height - 4f * scale),
        )

        if (isSelected) drawSelectionBorder(cx, cy, halfW + 2f, selectionColor)
    }

    /**
     * Draw a WASH fixture: larger housing with wide soft radial glow.
     */
    fun DrawScope.drawWashFixture(
        cx: Float,
        cy: Float,
        color: Color,
        isSelected: Boolean,
        selectionColor: Color,
        scale: Float = 1f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val housingSize = 20f * scale
        val half = housingSize / 2f
        val lensRadius = 7f * scale

        // Wide soft glow (larger radius for wash)
        val glowRadius = 36f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(cx, cy),
        )
        // Housing border
        drawRect(
            color = housingBorderColor,
            topLeft = Offset(cx - half - 1f, cy - half - 1f),
            size = Size(housingSize + 2f, housingSize + 2f),
        )
        // Housing body
        drawRect(
            color = housingColor,
            topLeft = Offset(cx - half, cy - half),
            size = Size(housingSize, housingSize),
        )
        // Round lens (circle inside square housing)
        drawCircle(
            color = color,
            radius = lensRadius,
            center = Offset(cx, cy),
        )
        // Mount brackets
        drawRect(housingBorderColor, Offset(cx - 5f * scale, cy - half - 3f * scale), Size(2f * scale, 3f * scale))
        drawRect(housingBorderColor, Offset(cx + 3f * scale, cy - half - 3f * scale), Size(2f * scale, 3f * scale))

        if (isSelected) drawSelectionBorder(cx, cy, half + 2f, selectionColor)
    }

    /**
     * Draw a BAR fixture (Pixel Bar): horizontal row of colored segments.
     */
    fun DrawScope.drawBarFixture(
        cx: Float,
        cy: Float,
        color: Color,
        pixelCount: Int,
        isSelected: Boolean,
        selectionColor: Color,
        scale: Float = 1f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val segmentW = 8f * scale
        val segmentH = 12f * scale
        val gap = 2f * scale
        val totalW = pixelCount * segmentW + (pixelCount - 1) * gap
        val startX = cx - totalW / 2f
        val startY = cy - segmentH / 2f

        // Radial glow around entire bar (omnidirectional)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(cx, cy),
                radius = totalW / 2f + 12f,
            ),
            radius = totalW / 2f + 12f,
            center = Offset(cx, cy),
        )

        // Bar housing background
        drawRect(
            color = housingColor,
            topLeft = Offset(startX - 3f, startY - 3f),
            size = Size(totalW + 6f, segmentH + 6f),
        )

        // Individual pixel segments
        for (i in 0 until pixelCount) {
            val segX = startX + i * (segmentW + gap)
            // Slight brightness variation per segment for visual interest
            val brightness = 0.8f + 0.2f * ((i % 3).toFloat() / 2f)
            drawRect(
                color = color.copy(alpha = brightness),
                topLeft = Offset(segX, startY),
                size = Size(segmentW, segmentH),
            )
        }

        if (isSelected) {
            val selRadius = (totalW / 2f).coerceAtLeast(16f)
            drawSelectionBorder(cx, cy, selRadius, selectionColor)
        }
    }

    /**
     * Draw a BEAM_CONE fixture (Moving Head): square housing with directional beam cone.
     */
    fun DrawScope.drawBeamConeFixture(
        cx: Float,
        cy: Float,
        color: Color,
        isSelected: Boolean,
        reusablePath: Path,
        selectionColor: Color,
        scale: Float = 1f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        // Beam cone (downward triangle-like glow)
        val beamLength = 30f * scale
        val beamHalfWidth = 12f * scale
        reusablePath.reset()
        reusablePath.moveTo(cx, cy)
        reusablePath.lineTo(cx - beamHalfWidth, cy + beamLength)
        reusablePath.lineTo(cx + beamHalfWidth, cy + beamLength)
        reusablePath.close()
        drawPath(
            path = reusablePath,
            color = color.copy(alpha = 0.2f),
        )

        // Square housing (schematic style)
        val housingSize = 14f * scale
        val hh = housingSize / 2f
        drawRect(
            color = housingBorderColor,
            topLeft = Offset(cx - hh - 1f, cy - hh - 1f),
            size = Size(housingSize + 2f, housingSize + 2f),
        )
        drawRect(
            color = housingColor,
            topLeft = Offset(cx - hh, cy - hh),
            size = Size(housingSize, housingSize),
        )
        // Inner color lens
        val lensInset = 2f * scale
        drawRect(
            color = color,
            topLeft = Offset(cx - hh + lensInset, cy - hh + lensInset),
            size = Size(housingSize - 2 * lensInset, housingSize - 2 * lensInset),
        )
        // Directional indicator (short line pointing down = default tilt direction)
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(cx, cy + hh),
            end = Offset(cx, cy + hh + 8f * scale),
            strokeWidth = 2f,
        )

        if (isSelected) {
            drawSelectionBorder(cx, cy, hh + 2f, selectionColor)
        }
    }

    /**
     * Draw a pixelated selection border around a fixture.
     */
    fun DrawScope.drawSelectionBorder(cx: Float, cy: Float, radius: Float, color: Color) {
        val r = radius + 4f
        val pixel = 3f

        // Top edge
        drawRect(color, Offset(cx - r, cy - r), Size(2 * r, pixel))
        // Bottom edge
        drawRect(color, Offset(cx - r, cy + r - pixel), Size(2 * r, pixel))
        // Left edge
        drawRect(color, Offset(cx - r, cy - r), Size(pixel, 2 * r))
        // Right edge
        drawRect(color, Offset(cx + r - pixel, cy - r), Size(pixel, 2 * r))
    }
}

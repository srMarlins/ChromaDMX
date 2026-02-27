package com.chromadmx.ui.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Canvas drawing functions for rendering individual fixture types in
 * the stage preview.
 *
 * Each function draws a fixture at the given screen position using the
 * pixel-art aesthetic (glow halos, solid blocks, CRT-style colors)
 * established by VenueCanvas.
 */
object FixtureRenderer {

    /** Standardized dark fixture housing color (default; prefer theme via PixelDesign.colors.fixtureHousing). */
    val HousingColor = Color(0xFF1A1A2E)

    /** Lighter border for fixture housing (default; prefer theme via PixelDesign.colors.fixtureHousingBorder). */
    val HousingBorderColor = Color(0xFF2A2A3E)

    /**
     * Draw a PAR fixture as a square housing with colored lens and radial glow.
     */
    fun DrawScope.drawPar(
        position: Offset,
        color: Color,
        size: Float = 16f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val half = size / 2f
        val lensInset = 2f
        // Radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.35f), Color.Transparent),
                center = position,
                radius = size * 1.5f,
            ),
            radius = size * 1.5f,
            center = position,
        )
        // Housing border + body
        drawRect(housingBorderColor, Offset(position.x - half - 1f, position.y - half - 1f), Size(size + 2f, size + 2f))
        drawRect(housingColor, Offset(position.x - half, position.y - half), Size(size, size))
        // Colored lens
        drawRect(color, Offset(position.x - half + lensInset, position.y - half + lensInset), Size(size - 2 * lensInset, size - 2 * lensInset))
        // Mount brackets
        drawRect(housingBorderColor, Offset(position.x - 4f, position.y - half - 3f), Size(2f, 3f))
        drawRect(housingBorderColor, Offset(position.x + 2f, position.y - half - 3f), Size(2f, 3f))
    }

    /**
     * Draw a moving head with beam direction indicator.
     *
     * @param panTilt Normalized pan/tilt direction for the indicator line.
     * @param beamAngle Beam angle in degrees (unused visually, reserved for future).
     */
    fun DrawScope.drawMovingHead(
        position: Offset,
        color: Color,
        panTilt: Offset = Offset.Zero,
        @Suppress("UNUSED_PARAMETER") beamAngle: Float = 15f,
        size: Float = 14f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val half = size / 2f
        // Fixture body (dark housing)
        drawRect(
            housingBorderColor,
            topLeft = Offset(position.x - half - 1f, position.y - half - 1f),
            size = Size(size + 2f, size + 2f),
        )
        drawRect(
            housingColor,
            topLeft = Offset(position.x - half, position.y - half),
            size = Size(size, size),
        )
        // Inner color lens
        drawRect(color, topLeft = Offset(position.x - half + 2f, position.y - half + 2f), size = Size(size - 4f, size - 4f))
        // Pan/tilt direction indicator line
        val dirX = position.x + panTilt.x * 12f
        val dirY = position.y + panTilt.y * 12f
        drawLine(color.copy(alpha = 0.7f), position, Offset(dirX, dirY), strokeWidth = 2f)
    }

    /**
     * Draw a pixel bar as a row of colored segments.
     *
     * @param segments Number of LED segments to render.
     * @param segmentColors Per-segment colors; falls back to [baseColor] if null or short.
     */
    fun DrawScope.drawPixelBar(
        position: Offset,
        segments: Int = 8,
        segmentColors: List<Color>? = null,
        baseColor: Color = Color.White,
        segmentSize: Float = 8f,
        housingColor: Color = HousingColor,
    ) {
        val gap = 2f
        val totalW = segments * segmentSize + (segments - 1) * gap
        val startX = position.x - totalW / 2f
        val startY = position.y - segmentSize / 2f
        // Subtle circular glow behind segment row
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor.copy(alpha = 0.15f), Color.Transparent),
                center = position,
                radius = totalW / 2f + 10f,
            ),
            radius = totalW / 2f + 10f,
            center = position,
        )
        // Housing
        drawRect(
            housingColor,
            topLeft = Offset(startX - 2f, startY - 2f),
            size = Size(totalW + 4f, segmentSize + 4f),
        )
        // Segments
        for (i in 0 until segments) {
            val c = segmentColors?.getOrNull(i) ?: baseColor
            val segX = startX + i * (segmentSize + gap)
            drawRect(c, topLeft = Offset(segX, startY), size = Size(segmentSize, segmentSize))
        }
    }

    /**
     * Draw a strobe as a wide rectangular flash panel with sharp glow.
     */
    fun DrawScope.drawStrobe(
        position: Offset,
        color: Color,
        size: Float = 14f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val width = size * 1.6f
        val height = size * 0.7f
        val halfW = width / 2f
        val halfH = height / 2f
        // Sharp flash glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                center = position,
                radius = size * 2f,
            ),
            radius = size * 2f,
            center = position,
        )
        // Housing
        drawRect(housingBorderColor, Offset(position.x - halfW - 1f, position.y - halfH - 1f), Size(width + 2f, height + 2f))
        drawRect(housingColor, Offset(position.x - halfW, position.y - halfH), Size(width, height))
        // Flash panel
        val flashColor = Color(
            red = (color.red + 1f) / 2f,
            green = (color.green + 1f) / 2f,
            blue = (color.blue + 1f) / 2f,
        )
        drawRect(flashColor, Offset(position.x - halfW + 2f, position.y - halfH + 2f), Size(width - 4f, height - 4f))
    }

    /**
     * Draw a wash fixture as a larger housing with wide soft radial glow.
     */
    fun DrawScope.drawWash(
        position: Offset,
        color: Color,
        size: Float = 20f,
        housingColor: Color = HousingColor,
        housingBorderColor: Color = HousingBorderColor,
    ) {
        val half = size / 2f
        val lensRadius = size * 0.35f
        // Wide soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                center = position,
                radius = size * 1.8f,
            ),
            radius = size * 1.8f,
            center = position,
        )
        // Housing
        drawRect(housingBorderColor, Offset(position.x - half - 1f, position.y - half - 1f), Size(size + 2f, size + 2f))
        drawRect(housingColor, Offset(position.x - half, position.y - half), Size(size, size))
        // Round lens
        drawCircle(color, radius = lensRadius, center = position)
        // Mount brackets
        drawRect(housingBorderColor, Offset(position.x - 5f, position.y - half - 3f), Size(2f, 3f))
        drawRect(housingBorderColor, Offset(position.x + 3f, position.y - half - 3f), Size(2f, 3f))
    }

    /**
     * Draw a pixelated selection border around a fixture.
     */
    fun DrawScope.drawSelection(
        position: Offset,
        size: Float = 20f,
        selectionColor: Color,
    ) {
        val r = size / 2f + 3f
        val pixel = 3f
        // Top edge
        drawRect(selectionColor, Offset(position.x - r, position.y - r), Size(2 * r, pixel))
        // Bottom edge
        drawRect(selectionColor, Offset(position.x - r, position.y + r - pixel), Size(2 * r, pixel))
        // Left edge
        drawRect(selectionColor, Offset(position.x - r, position.y - r), Size(pixel, 2 * r))
        // Right edge
        drawRect(selectionColor, Offset(position.x + r - pixel, position.y - r), Size(pixel, 2 * r))
    }
}

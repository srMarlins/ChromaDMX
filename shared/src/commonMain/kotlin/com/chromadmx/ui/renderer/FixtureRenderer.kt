package com.chromadmx.ui.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Canvas drawing functions for rendering individual fixture types in
 * the isometric stage preview.
 *
 * Each function draws a fixture at the given screen position using the
 * pixel-art aesthetic (glow halos, solid blocks, CRT-style colors)
 * established by VenueCanvas.
 */
object FixtureRenderer {

    /**
     * Draw a PAR/wash/strobe fixture as a pixel block with glow halo.
     */
    fun DrawScope.drawPar(position: Offset, color: Color, size: Float = 16f) {
        // Outer glow halo
        drawCircle(color.copy(alpha = 0.2f), radius = size * 1.5f, center = position)
        // Mid glow
        drawCircle(color.copy(alpha = 0.5f), radius = size * 0.9f, center = position)
        // Pixel block (square for pixel aesthetic)
        val half = size / 2f
        drawRect(color, topLeft = Offset(position.x - half, position.y - half), size = Size(size, size))
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
    ) {
        val half = size / 2f
        // Fixture body (dark housing)
        drawRect(
            Color(0xFF2A2A3E),
            topLeft = Offset(position.x - half - 1f, position.y - half - 1f),
            size = Size(size + 2f, size + 2f),
        )
        // Inner color
        drawRect(color, topLeft = Offset(position.x - half, position.y - half), size = Size(size, size))
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
    ) {
        val gap = 2f
        val totalW = segments * segmentSize + (segments - 1) * gap
        val startX = position.x - totalW / 2f
        val startY = position.y - segmentSize / 2f
        // Housing
        drawRect(
            Color(0xFF1A1A2E),
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
     * Draw a strobe as a flashing block with intensity-driven glow.
     */
    fun DrawScope.drawStrobe(position: Offset, intensity: Float, size: Float = 14f) {
        val color = Color.White.copy(alpha = intensity.coerceIn(0f, 1f))
        val half = size / 2f
        // Flash glow at high intensity
        if (intensity > 0.5f) {
            drawCircle(Color.White.copy(alpha = intensity * 0.3f), radius = size * 2f, center = position)
        }
        drawRect(color, topLeft = Offset(position.x - half, position.y - half), size = Size(size, size))
    }

    /**
     * Draw a wash fixture as concentric floor glow circles.
     */
    fun DrawScope.drawWash(position: Offset, color: Color, radius: Float = 24f) {
        drawCircle(color.copy(alpha = 0.15f), radius = radius, center = position)
        drawCircle(color.copy(alpha = 0.3f), radius = radius * 0.6f, center = position)
        drawCircle(color, radius = 6f, center = position)
    }

    /**
     * Draw a pixelated selection border around a fixture (cyan rectangle).
     */
    fun DrawScope.drawSelection(position: Offset, size: Float = 20f) {
        val selColor = Color(0xFF00FBFF) // cyan matching VenueCanvas
        val r = size / 2f + 3f
        val pixel = 3f
        // Top edge
        drawRect(selColor, Offset(position.x - r, position.y - r), Size(2 * r, pixel))
        // Bottom edge
        drawRect(selColor, Offset(position.x - r, position.y + r - pixel), Size(2 * r, pixel))
        // Left edge
        drawRect(selColor, Offset(position.x - r, position.y - r), Size(pixel, 2 * r))
        // Right edge
        drawRect(selColor, Offset(position.x + r - pixel, position.y - r), Size(pixel, 2 * r))
    }
}

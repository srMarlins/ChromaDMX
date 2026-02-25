package com.chromadmx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.ui.theme.StageBackground
import com.chromadmx.core.model.Color as DmxColor

/**
 * Canvas-based top-down venue visualization.
 *
 * Renders each fixture as a colored circle at its (x, y) position,
 * with color driven by the engine's latest computed fixture colors.
 * The canvas scales fixture positions to fit the available space.
 *
 * @param fixtures List of fixtures with 3D positions.
 * @param fixtureColors Parallel list of colors, one per fixture.
 *        If shorter than [fixtures], missing fixtures render as dim gray.
 */
@Composable
fun VenueCanvas(
    fixtures: List<Fixture3D>,
    fixtureColors: List<DmxColor>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(StageBackground),
    ) {
        if (fixtures.isEmpty()) return@Canvas

        val padding = 40f
        val canvasW = size.width - 2 * padding
        val canvasH = size.height - 2 * padding

        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        // Compute bounds
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        for (f in fixtures) {
            if (f.position.x < minX) minX = f.position.x
            if (f.position.x > maxX) maxX = f.position.x
            if (f.position.y < minY) minY = f.position.y
            if (f.position.y > maxY) maxY = f.position.y
        }

        val rangeX = (maxX - minX).coerceAtLeast(1f)
        val rangeY = (maxY - minY).coerceAtLeast(1f)

        for ((index, fixture) in fixtures.withIndex()) {
            val normX = (fixture.position.x - minX) / rangeX
            val normY = (fixture.position.y - minY) / rangeY
            val cx = padding + normX * canvasW
            val cy = padding + (1f - normY) * canvasH // Flip Y for top-down

            val dmxColor = fixtureColors.getOrNull(index) ?: DmxColor.BLACK
            val composeColor = dmxColor.toComposeColor()

            // Outer glow
            drawCircle(
                color = composeColor.copy(alpha = 0.3f),
                radius = 18f,
                center = Offset(cx, cy),
            )

            // Inner dot
            drawCircle(
                color = composeColor,
                radius = 10f,
                center = Offset(cx, cy),
            )
        }
    }
}

/**
 * Convert a DMX [DmxColor] (0-1 floats) to a Compose [Color].
 */
private fun DmxColor.toComposeColor(): Color {
    return Color(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = 1f,
    )
}

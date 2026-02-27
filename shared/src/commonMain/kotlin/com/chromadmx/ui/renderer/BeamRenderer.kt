package com.chromadmx.ui.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Beam cone rendering for the stage preview.
 *
 * Draws translucent light cones from fixture positions to the floor,
 * plus floor glow circles where beams impact the stage surface.
 */
object BeamRenderer {

    /**
     * Draw a beam cone from fixture position to floor.
     *
     * Renders a translucent triangle from [fixturePos] spreading to
     * [beamWidth] at [floorPos], with a brighter center line.
     */
    fun DrawScope.drawBeamCone(
        fixturePos: Offset,
        floorPos: Offset,
        color: Color,
        reusablePath: Path = Path(),
        alpha: Float = 0.15f,
        beamWidth: Float = 24f,
    ) {
        reusablePath.reset()
        reusablePath.moveTo(fixturePos.x, fixturePos.y)
        reusablePath.lineTo(floorPos.x - beamWidth / 2f, floorPos.y)
        reusablePath.lineTo(floorPos.x + beamWidth / 2f, floorPos.y)
        reusablePath.close()
        drawPath(reusablePath, color.copy(alpha = alpha))
        // Brighter center line
        drawLine(color.copy(alpha = alpha * 0.5f), fixturePos, floorPos, strokeWidth = 2f)
    }

    /**
     * Draw floor glow where a beam hits the stage surface.
     *
     * Renders concentric translucent circles for a soft light pool effect.
     */
    fun DrawScope.drawFloorGlow(position: Offset, color: Color, radius: Float = 16f) {
        drawCircle(color.copy(alpha = 0.1f), radius = radius, center = position)
        drawCircle(color.copy(alpha = 0.2f), radius = radius * 0.5f, center = position)
    }
}

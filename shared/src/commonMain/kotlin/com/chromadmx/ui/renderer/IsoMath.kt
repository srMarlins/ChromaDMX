package com.chromadmx.ui.renderer

import androidx.compose.ui.geometry.Offset
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.ui.state.IsoAngle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure math functions for isometric coordinate conversion.
 *
 * Provides world-to-screen and screen-to-world transformations for the
 * isometric stage preview, plus depth sorting for the painter's algorithm.
 */
object IsoMath {
    /**
     * Convert world coordinates to isometric screen position.
     *
     * Uses standard isometric projection:
     * - screenX = (x - y) * cos(angle)
     * - screenY = (x + y) * sin(angle) - z
     */
    fun worldToIso(x: Float, y: Float, z: Float, angle: IsoAngle): Offset {
        val radians = angle.toRadians()
        val cosA = cos(radians)
        val sinA = sin(radians)
        val screenX = (x - y) * cosA
        val screenY = (x + y) * sinA - z
        return Offset(screenX, screenY)
    }

    fun worldToIso(position: Vec3, angle: IsoAngle): Offset =
        worldToIso(position.x, position.y, position.z, angle)

    /**
     * Convert screen position back to world coordinates (assumes z=0 ground plane).
     *
     * Inverse of the projection with z=0:
     * - x = screenX / (2 * cosA) + screenY / (2 * sinA)
     * - y = -screenX / (2 * cosA) + screenY / (2 * sinA)
     */
    fun isoToWorld(screenX: Float, screenY: Float, angle: IsoAngle): Pair<Float, Float> {
        val radians = angle.toRadians()
        val cosA = cos(radians)
        val sinA = sin(radians)
        if (cosA == 0f || sinA == 0f) return Pair(0f, 0f)
        val worldX = screenX / (2f * cosA) + screenY / (2f * sinA)
        val worldY = -screenX / (2f * cosA) + screenY / (2f * sinA)
        return Pair(worldX, worldY)
    }

    /**
     * Sort fixtures by depth for painter's algorithm (back to front).
     *
     * Returns indexed fixtures sorted so that the furthest from the camera
     * are drawn first, ensuring correct overlap for isometric rendering.
     */
    fun sortFixturesByDepth(fixtures: List<Fixture3D>, angle: IsoAngle): List<IndexedValue<Fixture3D>> {
        return fixtures.withIndex().sortedBy { (_, f) ->
            val radians = angle.toRadians()
            val cosA = cos(radians)
            val sinA = sin(radians)
            // Depth increases with (x + y) for standard angles; negate for back-to-front
            -(f.position.x * sinA + f.position.y * cosA) + f.position.z
        }
    }
}

/**
 * Convert [IsoAngle] enum to radians for trigonometric calculations.
 */
fun IsoAngle.toRadians(): Float = when (this) {
    IsoAngle.ZERO -> 0.4636f        // ~26.57 deg (true isometric = atan(0.5))
    IsoAngle.FORTY_FIVE -> 0.7854f  // 45 deg
    IsoAngle.NINETY -> 1.0472f      // 60 deg (steeper angle)
}

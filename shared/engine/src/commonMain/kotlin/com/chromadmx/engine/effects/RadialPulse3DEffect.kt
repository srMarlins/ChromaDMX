package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.MathUtils
import com.chromadmx.engine.effect.SpatialEffect
import kotlin.math.abs

/**
 * Expanding spherical pulse radiating outward from a center point.
 *
 * A thin shell of color expands from [center] at [speed] units/sec.
 * The shell has a configurable [width]; fixtures within the shell glow
 * at [color], fading smoothly at the edges.
 *
 * **Params:**
 * - `centerX` (Float) — center X. Default: 0.
 * - `centerY` (Float) — center Y. Default: 0.
 * - `centerZ` (Float) — center Z. Default: 0.
 * - `speed`   (Float) — expansion speed in units/sec. Default: 2.0.
 * - `color`   (Color) — pulse color. Default: [Color.WHITE].
 * - `width`   (Float) — shell thickness. Default: 0.3.
 */
class RadialPulse3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Radial Pulse 3D"

    override fun compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
        val center = Vec3(
            params.getFloat("centerX", 0f),
            params.getFloat("centerY", 0f),
            params.getFloat("centerZ", 0f)
        )
        val speed = params.getFloat("speed", 2.0f)
        val color = params.getColor("color", Color.WHITE)
        val width = params.getFloat("width", 0.3f).coerceAtLeast(0.001f)

        val dist = pos.distanceTo(center)
        val radius = time * speed

        // Distance of fixture from the expanding shell
        val shellDist = abs(dist - radius)

        // Smooth falloff: 1 at shell center, 0 at width/2 from shell
        val halfWidth = width * 0.5f
        val brightness = if (shellDist < halfWidth) {
            1f - (shellDist / halfWidth)
        } else {
            0f
        }

        return color * brightness
    }

    companion object {
        const val ID = "radial-pulse-3d"
    }
}

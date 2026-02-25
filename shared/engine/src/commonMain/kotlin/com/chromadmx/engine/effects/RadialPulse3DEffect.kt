package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.MathUtils
import com.chromadmx.engine.effect.SpatialEffect
import com.chromadmx.engine.util.ColorUtils
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
 * - `centerY`  (Float)       — center Y. Default: 0.
 * - `centerZ`  (Float)       — center Z. Default: 0.
 * - `speed`    (Float)       — expansion speed in units/sec. Default: 2.0.
 * - `color`    (Color)       — pulse color (if palette is empty). Default: [Color.WHITE].
 * - `palette`  (List<Color>) — optional palette to map across the pulse radius.
 * - `width`    (Float)       — shell thickness. Default: 0.3.
 * - `beatSync` (Boolean)     — if true, radius is driven by beat phase. Default: false.
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
        val width = params.getFloat("width", 0.3f).coerceAtLeast(0.001f)
        val beatSync = params.getBoolean("beatSync", false)

        val dist = pos.distanceTo(center)
        val radius = if (beatSync) {
            beat.beatPhase * speed // Use speed as max radius or scale
        } else {
            time * speed
        }

        // Distance of fixture from the expanding shell
        val shellDist = abs(dist - radius)

        // Smooth falloff: 1 at shell center, 0 at width/2 from shell
        val halfWidth = width * 0.5f
        val brightness = if (shellDist < halfWidth) {
            1f - (shellDist / halfWidth)
        } else {
            0f
        }

        val palette = params.getColorList("palette", emptyList())
        val baseColor = if (palette.isNotEmpty()) {
            ColorUtils.samplePalette(palette, (radius % 1.0f))
        } else {
            params.getColor("color", Color.WHITE)
        }

        return baseColor * brightness
    }

    companion object {
        const val ID = "radial-pulse-3d"
    }
}

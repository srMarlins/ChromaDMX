package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.SpatialEffect
import kotlin.math.PI
import kotlin.math.sin

/**
 * Sinusoidal wave oscillating along a spatial axis, producing a smooth
 * color transition between two colors.
 *
 * **Params:**
 * - `axis`       (String)      — "x", "y", or "z". Default: "x".
 * - `wavelength` (Float)       — spatial wavelength in units. Default: 1.0.
 * - `speed`      (Float)       — wave speed in units/sec. Default: 1.0.
 * - `colors`     (List<Color>) — two colors to oscillate between. Default: [BLACK, WHITE].
 */
class WaveEffect3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Wave 3D"

    override fun compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
        val axis = params.getString("axis", "x")
        val wavelength = params.getFloat("wavelength", 1.0f).coerceAtLeast(0.001f)
        val speed = params.getFloat("speed", 1.0f)
        val colors = params.getColorList("colors", DEFAULT_COLORS)

        val colorA = colors.getOrElse(0) { Color.BLACK }
        val colorB = colors.getOrElse(1) { Color.WHITE }

        val axisValue = when (axis) {
            "y" -> pos.y
            "z" -> pos.z
            else -> pos.x
        }

        // Sinusoidal wave: sin(2*pi*(pos/wavelength - time*speed))
        // Remap from -1..1 to 0..1 for interpolation
        val phase = (2.0 * PI * (axisValue / wavelength - time * speed)).toFloat()
        val t = (sin(phase) + 1f) * 0.5f

        return colorA.lerp(colorB, t)
    }

    companion object {
        const val ID = "wave-3d"
        val DEFAULT_COLORS = listOf(Color.BLACK, Color.WHITE)
    }
}

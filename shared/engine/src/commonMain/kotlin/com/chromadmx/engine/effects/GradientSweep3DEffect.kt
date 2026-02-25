package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.MathUtils
import com.chromadmx.engine.effect.SpatialEffect
import com.chromadmx.engine.util.ColorUtils

/**
 * A color gradient that sweeps through 3D space along a chosen axis,
 * scrolling over time.
 *
 * **Params:**
 * - `axis`     (String)      — "x", "y", or "z". Default: "x".
 * - `speed`    (Float)       — scroll speed in units/sec. Default: 1.0.
 * - `palette`  (List<Color>) — gradient palette (>=2 colors). Default: red->blue.
 * - `beatSync` (Boolean)     — if true, scroll is driven by beat phase. Default: false.
 */
class GradientSweep3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Gradient Sweep 3D"

    override fun compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
        val axis = params.getString("axis", "x")
        val speed = params.getFloat("speed", 1.0f)
        val palette = params.getColorList("palette", DEFAULT_PALETTE)
        val beatSync = params.getBoolean("beatSync", false)

        val axisValue = when (axis) {
            "y" -> pos.y
            "z" -> pos.z
            else -> pos.x
        }

        // Scrolling normalized position (wraps 0..1)
        val t = if (beatSync) {
            MathUtils.wrap(axisValue + beat.beatPhase * speed, 1f)
        } else {
            MathUtils.wrap(axisValue + time * speed, 1f)
        }

        return ColorUtils.samplePalette(palette, t)
    }

    companion object {
        const val ID = "gradient-sweep-3d"
        val DEFAULT_PALETTE = listOf(Color.RED, Color.BLUE)
    }
}

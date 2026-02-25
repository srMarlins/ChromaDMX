package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.MathUtils
import com.chromadmx.engine.effect.SpatialEffect
import com.chromadmx.engine.util.ColorUtils

/**
 * Full HSV rainbow sweeping through 3D space along an axis.
 *
 * **Params:**
 * - `axis`   (String) — "x", "y", or "z". Default: "x".
 * - `speed`  (Float)  — scroll speed in units/sec. Default: 1.0.
 * - `spread` (Float)  — how many hue cycles fit in one unit. Default: 1.0.
 */
class RainbowSweep3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Rainbow Sweep 3D"

    private data class Context(
        val axis: String,
        val spread: Float,
        val timeOffset: Float
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val axis = params.getString("axis", "x")
        val speed = params.getFloat("speed", 1.0f)
        val spread = params.getFloat("spread", 1.0f)

        // Pre-calculate time offset: time * speed
        val timeOffset = time * speed

        return Context(axis, spread, timeOffset)
    }

    override fun compute(pos: Vec3, context: Any?): Color {
        val ctx = context as? Context ?: return Color.BLACK

        val axisValue = when (ctx.axis) {
            "y" -> pos.y
            "z" -> pos.z
            else -> pos.x
        }

        // Hue cycles through 0-360 based on position and time
        val hue = MathUtils.wrap((axisValue * ctx.spread + ctx.timeOffset) * 360f, 360f)

        return ColorUtils.hsvToRgb(hue, 1.0f, 1.0f)
    }

    companion object {
        const val ID = "rainbow-sweep-3d"
    }
}

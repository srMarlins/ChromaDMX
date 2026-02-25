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
 * - `axis`    (String)      — "x", "y", or "z". Default: "x".
 * - `speed`   (Float)       — scroll speed in units/sec. Default: 1.0.
 * - `palette` (List<Color>) — gradient palette (>=2 colors). Default: red->blue.
 */
class GradientSweep3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Gradient Sweep 3D"

    private data class Context(
        val axis: String,
        val speed: Float,
        val palette: List<Color>,
        val time: Float
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        return Context(
            axis = params.getString("axis", "x"),
            speed = params.getFloat("speed", 1.0f),
            palette = params.getColorList("palette", DEFAULT_PALETTE),
            time = time
        )
    }

    override fun compute(pos: Vec3, context: Any?): Color {
        val ctx = context as? Context ?: return Color.BLACK

        val axisValue = when (ctx.axis) {
            "y" -> pos.y
            "z" -> pos.z
            else -> pos.x
        }

        // Scrolling normalized position (wraps 0..1)
        val t = MathUtils.wrap(axisValue + ctx.time * ctx.speed, 1f)

        return ColorUtils.samplePalette(ctx.palette, t)
    }

    companion object {
        const val ID = "gradient-sweep-3d"
        val DEFAULT_PALETTE = listOf(Color.RED, Color.BLUE)
    }
}

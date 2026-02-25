package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.MathUtils
import com.chromadmx.engine.effect.SpatialEffect
import kotlin.math.abs
import kotlin.math.max

/**
 * Sequential light chase through fixtures sorted by a spatial axis.
 *
 * A "head" position scrolls along the chosen axis at [speed] units/sec.
 * Fixtures near the head light up in [color], fading away over a [tail]
 * length behind the head.
 *
 * **Params:**
 * - `axis`  (String) — "x", "y", or "z". Default: "x".
 * - `speed` (Float)  — head speed in units/sec. Default: 2.0.
 * - `color` (Color)  — chase color. Default: [Color.WHITE].
 * - `tail`  (Float)  — fade-out length behind the head. Default: 0.5.
 */
class Chase3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Chase 3D"

    private data class Context(
        val axis: String,
        val speed: Float,
        val color: Color,
        val tail: Float,
        val time: Float
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        return Context(
            axis = params.getString("axis", "x"),
            speed = params.getFloat("speed", 2.0f),
            color = params.getColor("color", Color.WHITE),
            tail = params.getFloat("tail", 0.5f).coerceAtLeast(0.001f),
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

        // Head position wraps through 0..1
        val headPos = MathUtils.wrap(ctx.time * ctx.speed, 1f)

        // Distance from head (wrapping aware)
        val rawDist = axisValue - headPos
        // We want the distance "behind" the head, so the head leads
        val wrappedDist = MathUtils.wrap(rawDist, 1f)

        // Fixtures within the tail fade from full brightness to zero
        val brightness = if (wrappedDist <= ctx.tail) {
            1f - (wrappedDist / ctx.tail)
        } else {
            0f
        }

        return ctx.color * brightness
    }

    companion object {
        const val ID = "chase-3d"
    }
}

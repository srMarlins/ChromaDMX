package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.MovementEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular pan/tilt motion.
 *
 * All fixtures trace a circle in pan/tilt space, with optional per-fixture
 * phase offset based on spatial position.
 *
 * **Params:**
 * - `radius`     (Float)  — circle radius in normalized units, 0.0-0.5. Default: 0.25.
 * - `speed`      (Float)  — rotations per second. Default: 1.0.
 * - `direction`  (String) — "cw" or "ccw". Default: "cw".
 * - `centerPan`  (Float)  — center pan position, 0.0-1.0. Default: 0.5.
 * - `centerTilt` (Float)  — center tilt position, 0.0-1.0. Default: 0.5.
 * - `phaseOffset`(Float)  — per-fixture phase offset based on x position. Default: 0.0.
 */
class CircleMovementEffect : MovementEffect {

    override val id: String = ID
    override val name: String = "Circle Movement"

    private data class Context(
        val radius: Float,
        val timeAngle: Float,
        val directionSign: Float,
        val centerPan: Float,
        val centerTilt: Float,
        val phaseOffset: Float
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val radius = params.getFloat("radius", 0.25f).coerceIn(0f, 0.5f)
        val speed = params.getFloat("speed", 1.0f)
        val direction = params.getString("direction", "cw")
        val centerPan = params.getFloat("centerPan", 0.5f).coerceIn(0f, 1f)
        val centerTilt = params.getFloat("centerTilt", 0.5f).coerceIn(0f, 1f)
        val phaseOffset = params.getFloat("phaseOffset", 0f)

        val directionSign = if (direction == "ccw") -1f else 1f
        val timeAngle = (time * speed * 2.0 * PI).toFloat()

        return Context(radius, timeAngle, directionSign, centerPan, centerTilt, phaseOffset)
    }

    override fun computeMovement(pos: Vec3, context: Any?): FixtureOutput {
        val ctx = context as? Context ?: return FixtureOutput.DEFAULT

        val spatialPhase = pos.x * ctx.phaseOffset * (2.0 * PI).toFloat()
        val angle = (ctx.timeAngle + spatialPhase) * ctx.directionSign

        val pan = (ctx.centerPan + cos(angle.toDouble()).toFloat() * ctx.radius).coerceIn(0f, 1f)
        val tilt = (ctx.centerTilt + sin(angle.toDouble()).toFloat() * ctx.radius).coerceIn(0f, 1f)

        return FixtureOutput(pan = pan, tilt = tilt)
    }

    companion object {
        const val ID = "circle-movement"
    }
}

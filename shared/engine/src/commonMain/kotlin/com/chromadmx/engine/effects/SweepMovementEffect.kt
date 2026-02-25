package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.MovementEffect
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pan/tilt sweep synchronized to time.
 *
 * Produces sinusoidal sweeps on pan, tilt, or both axes.
 * The sweep oscillates within a configurable range around a center point.
 *
 * **Params:**
 * - `axis`      (String) — "pan", "tilt", or "both". Default: "both".
 * - `range`     (Float)  — amplitude of sweep, 0.0-1.0. Default: 0.5.
 * - `speed`     (Float)  — oscillation speed in Hz. Default: 1.0.
 * - `centerPan` (Float)  — center pan position, 0.0-1.0. Default: 0.5.
 * - `centerTilt`(Float)  — center tilt position, 0.0-1.0. Default: 0.5.
 * - `phaseOffset` (Float) — spatial phase offset per unit of position.x. Default: 0.0.
 */
class SweepMovementEffect : MovementEffect {

    override val id: String = ID
    override val name: String = "Sweep Movement"

    private data class Context(
        val axis: String,
        val range: Float,
        val timePhase: Float,
        val centerPan: Float,
        val centerTilt: Float,
        val phaseOffset: Float
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val axis = params.getString("axis", "both")
        val range = params.getFloat("range", 0.5f).coerceIn(0f, 1f)
        val speed = params.getFloat("speed", 1.0f)
        val centerPan = params.getFloat("centerPan", 0.5f).coerceIn(0f, 1f)
        val centerTilt = params.getFloat("centerTilt", 0.5f).coerceIn(0f, 1f)
        val phaseOffset = params.getFloat("phaseOffset", 0f)

        val timePhase = (time * speed * 2.0 * PI).toFloat()

        return Context(axis, range, timePhase, centerPan, centerTilt, phaseOffset)
    }

    override fun computeMovement(pos: Vec3, context: Any?): FixtureOutput {
        val ctx = context as? Context ?: return FixtureOutput.DEFAULT

        val spatialPhase = pos.x * ctx.phaseOffset * (2.0 * PI).toFloat()
        val sweep = sin((ctx.timePhase + spatialPhase).toDouble()).toFloat()

        val pan = when (ctx.axis) {
            "pan", "both" -> (ctx.centerPan + sweep * ctx.range * 0.5f).coerceIn(0f, 1f)
            else -> null
        }

        val tilt = when (ctx.axis) {
            "tilt" -> (ctx.centerTilt + sweep * ctx.range * 0.5f).coerceIn(0f, 1f)
            "both" -> {
                // For "both", offset tilt by 90 degrees (cos) for a figure-8-like motion
                val tiltSweep = sin(ctx.timePhase.toDouble() + spatialPhase.toDouble() + PI * 0.5).toFloat()
                (ctx.centerTilt + tiltSweep * ctx.range * 0.5f).coerceIn(0f, 1f)
            }
            else -> null
        }

        return FixtureOutput(pan = pan, tilt = tilt)
    }

    companion object {
        const val ID = "sweep-movement"
    }
}

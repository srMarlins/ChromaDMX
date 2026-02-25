package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.MovementEffect
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * All heads point at a target 3D point.
 *
 * Computes the pan and tilt angles from each fixture's position to a
 * given target coordinate, then normalizes them to the 0.0-1.0 range.
 *
 * The coordinate system convention:
 * - Pan (horizontal): computed from the XZ plane angle. 0.5 = forward (−Z), 0.0 = left, 1.0 = right.
 * - Tilt (vertical): computed from elevation. 0.5 = horizontal, 0.0 = straight down, 1.0 = straight up.
 *
 * **Params:**
 * - `targetX` (Float) — target X coordinate. Default: 0.0.
 * - `targetY` (Float) — target Y coordinate. Default: 0.0.
 * - `targetZ` (Float) — target Z coordinate. Default: 0.0.
 */
class FollowEffect : MovementEffect {

    override val id: String = ID
    override val name: String = "Follow Target"

    private data class Context(
        val target: Vec3
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val targetX = params.getFloat("targetX", 0f)
        val targetY = params.getFloat("targetY", 0f)
        val targetZ = params.getFloat("targetZ", 0f)
        return Context(Vec3(targetX, targetY, targetZ))
    }

    override fun computeMovement(pos: Vec3, context: Any?): FixtureOutput {
        val ctx = context as? Context ?: return FixtureOutput.DEFAULT

        val delta = ctx.target - pos
        val horizontalDist = sqrt(delta.x * delta.x + delta.z * delta.z)

        // Pan: angle in the XZ plane
        // atan2(x, -z) gives 0 when pointing forward (-Z), positive when right
        // Normalize from [-PI, PI] to [0, 1]
        val panAngle = atan2(delta.x.toDouble(), (-delta.z).toDouble()).toFloat()
        val pan = ((panAngle / PI.toFloat()) * 0.5f + 0.5f).coerceIn(0f, 1f)

        // Tilt: elevation angle
        // atan2(y, horizontal) gives the elevation
        // Normalize from [-PI/2, PI/2] to [0, 1] where 0.5 = horizontal
        val tiltAngle = atan2(delta.y.toDouble(), horizontalDist.toDouble()).toFloat()
        val tilt = ((tiltAngle / (PI.toFloat() * 0.5f)) * 0.5f + 0.5f).coerceIn(0f, 1f)

        return FixtureOutput(pan = pan, tilt = tilt)
    }

    companion object {
        const val ID = "follow-target"
    }
}

package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.MovementEffect

/**
 * Random position jumps on beat.
 *
 * Each beat, a new random pan/tilt position is computed using a deterministic
 * hash of the beat count. All fixtures jump to the same position unless
 * `perFixture` is true, in which case each fixture gets a different position
 * based on its spatial coordinates.
 *
 * **Params:**
 * - `rangePan`   (Float)   -- amplitude of pan range, 0.0-1.0. Default: 0.5.
 * - `rangeTilt`  (Float)   -- amplitude of tilt range, 0.0-1.0. Default: 0.5.
 * - `centerPan`  (Float)   -- center pan position, 0.0-1.0. Default: 0.5.
 * - `centerTilt` (Float)   -- center tilt position, 0.0-1.0. Default: 0.5.
 * - `perFixture` (Boolean) -- different random position per fixture. Default: false.
 */
class RandomMovementEffect : MovementEffect {

    override val id: String = ID
    override val name: String = "Random Movement"

    private data class Context(
        val rangePan: Float,
        val rangeTilt: Float,
        val centerPan: Float,
        val centerTilt: Float,
        val perFixture: Boolean,
        val beatIndex: Int
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val rangePan = params.getFloat("rangePan", 0.5f).coerceIn(0f, 1f)
        val rangeTilt = params.getFloat("rangeTilt", 0.5f).coerceIn(0f, 1f)
        val centerPan = params.getFloat("centerPan", 0.5f).coerceIn(0f, 1f)
        val centerTilt = params.getFloat("centerTilt", 0.5f).coerceIn(0f, 1f)
        val perFixture = params.getBoolean("perFixture", false)

        // Count total beats elapsed
        val beatsPerSecond = beat.bpm / 60f
        val beatIndex = (beat.elapsed * beatsPerSecond).toInt()

        return Context(rangePan, rangeTilt, centerPan, centerTilt, perFixture, beatIndex)
    }

    override fun computeMovement(pos: Vec3, context: Any?): FixtureOutput {
        val ctx = context as? Context ?: return FixtureOutput.DEFAULT

        val seed = if (ctx.perFixture) {
            // Mix in position for per-fixture variation
            hash(ctx.beatIndex * 73856093 xor
                (pos.x * 19349663).toInt() xor
                (pos.y * 83492791).toInt() xor
                (pos.z * 41729501).toInt())
        } else {
            hash(ctx.beatIndex)
        }

        // Generate two pseudo-random floats in [-0.5, 0.5] from the seed
        val panRand = ((seed and 0xFFFF) / 65535f) - 0.5f
        val tiltRand = (((seed ushr 16) and 0xFFFF) / 65535f) - 0.5f

        val pan = (ctx.centerPan + panRand * ctx.rangePan).coerceIn(0f, 1f)
        val tilt = (ctx.centerTilt + tiltRand * ctx.rangeTilt).coerceIn(0f, 1f)

        return FixtureOutput(pan = pan, tilt = tilt)
    }

    companion object {
        const val ID = "random-movement"

        /**
         * Simple integer hash for deterministic pseudo-randomness.
         * Based on a variant of the Wang hash.
         */
        internal fun hash(input: Int): Int {
            var x = input
            x = ((x ushr 16) xor x) * 0x45d9f3b
            x = ((x ushr 16) xor x) * 0x45d9f3b
            x = (x ushr 16) xor x
            return x
        }
    }
}

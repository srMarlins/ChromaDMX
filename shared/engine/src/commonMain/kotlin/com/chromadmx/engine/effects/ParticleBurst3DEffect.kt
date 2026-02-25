package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.SpatialEffect
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Particles exploding outward from a center point.
 *
 * A fixed set of [count] particles radiate outward at [speed] from [center].
 * Each particle illuminates nearby fixtures, fading as it travels.
 * Particle directions are deterministic (seeded from their index).
 *
 * **Params:**
 * - `centerX` (Float) — center X. Default: 0.
 * - `centerY` (Float) — center Y. Default: 0.
 * - `centerZ` (Float) — center Z. Default: 0.
 * - `speed`   (Float) — expansion speed. Default: 3.0.
 * - `count`   (Int)   — number of particles. Default: 12.
 * - `fade`    (Float) — distance at which a particle fully fades. Default: 0.5.
 * - `color`   (Color) — particle color. Default: [Color.WHITE].
 */
class ParticleBurst3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Particle Burst 3D"

    override fun compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
        val center = Vec3(
            params.getFloat("centerX", 0f),
            params.getFloat("centerY", 0f),
            params.getFloat("centerZ", 0f)
        )
        val speed = params.getFloat("speed", 3.0f)
        val count = params.getInt("count", 12).coerceIn(1, 256)
        val fade = params.getFloat("fade", 0.5f).coerceAtLeast(0.001f)
        val color = params.getColor("color", Color.WHITE)

        var brightness = 0f

        for (i in 0 until count) {
            // Deterministic direction using golden-angle spherical distribution
            val dir = particleDirection(i, count)

            // Particle position at current time
            val particlePos = center + dir * (time * speed)

            // Distance from this fixture to the particle
            val dist = pos.distanceTo(particlePos)

            // Brightness contribution (inverse distance with fade)
            if (dist < fade) {
                val contribution = (1f - dist / fade)
                // Life fading: particle dims as it travels further from center
                val life = max(0f, 1f - (time * speed) / 5f)
                brightness += contribution * life
            }
        }

        brightness = brightness.coerceIn(0f, 1f)
        return color * brightness
    }

    companion object {
        const val ID = "particle-burst-3d"

        /** Golden angle in radians for spherical distribution. */
        private const val GOLDEN_ANGLE = 2.399963f // PI * (3 - sqrt(5))

        /**
         * Deterministic particle direction using Fibonacci sphere distribution.
         * Produces evenly-spaced directions on a unit sphere.
         */
        fun particleDirection(index: Int, total: Int): Vec3 {
            // y goes from ~1 to ~-1
            val y = 1f - (2f * index.toFloat()) / (total - 1).coerceAtLeast(1).toFloat()
            val radiusAtY = sqrt(max(0f, 1f - y * y))
            val theta = GOLDEN_ANGLE * index
            val x = cos(theta) * radiusAtY
            val z = sin(theta) * radiusAtY
            return Vec3(x, y, z)
        }
    }
}

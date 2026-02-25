package com.chromadmx.engine.effect

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3

/**
 * A spatial effect computes a color for any point in 3D space at a given
 * moment in time.  Effects are pure functions — the same inputs always
 * produce the same output — which makes them deterministic, testable,
 * and thread-safe.
 */
interface SpatialEffect {

    /** Unique identifier used for registry look-ups and serialization. */
    val id: String

    /** Human-readable display name. */
    val name: String

    /**
     * Compute the color contribution of this effect at [pos] in 3D space.
     *
     * @param pos    Fixture position in venue space.
     * @param time   Seconds elapsed since the engine was started.
     * @param beat   Current musical timing snapshot.
     * @param params Per-effect parameters (colors, speeds, axes, etc.).
     * @return The computed [Color] for this position/time.
     */
    fun compute(
        pos: Vec3,
        time: Float,
        beat: BeatState,
        params: EffectParams
    ): Color
}

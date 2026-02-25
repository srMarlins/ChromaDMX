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
     * Prepare data for the current frame.
     * This is called once per frame (or update tick) for each active layer.
     * Use this to pre-calculate values, extract parameters, or set up look-up tables
     * that are constant for the entire frame but might depend on time or parameters.
     *
     * @param params Per-effect parameters.
     * @param time   Seconds elapsed since the engine was started.
     * @param beat   Current musical timing snapshot.
     * @return An opaque context object passed to [compute] for every pixel.
     */
    fun prepare(params: EffectParams, time: Float, beat: BeatState): Any? = null

    /**
     * Compute the color contribution of this effect at [pos] in 3D space.
     *
     * @param pos     Fixture position in venue space.
     * @param context The context object returned by [prepare].
     * @return The computed [Color] for this position.
     */
    fun compute(
        pos: Vec3,
        context: Any?
    ): Color
}

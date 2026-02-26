package com.chromadmx.engine.effect

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3

/**
 * A movement effect computes non-color fixture output (pan, tilt, gobo, etc.)
 * for any point in 3D space at a given moment in time.
 *
 * Like [SpatialEffect], movement effects use a prepare/compute split:
 * [prepare] is called once per frame, and [computeMovement] is called per fixture.
 *
 * Movement effects return [FixtureOutput] which may contain any combination
 * of movement values. Null fields mean "no opinion" and will be ignored
 * during compositing.
 */
interface MovementEffect {

    /** Unique identifier used for registry look-ups and serialization. */
    val id: String

    /** Human-readable display name. */
    val name: String

    /**
     * Prepare data for the current frame.
     * Called once per frame for each active movement layer.
     *
     * @param params Per-effect parameters.
     * @param time   Seconds elapsed since the engine was started.
     * @param beat   Current musical timing snapshot.
     * @return An opaque context object passed to [computeMovement] for every fixture.
     */
    fun prepare(params: EffectParams, time: Float, beat: BeatState): Any? = null

    /**
     * Compute the movement contribution of this effect at [pos] in 3D space.
     *
     * @param pos     Fixture position in venue space.
     * @param context The context object returned by [prepare].
     * @return The computed [FixtureOutput] for this position (color is typically BLACK / default).
     */
    fun computeMovement(pos: Vec3, context: Any?): FixtureOutput
}

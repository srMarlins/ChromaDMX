package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.MovementEffect

/**
 * Switch gobo slot synchronized to the beat.
 *
 * Cycles through gobo wheel slots, advancing to the next slot on each beat
 * (when beatPhase crosses zero). The slot is position-independent — all
 * fixtures show the same gobo at any given moment.
 *
 * **Params:**
 * - `slotCount`    (Int)     — number of gobo slots available. Default: 8.
 * - `changeOnBeat` (Boolean) — when true, slot changes on each beat. Default: true.
 *                              When false, slot changes on each bar.
 */
class GoboBeatSyncEffect : MovementEffect {

    override val id: String = ID
    override val name: String = "Gobo Beat Sync"

    private data class Context(
        val goboSlot: Int
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val slotCount = params.getInt("slotCount", 8).coerceAtLeast(1)
        val changeOnBeat = params.getBoolean("changeOnBeat", true)

        // Determine which slot based on beat or bar phase progression.
        // Use elapsed time and BPM to count total beats, then modulo slotCount.
        val beatsPerSecond = beat.bpm / 60f
        val totalBeats = (beat.elapsed * beatsPerSecond).toInt()

        val slot = if (changeOnBeat) {
            totalBeats % slotCount
        } else {
            // Change on bar (every 4 beats)
            (totalBeats / 4) % slotCount
        }

        return Context(goboSlot = slot)
    }

    override fun computeMovement(pos: Vec3, context: Any?): FixtureOutput {
        val ctx = context as? Context ?: return FixtureOutput.DEFAULT
        return FixtureOutput(gobo = ctx.goboSlot)
    }

    companion object {
        const val ID = "gobo-beat-sync"
    }
}

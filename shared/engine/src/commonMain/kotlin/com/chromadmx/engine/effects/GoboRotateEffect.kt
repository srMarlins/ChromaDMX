package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.MovementEffect

/**
 * Cycle through gobo wheel slots at a configurable speed.
 *
 * Unlike [GoboBeatSyncEffect], this cycles based on elapsed time rather than
 * beat synchronization, providing a smooth, continuous rotation through slots.
 *
 * **Params:**
 * - `slotCount`  (Int)   -- number of gobo slots available. Default: 8.
 * - `speed`      (Float) -- rotations through the full wheel per second. Default: 0.5.
 * - `startSlot`  (Int)   -- first slot index to begin cycling from. Default: 0.
 */
class GoboRotateEffect : MovementEffect {

    override val id: String = ID
    override val name: String = "Gobo Rotate"

    private data class Context(
        val goboSlot: Int
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val slotCount = params.getInt("slotCount", 8).coerceAtLeast(1)
        val speed = params.getFloat("speed", 0.5f)
        val startSlot = params.getInt("startSlot", 0).coerceIn(0, slotCount - 1)

        // Continuous rotation: time * speed gives fraction of full rotations
        val totalSlotPhase = time * speed * slotCount
        val slot = (startSlot + totalSlotPhase.toInt()) % slotCount

        return Context(goboSlot = slot.coerceIn(0, slotCount - 1))
    }

    override fun computeMovement(pos: Vec3, context: Any?): FixtureOutput {
        val ctx = context as? Context ?: return FixtureOutput.DEFAULT
        return FixtureOutput(gobo = ctx.goboSlot)
    }

    companion object {
        const val ID = "gobo-rotate"
    }
}

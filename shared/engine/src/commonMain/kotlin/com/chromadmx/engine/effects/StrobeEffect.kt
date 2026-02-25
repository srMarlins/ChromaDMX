package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.SpatialEffect

/**
 * On/off flash synced to the beat clock.
 *
 * The strobe is ON when the beat phase is below the duty cycle threshold,
 * and OFF otherwise. All fixtures flash in unison (position-independent).
 *
 * **Params:**
 * - `color`     (Color) — flash color. Default: [Color.WHITE].
 * - `dutyCycle` (Float) — fraction of the beat that is ON, 0.0-1.0. Default: 0.5.
 */
class StrobeEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Strobe"

    override fun compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
        val color = params.getColor("color", Color.WHITE)
        val dutyCycle = params.getFloat("dutyCycle", 0.5f).coerceIn(0f, 1f)

        return if (beat.beatPhase < dutyCycle) color else Color.BLACK
    }

    companion object {
        const val ID = "strobe"
    }
}

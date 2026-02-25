package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.SpatialEffect

/**
 * The simplest possible effect: every fixture gets the same color.
 *
 * **Params:**
 * - `color` (Color) — the output color. Default: [Color.WHITE].
 */
class SolidColorEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Solid Color"

    override fun compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color {
        return params.getColor("color", Color.WHITE)
    }

    companion object {
        const val ID = "solid-color"
    }
}

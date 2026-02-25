package com.chromadmx.engine.effect

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BlendMode

/**
 * One layer in the [EffectStack].
 *
 * Each layer wraps a [SpatialEffect] together with the blending parameters
 * that control how it composites onto the layers below it.
 *
 * @property effect    The spatial effect to evaluate.
 * @property params    Parameters passed to [effect] on each evaluation.
 * @property blendMode How this layer composites onto the stack below.
 * @property opacity   Layer opacity, 0.0 (invisible) to 1.0 (fully opaque).
 * @property enabled   When false the layer is skipped entirely.
 */
data class EffectLayer(
    val effect: SpatialEffect,
    val params: EffectParams = EffectParams.EMPTY,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1.0f,
    val enabled: Boolean = true
)

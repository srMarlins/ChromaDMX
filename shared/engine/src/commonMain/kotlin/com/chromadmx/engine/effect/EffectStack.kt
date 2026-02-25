package com.chromadmx.engine.effect

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.ColorBlending

/**
 * A compositing stack of [EffectLayer]s with a master dimmer.
 *
 * Evaluation walks the layers bottom-to-top (index 0 is the bottom),
 * computing each enabled layer's effect, blending its output onto the
 * running result using the layer's blend mode and opacity, and finally
 * multiplying by [masterDimmer].
 */
class EffectStack(
    layers: List<EffectLayer> = emptyList(),
    masterDimmer: Float = 1.0f
) {
    private val _layers = layers.toMutableList()

    /** Current ordered list of layers (bottom-to-top). */
    val layers: List<EffectLayer> get() = _layers.toList()

    /** Master dimmer applied after all layer compositing, 0.0-1.0. */
    var masterDimmer: Float = masterDimmer.coerceIn(0f, 1f)
        set(value) { field = value.coerceIn(0f, 1f) }

    /* ------------------------------------------------------------------ */
    /*  Layer manipulation                                                 */
    /* ------------------------------------------------------------------ */

    fun addLayer(layer: EffectLayer) {
        _layers.add(layer)
    }

    fun removeLayerAt(index: Int) {
        _layers.removeAt(index)
    }

    fun setLayer(index: Int, layer: EffectLayer) {
        _layers[index] = layer
    }

    fun clearLayers() {
        _layers.clear()
    }

    val layerCount: Int get() = _layers.size

    /* ------------------------------------------------------------------ */
    /*  Evaluation                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Evaluate the full stack for a single 3D position.
     *
     * @return The final composited and dimmed color.
     */
    fun evaluate(pos: Vec3, time: Float, beat: BeatState): Color {
        var result = Color.BLACK

        for (layer in _layers) {
            if (!layer.enabled) continue

            val layerColor = layer.effect.compute(pos, time, beat, layer.params)
            result = ColorBlending.blend(
                base = result,
                overlay = layerColor,
                mode = layer.blendMode,
                opacity = layer.opacity
            )
        }

        // Apply master dimmer
        return (result * masterDimmer).clamped()
    }
}

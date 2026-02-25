package com.chromadmx.engine.effect

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.ColorBlending
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * A compositing stack of [EffectLayer]s with a master dimmer.
 *
 * Evaluation walks the layers bottom-to-top (index 0 is the bottom),
 * computing each enabled layer's effect, blending its output onto the
 * running result using the layer's blend mode and opacity, and finally
 * multiplying by [masterDimmer].
 *
 * Thread safety: mutations use copy-on-write on an atomic snapshot,
 * synchronized via [lock]. The hot-path [evaluate] reads the atomic
 * snapshot without locking, so it is wait-free for the 60 fps engine loop.
 */
class EffectStack(
    layers: List<EffectLayer> = emptyList(),
    masterDimmer: Float = 1.0f
) : SynchronizedObject() {
    private val lock = this // use self as SynchronizedObject lock

    private val _layersRef = atomic(layers.toList())

    /** Current ordered list of layers (bottom-to-top). Returns an immutable snapshot. */
    val layers: List<EffectLayer> get() = _layersRef.value

    private val _masterDimmer = atomic(masterDimmer.coerceIn(0f, 1f))

    /** Master dimmer applied after all layer compositing, 0.0-1.0. */
    var masterDimmer: Float
        get() = _masterDimmer.value
        set(value) { _masterDimmer.value = value.coerceIn(0f, 1f) }

    /* ------------------------------------------------------------------ */
    /*  Layer manipulation (copy-on-write, synchronized)                   */
    /* ------------------------------------------------------------------ */

    fun addLayer(layer: EffectLayer) {
        synchronized(lock) {
            _layersRef.value = _layersRef.value + layer
        }
    }

    fun removeLayerAt(index: Int) {
        synchronized(lock) {
            _layersRef.value = _layersRef.value.toMutableList().apply { removeAt(index) }
        }
    }

    fun setLayer(index: Int, layer: EffectLayer) {
        synchronized(lock) {
            _layersRef.value = _layersRef.value.toMutableList().apply { this[index] = layer }
        }
    }

    fun moveLayer(fromIndex: Int, toIndex: Int) {
        synchronized(lock) {
            val list = _layersRef.value.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) return
            val layer = list.removeAt(fromIndex)
            list.add(toIndex, layer)
            _layersRef.value = list
        }
    }

    fun clearLayers() {
        synchronized(lock) {
            _layersRef.value = emptyList()
        }
    }

    /**
     * Atomically replace all layers. Use this instead of clearLayers() + addLayer()
     * sequences to avoid intermediate states visible to the engine loop.
     */
    fun replaceLayers(newLayers: List<EffectLayer>) {
        synchronized(lock) {
            _layersRef.value = newLayers.toList()
        }
    }

    val layerCount: Int get() = _layersRef.value.size

    /* ------------------------------------------------------------------ */
    /*  Evaluation                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Prepares the entire stack for evaluation for a given frame.
     * This captures the current layer snapshot and prepares each effect.
     * The returned [FrameEvaluator] can then be used to compute colors for
     * multiple positions very efficiently.
     */
    fun buildFrame(time: Float, beat: BeatState): FrameEvaluator {
        val snapshot = _layersRef.value
        val contexts = snapshot.map { layer ->
            if (layer.enabled) layer.effect.prepare(layer.params, time, beat) else null
        }
        return FrameEvaluator(snapshot, contexts, _masterDimmer.value)
    }

    /**
     * Evaluator for a single frame. Contains pre-calculated contexts for all layers.
     */
    class FrameEvaluator(
        private val layers: List<EffectLayer>,
        private val contexts: List<Any?>,
        private val masterDimmer: Float
    ) {
        fun evaluate(pos: Vec3): Color {
            var result = Color.BLACK

            // Using indices loop to avoid iterator allocation
            for (i in layers.indices) {
                val layer = layers[i]
                if (!layer.enabled) continue

                val layerColor = layer.effect.compute(pos, contexts[i])
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

    /**
     * Evaluate the full stack for a single 3D position.
     *
     * Reads the atomic layer snapshot without locking (wait-free).
     *
     * Note: For bulk evaluation (e.g. all fixtures), prefer [buildFrame] and
     * then [FrameEvaluator.evaluate] inside the loop to avoid redundant preparation.
     *
     * @return The final composited and dimmed color.
     */
    fun evaluate(pos: Vec3, time: Float, beat: BeatState): Color {
        return buildFrame(time, beat).evaluate(pos)
    }
}

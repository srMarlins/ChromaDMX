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
     * Evaluate the full stack for a single 3D position.
     *
     * Reads the atomic layer snapshot without locking (wait-free).
     *
     * @param pos     Fixture position in 3D space.
     * @param time    Current engine time in seconds.
     * @param beat    Current beat/tempo state.
     * @param groupId Optional ID of the fixture's group for filtering layers.
     * @return The final composited and dimmed color.
     */
    fun evaluate(
        pos: Vec3,
        time: Float,
        beat: BeatState,
        groupId: String? = null
    ): Color {
        val snapshot = _layersRef.value  // single atomic read
        var result = Color.BLACK

        for (layer in snapshot) {
            if (!layer.enabled) continue

            // Filter by group if targetGroupId is set
            if (layer.targetGroupId != null && layer.targetGroupId != groupId) continue

            val layerColor = layer.effect.compute(pos, time, beat, layer.params)
            result = ColorBlending.blend(
                base = result,
                overlay = layerColor,
                mode = layer.blendMode,
                opacity = layer.opacity
            )
        }

        // Apply master dimmer
        return (result * _masterDimmer.value).clamped()
    }
}

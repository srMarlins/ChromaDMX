package com.chromadmx.engine.effect

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.util.ColorBlending
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * A compositing stack of [EffectLayer]s and [MovementLayer]s with a master dimmer.
 *
 * Evaluation walks the layers bottom-to-top (index 0 is the bottom),
 * computing each enabled layer's effect, blending its output onto the
 * running result using the layer's blend mode and opacity, and finally
 * multiplying by [masterDimmer].
 *
 * Movement layers are evaluated separately and produce [FixtureOutput] values
 * containing pan, tilt, gobo, focus, zoom, and strobe rate.
 *
 * Thread safety: mutations use copy-on-write on an atomic snapshot,
 * synchronized via [lock]. The hot-path [evaluate] reads the atomic
 * snapshot without locking, so it is wait-free for the 60 fps engine loop.
 */
class EffectStack(
    layers: List<EffectLayer> = emptyList(),
    movementLayers: List<MovementLayer> = emptyList(),
    masterDimmer: Float = 1.0f
) : SynchronizedObject() {
    private val lock = this // use self as SynchronizedObject lock

    private val _layersRef = atomic(layers.toList())
    private val _movementLayersRef = atomic(movementLayers.toList())

    /** Current ordered list of color layers (bottom-to-top). Returns an immutable snapshot. */
    val layers: List<EffectLayer> get() = _layersRef.value

    /** Current ordered list of movement layers (bottom-to-top). Returns an immutable snapshot. */
    val movementLayers: List<MovementLayer> get() = _movementLayersRef.value

    private val _masterDimmer = atomic(masterDimmer.coerceIn(0f, 1f))

    /** Master dimmer applied after all layer compositing, 0.0-1.0. */
    var masterDimmer: Float
        get() = _masterDimmer.value
        set(value) { _masterDimmer.value = value.coerceIn(0f, 1f) }

    /* ------------------------------------------------------------------ */
    /*  Color layer manipulation (copy-on-write, synchronized)             */
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
    /*  Movement layer manipulation (copy-on-write, synchronized)          */
    /* ------------------------------------------------------------------ */

    fun addMovementLayer(layer: MovementLayer) {
        synchronized(lock) {
            _movementLayersRef.value = _movementLayersRef.value + layer
        }
    }

    fun removeMovementLayerAt(index: Int) {
        synchronized(lock) {
            _movementLayersRef.value = _movementLayersRef.value.toMutableList().apply { removeAt(index) }
        }
    }

    fun setMovementLayer(index: Int, layer: MovementLayer) {
        synchronized(lock) {
            _movementLayersRef.value = _movementLayersRef.value.toMutableList().apply { this[index] = layer }
        }
    }

    fun clearMovementLayers() {
        synchronized(lock) {
            _movementLayersRef.value = emptyList()
        }
    }

    fun replaceMovementLayers(newLayers: List<MovementLayer>) {
        synchronized(lock) {
            _movementLayersRef.value = newLayers.toList()
        }
    }

    val movementLayerCount: Int get() = _movementLayersRef.value.size

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
        val colorSnapshot = _layersRef.value
        val colorContexts = colorSnapshot.map { layer ->
            if (layer.enabled) layer.effect.prepare(layer.params, time, beat) else null
        }

        val movementSnapshot = _movementLayersRef.value
        val movementContexts = movementSnapshot.map { layer ->
            if (layer.enabled) layer.effect.prepare(layer.params, time, beat) else null
        }

        return FrameEvaluator(
            colorSnapshot, colorContexts,
            movementSnapshot, movementContexts,
            _masterDimmer.value
        )
    }

    /**
     * Evaluator for a single frame. Contains pre-calculated contexts for all layers.
     */
    class FrameEvaluator(
        private val colorLayers: List<EffectLayer>,
        private val colorContexts: List<Any?>,
        private val movementLayers: List<MovementLayer>,
        private val movementContexts: List<Any?>,
        private val masterDimmer: Float
    ) {
        /**
         * Evaluate the color stack only. Backward-compatible method.
         */
        fun evaluate(pos: Vec3): Color {
            var result = Color.BLACK

            // Using indices loop to avoid iterator allocation
            for (i in colorLayers.indices) {
                val layer = colorLayers[i]
                if (!layer.enabled) continue

                val layerColor = layer.effect.compute(pos, colorContexts[i])
                result = ColorBlending.blend(
                    base = result,
                    overlay = layerColor,
                    mode = layer.blendMode,
                    opacity = layer.opacity
                )
            }

            // Apply master dimmer
            if (masterDimmer <= 0f) return Color.BLACK
            if (masterDimmer >= 1f) return result

            // Bypass clamped() to avoid double allocation (components are already 0-1)
            return Color(
                result.r * masterDimmer,
                result.g * masterDimmer,
                result.b * masterDimmer
            )
        }

        /**
         * Evaluate the full fixture output including color and movement layers.
         *
         * Color layers are composited first, then movement layers are
         * composited onto a [FixtureOutput] that starts with the computed color.
         */
        fun evaluateFixtureOutput(pos: Vec3): FixtureOutput {
            // First evaluate color stack
            val color = evaluate(pos)

            // Start with the color result
            var result = FixtureOutput(color = color)

            // Composite movement layers
            for (i in movementLayers.indices) {
                val layer = movementLayers[i]
                if (!layer.enabled) continue

                val layerOutput = layer.effect.computeMovement(pos, movementContexts[i])
                result = result.blendMovementOnly(
                    other = layerOutput,
                    mode = layer.blendMode,
                    opacity = layer.opacity
                )
            }

            return result
        }

        /** Whether this frame has any movement layers. */
        val hasMovementLayers: Boolean get() = movementLayers.any { it.enabled }
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

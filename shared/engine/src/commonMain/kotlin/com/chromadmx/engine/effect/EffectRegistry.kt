package com.chromadmx.engine.effect

/**
 * Thread-safe registry for looking up [SpatialEffect] instances by [SpatialEffect.id].
 *
 * The registry is pre-populated with the built-in V1 effects at construction
 * time.  Additional effects (e.g. user-created or plugin-provided) can be
 * registered at runtime.
 */
class EffectRegistry {

    private val effects = mutableMapOf<String, SpatialEffect>()

    /** Register an effect. Overwrites any previous effect with the same id. */
    fun register(effect: SpatialEffect) {
        effects[effect.id] = effect
    }

    /** Look up an effect by its unique [id], or null if not found. */
    fun get(id: String): SpatialEffect? = effects[id]

    /** All currently registered effect ids. */
    fun ids(): Set<String> = effects.keys.toSet()

    /** All currently registered effects. */
    fun all(): List<SpatialEffect> = effects.values.toList()

    /** Number of registered effects. */
    val size: Int get() = effects.size

    /** Remove an effect by [id]. Returns true if an effect was removed. */
    fun unregister(id: String): Boolean = effects.remove(id) != null
}

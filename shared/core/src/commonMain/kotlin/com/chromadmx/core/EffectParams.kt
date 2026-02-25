package com.chromadmx.core

import com.chromadmx.core.model.Color

/**
 * Flexible parameter bag for per-effect configuration.
 *
 * Internally a `Map<String, Any>` — typed getters extract values safely,
 * returning a default when the key is missing or the wrong type.
 */
class EffectParams(
    private val params: Map<String, Any> = emptyMap()
) {
    /* ------------------------------------------------------------------ */
    /*  Typed getters                                                      */
    /* ------------------------------------------------------------------ */

    fun getFloat(key: String, default: Float = 0f): Float =
        when (val v = params[key]) {
            is Float  -> v
            is Double -> v.toFloat()
            is Int    -> v.toFloat()
            is Number -> v.toFloat()
            else      -> default
        }

    fun getInt(key: String, default: Int = 0): Int =
        when (val v = params[key]) {
            is Int    -> v
            is Float  -> v.toInt()
            is Double -> v.toInt()
            is Number -> v.toInt()
            else      -> default
        }

    fun getString(key: String, default: String = ""): String =
        (params[key] as? String) ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        (params[key] as? Boolean) ?: default

    fun getColor(key: String, default: Color = Color.WHITE): Color =
        (params[key] as? Color) ?: default

    @Suppress("UNCHECKED_CAST")
    fun getColorList(key: String, default: List<Color> = emptyList()): List<Color> =
        (params[key] as? List<Color>) ?: default

    /* ------------------------------------------------------------------ */
    /*  Query / immutable update                                           */
    /* ------------------------------------------------------------------ */

    fun contains(key: String): Boolean = key in params

    /** Return a new [EffectParams] with [key] set to [value]. */
    fun with(key: String, value: Any): EffectParams =
        EffectParams(params + (key to value))

    /** Return a new [EffectParams] with all entries from [other] merged in. */
    fun merge(other: EffectParams): EffectParams =
        EffectParams(params + other.params)

    /** The underlying map (read-only). */
    fun toMap(): Map<String, Any> = params

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EffectParams) return false
        return params == other.params
    }

    override fun hashCode(): Int = params.hashCode()

    override fun toString(): String = "EffectParams($params)"

    companion object {
        val EMPTY = EffectParams()
    }
}

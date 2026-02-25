package com.chromadmx.core

import com.chromadmx.core.model.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Flexible parameter bag for per-effect configuration.
 *
 * Internally a `Map<String, Any>` — typed getters extract values safely,
 * returning a default when the key is missing or the wrong type.
 */
@Serializable(with = EffectParamsSerializer::class)
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

/**
 * Custom serializer for [EffectParams] that handles the `Map<String, Any>`
 * by converting supported types to/from [JsonElement].
 */
object EffectParamsSerializer : KSerializer<EffectParams> {
    private val mapSerializer = MapSerializer(String.serializer(), JsonElement.serializer())
    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun serialize(encoder: Encoder, value: EffectParams) {
        val jsonMap = value.toMap().mapValues { (_, v) ->
            when (v) {
                is Float -> JsonPrimitive(v)
                is Double -> JsonPrimitive(v)
                is Int -> JsonPrimitive(v)
                is Long -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                is String -> JsonPrimitive(v)
                is Color -> json.encodeToJsonElement(Color.serializer(), v)
                is List<*> -> {
                    JsonArray(v.map { item ->
                        when (item) {
                            is Color -> json.encodeToJsonElement(Color.serializer(), item)
                            is Float -> JsonPrimitive(item)
                            is Double -> JsonPrimitive(item)
                            is Int -> JsonPrimitive(item)
                            is Long -> JsonPrimitive(item)
                            is Boolean -> JsonPrimitive(item)
                            is String -> JsonPrimitive(item)
                            else -> JsonPrimitive(item.toString())
                        }
                    })
                }
                else -> JsonPrimitive(v.toString())
            }
        }
        encoder.encodeSerializableValue(mapSerializer, jsonMap)
    }

    override fun deserialize(decoder: Decoder): EffectParams {
        val jsonMap = decoder.decodeSerializableValue(mapSerializer)
        val map = jsonMap.mapValues { (_, v) ->
            when (v) {
                is JsonPrimitive -> {
                    if (v is JsonNull) "null"
                    else if (v.isString) v.content
                    else if (v.booleanOrNull != null) v.boolean
                    else if (v.doubleOrNull != null) {
                        val d = v.double
                        if (d == d.toInt().toDouble()) d.toInt() else d.toFloat()
                    } else v.content
                }
                is JsonObject -> {
                    if (v.containsKey("r") && v.containsKey("g") && v.containsKey("b")) {
                        json.decodeFromJsonElement(Color.serializer(), v)
                    } else v.toString()
                }
                is JsonArray -> {
                    if (v.isNotEmpty() && v[0] is JsonObject && (v[0] as JsonObject).containsKey("r")) {
                        v.map { json.decodeFromJsonElement(Color.serializer(), it) }
                    } else {
                        v.map { (it as? JsonPrimitive)?.content ?: it.toString() }
                    }
                }
            }
        }
        return EffectParams(map)
    }
}

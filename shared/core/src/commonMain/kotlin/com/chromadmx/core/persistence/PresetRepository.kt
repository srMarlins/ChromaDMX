package com.chromadmx.core.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.model.Genre
import com.chromadmx.core.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLDelight-backed repository for preset persistence.
 */
class PresetRepository(private val db: ChromaDmxDatabase) {

    private val queries get() = db.fixturesQueries
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Observe all presets, ordered by sort order and creation date. */
    fun allPresets(): Flow<List<Preset>> {
        return queries.selectAllPresets()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.mapNotNull(::toModel) }
    }

    /** Observe only built-in presets, ordered by sort order and name. */
    fun builtinPresets(): Flow<List<Preset>> {
        return queries.selectBuiltinPresets()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.mapNotNull(::toModel) }
    }

    private fun toModel(row: com.chromadmx.core.db.Preset): Preset? {
        return runCatching {
            Preset(
                id = row.preset_id,
                name = row.name,
                genre = row.genre?.let { name -> Genre.entries.find { it.name == name } },
                layers = json.decodeFromString(row.layer_data),
                masterDimmer = row.master_dimmer.toFloat(),
                isBuiltIn = row.is_builtin == 1L,
                sortOrder = row.sort_order.toInt(),
                createdAt = row.created_at,
                thumbnailColors = json.decodeFromString(row.thumbnail_colors_json)
            )
        }.getOrNull()
    }

    /** Insert or replace a preset. */
    suspend fun save(preset: Preset) {
        queries.insertOrReplacePreset(
            preset_id = preset.id,
            name = preset.name,
            genre = preset.genre?.name,
            layer_data = json.encodeToString(preset.layers),
            master_dimmer = preset.masterDimmer.toDouble(),
            is_builtin = if (preset.isBuiltIn) 1L else 0L,
            sort_order = preset.sortOrder.toLong(),
            created_at = preset.createdAt,
            thumbnail_colors_json = json.encodeToString(preset.thumbnailColors)
        )
    }

    /** Delete a preset by its ID. */
    suspend fun delete(presetId: String) {
        queries.deletePreset(presetId)
    }
}

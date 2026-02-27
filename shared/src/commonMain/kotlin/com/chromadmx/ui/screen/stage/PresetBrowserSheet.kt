package com.chromadmx.ui.screen.stage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.Genre
import com.chromadmx.core.model.ScenePreset
import com.chromadmx.ui.components.PixelBottomSheet
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelChip
import com.chromadmx.ui.components.PixelDialog
import com.chromadmx.ui.components.PixelDropdown
import com.chromadmx.ui.components.PixelTextField
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.state.StageEvent
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.theme.PixelShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

// ============================================================================
// PresetBrowserSheet — Bottom sheet for browsing, applying, saving, and
// deleting presets. Genre filter tabs, preset card grid, save dialog.
// ============================================================================

/**
 * Bottom sheet displaying a grid of browsable presets with genre filtering,
 * save/delete/favorite actions.
 *
 * @param visible       Whether the sheet is shown.
 * @param presets       Full list of [ScenePreset] from the library.
 * @param favoriteIds   Set of favorite preset IDs.
 * @param activePresetName Currently active preset name for highlight.
 * @param onEvent       Event callback routed to [StageViewModelV2.onEvent].
 * @param onDismiss     Callback to close the sheet.
 */
@Composable
fun PresetBrowserSheet(
    visible: Boolean,
    presets: ImmutableList<ScenePreset>,
    favoriteIds: ImmutableSet<String>,
    activePresetName: String?,
    onEvent: (StageEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedGenre by remember { mutableStateOf<Genre?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<ScenePreset?>(null) }

    val filteredPresets = remember(presets, selectedGenre) {
        if (selectedGenre == null) presets
        else presets.filter { it.genre == selectedGenre }
    }

    PixelBottomSheet(visible = visible, onDismiss = onDismiss) {
        // ── Header row ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = PixelDesign.colors.primary,
            )

            PixelButton(
                onClick = { showSaveDialog = true },
                variant = PixelButtonVariant.Secondary,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "SAVE CURRENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 8.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Genre filter tabs ───────────────────────────────────────
        GenreFilterRow(
            selectedGenre = selectedGenre,
            onGenreSelected = { selectedGenre = it },
        )

        Spacer(Modifier.height(10.dp))

        // ── Preset grid ─────────────────────────────────────────────
        if (filteredPresets.isEmpty()) {
            Text(
                text = "No presets found",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 10.sp,
                ),
                color = PixelDesign.colors.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            PresetGrid(
                presets = filteredPresets,
                favoriteIds = favoriteIds,
                activePresetName = activePresetName,
                onApply = { preset ->
                    onEvent(StageEvent.ApplyScene(preset.name))
                },
                onToggleFavorite = { preset ->
                    onEvent(StageEvent.ToggleFavorite(preset.id))
                },
                onLongPress = { preset ->
                    if (!preset.isBuiltIn) {
                        showDeleteConfirmation = preset
                    }
                },
            )
        }
    }

    // ── Save dialog ─────────────────────────────────────────────────
    if (showSaveDialog) {
        SavePresetDialog(
            onSave = { name, genre ->
                onEvent(StageEvent.SaveCurrentPreset(name, genre))
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    // ── Delete confirmation dialog ──────────────────────────────────
    showDeleteConfirmation?.let { preset ->
        DeletePresetDialog(
            presetName = preset.name,
            onConfirm = {
                onEvent(StageEvent.DeletePreset(preset.id))
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null },
        )
    }
}

// ── Genre Filter Row ────────────────────────────────────────────────────

@Composable
private fun GenreFilterRow(
    selectedGenre: Genre?,
    onGenreSelected: (Genre?) -> Unit,
) {
    val genres = listOf(null) + Genre.entries
    val genreLabels = listOf("ALL") + Genre.entries.map { it.name }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(genres.size) { index ->
            PixelChip(
                text = genreLabels[index],
                selected = genres[index] == selectedGenre,
                onClick = { onGenreSelected(genres[index]) },
            )
        }
    }
}

// ── Preset Grid ─────────────────────────────────────────────────────────

@Composable
private fun PresetGrid(
    presets: List<ScenePreset>,
    favoriteIds: Set<String>,
    activePresetName: String?,
    onApply: (ScenePreset) -> Unit,
    onToggleFavorite: (ScenePreset) -> Unit,
    onLongPress: (ScenePreset) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                isFavorite = preset.id in favoriteIds,
                isActive = preset.name == activePresetName,
                onTap = { onApply(preset) },
                onLongPress = { onLongPress(preset) },
                onToggleFavorite = { onToggleFavorite(preset) },
            )
        }
    }
}

// ── Preset Card ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetCard(
    preset: ScenePreset,
    isFavorite: Boolean,
    isActive: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val borderColor = if (isActive) {
        PixelDesign.colors.primary
    } else {
        PixelDesign.colors.outlineVariant.copy(alpha = 0.5f)
    }
    val bgColor = if (isActive) {
        PixelDesign.colors.primary.copy(alpha = 0.1f)
    } else {
        PixelDesign.colors.surfaceVariant.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelBorder(
                width = 1.dp,
                color = borderColor,
                pixelSize = 1.dp,
            )
            .clip(PixelShape(4.dp))
            .background(bgColor)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Color swatch thumbnail ──────────────────────────────────
        ColorSwatchRow(
            colors = preset.thumbnailColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        )

        Spacer(Modifier.height(6.dp))

        // ── Preset name ─────────────────────────────────────────────
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = if (isActive) PixelDesign.colors.primary else PixelDesign.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))

        // ── Bottom row: favorite + genre tag ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Favorite star / Lock icon for built-in
            Text(
                text = if (preset.isBuiltIn) {
                    "\uD83D\uDD12" // lock
                } else if (isFavorite) {
                    "\u2605" // filled star
                } else {
                    "\u2606" // empty star
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                ),
                color = when {
                    preset.isBuiltIn -> PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.5f)
                    isFavorite -> PixelDesign.colors.tertiary
                    else -> PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.clickable(
                    enabled = !preset.isBuiltIn,
                    onClick = onToggleFavorite,
                ),
            )

            // Genre tag
            Text(
                text = preset.genre?.name ?: "---",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 7.sp,
                ),
                color = PixelDesign.colors.info.copy(alpha = 0.7f),
            )
        }
    }
}

// ── Color Swatch Row ────────────────────────────────────────────────────

@Composable
private fun ColorSwatchRow(
    colors: List<com.chromadmx.core.model.Color>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(PixelShape(2.dp))
            .background(PixelDesign.colors.surface),
        horizontalArrangement = Arrangement.Start,
    ) {
        if (colors.isEmpty()) {
            // Fallback: dark gradient placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .background(PixelDesign.colors.surfaceVariant),
            )
        } else {
            colors.forEach { dmxColor ->
                val composeColor = Color(
                    red = dmxColor.r,
                    green = dmxColor.g,
                    blue = dmxColor.b,
                    alpha = 1f,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(composeColor),
                )
            }
        }
    }
}

// ── Save Preset Dialog ──────────────────────────────────────────────────

@Composable
private fun SavePresetDialog(
    onSave: (name: String, genre: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var presetName by remember { mutableStateOf("") }
    val genreOptions = remember { Genre.entries.map { it.name } }
    var selectedGenreIndex by remember { mutableStateOf(genreOptions.indexOf("CUSTOM").coerceAtLeast(0)) }

    PixelDialog(
        onDismissRequest = onDismiss,
        title = "SAVE PRESET",
        confirmButton = {
            PixelButton(
                onClick = {
                    val name = presetName.trim()
                    if (name.isNotEmpty()) {
                        onSave(name, genreOptions[selectedGenreIndex])
                    }
                },
                enabled = presetName.trim().isNotEmpty(),
            ) {
                Text(
                    text = "SAVE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                )
            }
        },
        dismissButton = {
            PixelButton(
                onClick = onDismiss,
                variant = PixelButtonVariant.Secondary,
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                )
            }
        },
    ) {
        PixelTextField(
            value = presetName,
            onValueChange = { presetName = it },
            label = "Name",
            placeholder = "My Preset",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "\u25B8 GENRE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 8.sp,
            ),
            color = PixelDesign.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        PixelDropdown(
            items = genreOptions,
            selectedIndex = selectedGenreIndex,
            onItemSelected = { selectedGenreIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Delete Confirmation Dialog ──────────────────────────────────────────

@Composable
private fun DeletePresetDialog(
    presetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PixelDialog(
        onDismissRequest = onDismiss,
        title = "DELETE PRESET",
        confirmButton = {
            PixelButton(
                onClick = onConfirm,
                variant = PixelButtonVariant.Danger,
            ) {
                Text(
                    text = "DELETE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                )
            }
        },
        dismissButton = {
            PixelButton(
                onClick = onDismiss,
                variant = PixelButtonVariant.Surface,
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                )
            }
        },
    ) {
        Text(
            text = "Delete \"$presetName\"? This cannot be undone.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 10.sp,
            ),
            color = PixelDesign.colors.onSurface,
        )
    }
}

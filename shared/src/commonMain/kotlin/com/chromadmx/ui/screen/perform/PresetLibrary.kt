package com.chromadmx.ui.screen.perform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.ScenePreset
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.ui.theme.DmxBackground

/**
 * Full-screen (or bottom sheet) grid view of presets with genre filters.
 */
@Composable
fun PresetLibrary(
    presets: List<ScenePreset>,
    genres: List<String>,
    fixtures: List<Fixture3D>,
    effectRegistry: EffectRegistry,
    onPresetTap: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val filteredScenes = if (selectedGenre == null) {
        presets
    } else {
        presets.filter { it.genre?.name?.equals(selectedGenre, ignoreCase = true) == true }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DmxBackground.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRESET LIBRARY",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Button(onClick = onClose) {
                Text("DONE")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedGenre == null,
                onClick = { selectedGenre = null },
                label = { Text("ALL") }
            )
            genres.forEach { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = { selectedGenre = genre },
                    label = { Text(genre.uppercase()) }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(70.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredScenes, key = { it.id }) { preset ->
                PresetThumbnailItem(
                    preset = preset,
                    isActive = false,
                    fixtures = fixtures,
                    effectRegistry = effectRegistry,
                    onTap = {
                        onPresetTap(preset.id)
                        onClose()
                    },
                    onLongPress = {},
                    onRelease = {}
                )
            }
        }
    }
}

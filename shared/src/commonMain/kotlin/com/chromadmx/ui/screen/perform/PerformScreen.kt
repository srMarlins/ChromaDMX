package com.chromadmx.ui.screen.perform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.screen.settings.SettingsScreen
import com.chromadmx.ui.viewmodel.PerformViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModel
import org.koin.compose.getKoin
import com.chromadmx.core.model.Color as DmxColor

/**
 * Main perform screen: venue canvas visualization at top, beat visualization,
 * effect layer cards in a scrollable column, master dimmer, and scene presets.
 */
@Composable
fun PerformScreen(
    viewModel: PerformViewModel,
    fixtures: List<Fixture3D> = emptyList(),
    fixtureColors: List<DmxColor> = emptyList(),
) {
    val beatState by viewModel.beatState.collectAsState()
    val masterDimmer by viewModel.masterDimmer.collectAsState()
    val layers by viewModel.layers.collectAsState()
    var activePreset by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Venue canvas visualization (when fixtures are mapped)
            if (fixtures.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    VenueCanvas(
                        fixtures = fixtures,
                        fixtureColors = fixtureColors,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Gear icon overlay
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

        // Beat visualization + tap button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BeatVisualization(
                beatState = beatState,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = { viewModel.tap() },
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Text("TAP")
            }
        }

        // Master dimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Master",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MasterDimmerSlider(
                value = masterDimmer,
                onValueChange = { viewModel.setMasterDimmer(it) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Effect layers header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Effect Layers",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            OutlinedButton(onClick = { viewModel.addLayer() }) {
                Text("+ Add Layer")
            }
        }

        // Layer cards
        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(layers, key = { index, layer -> "${index}_${layer.effect.id}" }) { index, layer ->
                EffectLayerCard(
                    layerIndex = index,
                    layer = layer,
                    onOpacityChange = { viewModel.setLayerOpacity(index, it) },
                    onToggleEnabled = { viewModel.toggleLayerEnabled(index) },
                    onRemove = { viewModel.removeLayer(index) },
                )
            }

            if (layers.isEmpty()) {
                item {
                    Text(
                        text = "No effect layers. Tap '+ Add Layer' to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    )
                }
            }
        }

        // Scene presets
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Scene Presets",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(4.dp))
        ScenePresetRow(
            activePreset = activePreset,
            onPresetTap = { activePreset = it },
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

        // Settings Overlay
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            val koin = getKoin()
            val settingsVm = remember { koin.get<SettingsViewModel>() }
            DisposableEffect(settingsVm) {
                onDispose { settingsVm.onCleared() }
            }

            SettingsScreen(
                viewModel = settingsVm,
                onClose = { showSettings = false }
            )
        }
    }
}

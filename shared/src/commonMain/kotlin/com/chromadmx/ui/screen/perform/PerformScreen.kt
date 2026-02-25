package com.chromadmx.ui.screen.perform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.theme.DmxBackground
import com.chromadmx.ui.viewmodel.PerformViewModel
import com.chromadmx.core.model.Color as DmxColor

/**
 * Main perform screen: venue canvas visualization at top with overlaid controls,
 * beat visualization, and preset strip at the bottom.
 *
 * The layout uses a stack (Box) to overlay the master dimmer and the swipeable
 * effect layer panel over the stage preview.
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
    val scenes by viewModel.allScenes.collectAsState()
    val genres = viewModel.availableGenres()

    var isLayerPanelVisible by remember { mutableStateOf(false) }
    var isLibraryVisible by remember { mutableStateOf(false) }
    var activePreset by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DmxBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Top section: Venue Canvas with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Background stage preview
                VenueCanvas(
                    fixtures = fixtures,
                    fixtureColors = fixtureColors,
                    modifier = Modifier.fillMaxSize(),
                )

                // Master Dimmer (Vertical, Right Edge)
                MasterDimmerSlider(
                    value = masterDimmer,
                    onValueChange = { viewModel.setMasterDimmer(it) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp, top = 20.dp, bottom = 20.dp)
                )

                // Swipe trigger area for layer panel (top portion only, avoids master dimmer)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(40.dp)
                        .align(Alignment.CenterEnd)
                        .padding(bottom = 200.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount < -15f) {
                                    isLayerPanelVisible = true
                                }
                            }
                        }
                )
            }

            // Middle section: Beat visualization
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

            // Bottom section: Scene Presets
            Spacer(Modifier.height(4.dp))
            ScenePresetRow(
                scenes = scenes,
                activePreset = activePreset,
                fixtures = fixtures,
                effectRegistry = viewModel.effectRegistry,
                onPresetTap = {
                    activePreset = it
                    viewModel.applyScene(it)
                },
                onPresetLongPress = { viewModel.previewScene(it) },
                onSwipeUp = { isLibraryVisible = true },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        // Tap background to close layer panel (must be before panel so panel draws on top)
        if (isLayerPanelVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 280.dp) // Width of panel
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 15f) isLayerPanelVisible = false
                        }
                    }
                    .clickable(enabled = true, onClick = { isLayerPanelVisible = false })
            )
        }

        // Layer Panel Overlay (Animated)
        AnimatedVisibility(
            visible = isLayerPanelVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            EffectLayerPanel(
                layers = layers,
                onOpacityChange = { index, opacity -> viewModel.setLayerOpacity(index, opacity) },
                onToggleEnabled = { index -> viewModel.toggleLayerEnabled(index) },
                onReorder = { from, to -> viewModel.reorderLayer(from, to) },
                onAddLayer = { viewModel.addLayer() },
                onRemoveLayer = { index -> viewModel.removeLayer(index) },
                modifier = Modifier.pointerInput(Unit) {
                    // Detect swipe right to close
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 15f) {
                            isLayerPanelVisible = false
                        }
                    }
                }
            )
        }

        // Preset Library Overlay
        AnimatedVisibility(
            visible = isLibraryVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PresetLibrary(
                scenes = scenes,
                genres = genres,
                fixtures = fixtures,
                effectRegistry = viewModel.effectRegistry,
                onPresetTap = {
                    activePreset = it
                    viewModel.applyScene(it)
                    isLibraryVisible = false
                },
                onClose = { isLibraryVisible = false }
            )
        }
    }
}

package com.chromadmx.ui.screen.stage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.ui.components.AudienceView
import com.chromadmx.ui.components.NodeHealthCompact
import com.chromadmx.ui.components.PixelBottomSheet
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelChip
import com.chromadmx.ui.components.PixelDropdown
import com.chromadmx.ui.components.PixelIconButton
import com.chromadmx.ui.components.PixelScaffold
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.PresetStrip
import com.chromadmx.ui.components.SimulationBadge
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.components.beat.BeatBar
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.screen.network.NodeListOverlay
import com.chromadmx.ui.state.FixtureState
import com.chromadmx.ui.state.IsoAngle
import com.chromadmx.ui.state.NetworkState
import com.chromadmx.ui.state.PerformanceState
import com.chromadmx.ui.state.PresetState
import com.chromadmx.ui.state.StageEvent
import com.chromadmx.ui.state.ViewMode
import com.chromadmx.ui.state.ViewState
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.viewmodel.StageViewModel
import com.chromadmx.ui.viewmodel.StageViewModelV2

// ============================================================================
// StageScreen — Main stage view with isometric/top-down/audience view modes,
// sliced state consumption, and the pixel design system.
//
// Each UI region subscribes to ONLY its needed state slice to minimize
// recomposition. All mutations route through StageViewModelV2.onEvent().
// ============================================================================

/**
 * Main Stage screen with isometric/top-down/audience view modes and sliced state.
 *
 * @param viewModel The V2 view model exposing sliced state flows.
 * @param onSettings Callback to navigate to the settings screen.
 * @param modifier Optional modifier.
 */
@Composable
fun StageScreen(
    viewModel: StageViewModelV2,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val perfState by viewModel.performanceState.collectAsState()
    val fixtureState by viewModel.fixtureState.collectAsState()
    val presetState by viewModel.presetState.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val viewState by viewModel.viewState.collectAsState()

    // Local overlay state
    var showFixtureEdit by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showSimTooltip by remember { mutableStateOf(false) }

    PixelScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            StageTopBar(
                perfState = perfState,
                networkState = networkState,
                viewState = viewState,
                isEditMode = fixtureState.isEditMode,
                onEvent = viewModel::onEvent,
                onSettings = onSettings,
            )
        },
        bottomBar = {
            PresetStripBar(presetState = presetState, onEvent = viewModel::onEvent)
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Main stage view — switches between modes
            when (viewState.mode) {
                ViewMode.TOP_DOWN -> VenueCanvas(
                    fixtures = fixtureState.fixtures,
                    fixtureColors = fixtureState.fixtureColors,
                    selectedFixtureIndex = fixtureState.selectedFixtureIndex,
                    isEditMode = fixtureState.isEditMode,
                    onFixtureTapped = { viewModel.onEvent(StageEvent.SelectFixture(it)) },
                    onBackgroundTapped = { viewModel.onEvent(StageEvent.SelectFixture(null)) },
                    onFixtureDragged = { idx, pos ->
                        viewModel.onEvent(StageEvent.UpdateFixturePosition(idx, pos))
                    },
                    onDragEnd = { idx ->
                        viewModel.onEvent(StageEvent.PersistFixturePosition(idx))
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                ViewMode.ISO -> IsometricPlaceholder(
                    fixtureState = fixtureState,
                    viewState = viewState,
                    onFixtureTapped = { viewModel.onEvent(StageEvent.SelectFixture(it)) },
                    onBackgroundTapped = { viewModel.onEvent(StageEvent.SelectFixture(null)) },
                    modifier = Modifier.fillMaxSize(),
                )

                ViewMode.AUDIENCE -> AudienceView(
                    fixtures = fixtureState.fixtures,
                    fixtureColors = fixtureState.fixtureColors,
                    onBackgroundTapped = { viewModel.onEvent(StageEvent.SelectFixture(null)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // --- Overlays ---

            // Camera controls overlay (top-right area)
            CameraControls(
                viewState = viewState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp),
            )

            // Simulation badge (top-left)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp),
            ) {
                AnimatedVisibility(
                    visible = viewState.isSimulationMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SimulationBadge(onTap = { showSimTooltip = !showSimTooltip })
                }
                AnimatedVisibility(
                    visible = showSimTooltip && viewState.isSimulationMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    PixelCard(
                        backgroundColor = PixelDesign.colors.surface.copy(alpha = 0.95f),
                        borderColor = PixelDesign.colors.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            text = "SIMULATION: ${viewState.simulationPresetName ?: "Custom"} (${viewState.simulationFixtureCount} fixtures)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PixelDesign.colors.onSurface,
                            ),
                        )
                    }
                }
            }

            // Edit mode indicator
            AnimatedVisibility(
                visible = fixtureState.isEditMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = "EDIT MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 8.sp,
                    ),
                    color = PixelDesign.colors.warning,
                    modifier = Modifier
                        .pixelBorder(
                            width = 1.dp,
                            color = PixelDesign.colors.warning.copy(alpha = 0.4f),
                            pixelSize = 1.dp,
                        )
                        .background(Color(0x88000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Edit Mode FAB (rescan)
            AnimatedVisibility(
                visible = fixtureState.isEditMode,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(StageEvent.RescanFixtures) },
                    containerColor = PixelDesign.colors.secondary,
                    contentColor = PixelDesign.colors.onSecondary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-scan Fixtures",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Fixture info/edit overlay
            AnimatedVisibility(
                visible = fixtureState.selectedFixtureIndex != null && !showFixtureEdit,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                fixtureState.selectedFixtureIndex?.let { index ->
                    val fixture = fixtureState.fixtures.getOrNull(index)
                    if (fixture != null) {
                        if (fixtureState.isEditMode) {
                            FixtureEditOverlay(
                                fixture = fixture,
                                fixtureIndex = index,
                                groups = fixtureState.groups,
                                onZHeightChanged = { z ->
                                    viewModel.onEvent(StageEvent.UpdateZHeight(index, z))
                                },
                                onGroupAssigned = { groupId ->
                                    viewModel.onEvent(StageEvent.AssignGroup(index, groupId))
                                },
                                onCreateGroup = {
                                    newGroupName = ""
                                    showNewGroupDialog = true
                                },
                                onTestFire = {
                                    viewModel.onEvent(StageEvent.TestFireFixture(index))
                                },
                                onDismiss = {
                                    viewModel.onEvent(StageEvent.SelectFixture(null))
                                },
                            )
                        } else {
                            FixtureInfoOverlay(
                                fixture = fixture,
                                fixtureIndex = index,
                                color = fixtureState.fixtureColors.getOrNull(index),
                                onDismiss = {
                                    viewModel.onEvent(StageEvent.SelectFixture(null))
                                },
                            )
                        }
                    }
                }
            }

            // Fixture edit bottom sheet (alternative to overlay for detailed editing)
            if (showFixtureEdit && fixtureState.selectedFixtureIndex != null) {
                val idx = fixtureState.selectedFixtureIndex!!
                if (idx in fixtureState.fixtures.indices) {
                    FixtureEditSheet(
                        fixture = fixtureState.fixtures[idx],
                        fixtureIndex = idx,
                        groups = fixtureState.groups,
                        onEvent = viewModel::onEvent,
                        onDismiss = { showFixtureEdit = false },
                    )
                }
            }

            // New group dialog
            if (showNewGroupDialog) {
                AlertDialog(
                    onDismissRequest = { showNewGroupDialog = false },
                    title = { Text("New Group") },
                    text = {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text("Group name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val name = newGroupName.trim()
                                if (name.isNotEmpty()) {
                                    viewModel.onEvent(
                                        StageEvent.CreateGroup(name, 0xFF00FBFF)
                                    )
                                }
                                showNewGroupDialog = false
                            },
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewGroupDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }

            // Node list overlay
            if (networkState.isNodeListOpen) {
                NodeListOverlay(
                    nodes = networkState.nodes,
                    currentTimeMs = networkState.currentTimeMs,
                    onDiagnose = { viewModel.onEvent(StageEvent.DiagnoseNode(it)) },
                    onClose = { viewModel.onEvent(StageEvent.ToggleNodeList) },
                )
            }
        }
    }
}

// ============================================================================
// StageTopBar — Top bar with performance info and controls.
// Subscribes to: PerformanceState, NetworkState, ViewState, isEditMode.
// ============================================================================

@Composable
private fun StageTopBar(
    perfState: PerformanceState,
    networkState: NetworkState,
    viewState: ViewState,
    isEditMode: Boolean,
    onEvent: (StageEvent) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PixelDesign.colors.surface.copy(alpha = 0.95f))
            .pixelBorder(
                width = 1.dp,
                color = PixelDesign.colors.outlineVariant,
                pixelSize = 1.dp,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Beat bar (BPM + phase indicators)
            BeatBar(
                beatState = perfState.beatState,
                onTapTempo = { onEvent(StageEvent.TapTempo) },
                modifier = Modifier.weight(1f),
            )

            // Master dimmer compact
            MasterDimmerCompact(
                value = perfState.masterDimmer,
                onValueChange = { onEvent(StageEvent.SetMasterDimmer(it)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )

            // Controls row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Node health
                NodeHealthCompact(
                    nodes = networkState.nodes,
                    currentTimeMs = networkState.currentTimeMs,
                    onClick = { onEvent(StageEvent.ToggleNodeList) },
                )

                // Edit mode toggle
                IconButton(
                    onClick = { onEvent(StageEvent.ToggleEditMode) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Mode",
                        tint = if (isEditMode) {
                            PixelDesign.colors.warning
                        } else {
                            PixelDesign.colors.onSurface.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Settings button
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = PixelDesign.colors.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ============================================================================
// PresetStripBar — Bottom preset selector.
// Subscribes to: PresetState.
// ============================================================================

@Composable
private fun PresetStripBar(
    presetState: PresetState,
    onEvent: (StageEvent) -> Unit,
) {
    PresetStrip(
        scenes = presetState.allScenes,
        activeSceneName = null, // Active scene tracked elsewhere; strip shows all
        onSceneTap = { name -> onEvent(StageEvent.ApplyScene(name)) },
        onSceneLongPress = { name -> onEvent(StageEvent.PreviewScene(name)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

// ============================================================================
// CameraControls — View mode and angle controls overlay.
// Subscribes to: ViewState.
// ============================================================================

@Composable
private fun CameraControls(
    viewState: ViewState,
    onEvent: (StageEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // View mode chips: "2D" "3D" "Front"
        PixelChip(
            text = "2D",
            selected = viewState.mode == ViewMode.TOP_DOWN,
            onClick = {
                if (viewState.mode != ViewMode.TOP_DOWN) {
                    onEvent(StageEvent.ToggleViewMode)
                }
            },
        )
        PixelChip(
            text = "3D",
            selected = viewState.mode == ViewMode.ISO,
            onClick = {
                if (viewState.mode != ViewMode.ISO) {
                    onEvent(StageEvent.ToggleViewMode)
                }
            },
        )
        PixelChip(
            text = "Front",
            selected = viewState.mode == ViewMode.AUDIENCE,
            onClick = {
                if (viewState.mode != ViewMode.AUDIENCE) {
                    onEvent(StageEvent.ToggleViewMode)
                }
            },
        )

        // Iso angle control (only visible in ISO mode)
        if (viewState.mode == ViewMode.ISO) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = viewState.isoAngle.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                ),
                color = PixelDesign.colors.onSurface,
            )
            PixelIconButton(
                onClick = {
                    onEvent(StageEvent.SetIsoAngle(nextAngle(viewState.isoAngle)))
                },
            ) {
                Text(
                    text = "\u21BB", // ↻ rotation symbol
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 14.sp,
                    ),
                    color = PixelDesign.colors.onSurface,
                )
            }
        }
    }
}

// ============================================================================
// FixtureEditSheet — Bottom sheet for editing a selected fixture.
// ============================================================================

@Composable
private fun FixtureEditSheet(
    fixture: Fixture3D,
    fixtureIndex: Int,
    groups: List<FixtureGroup>,
    onEvent: (StageEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    PixelBottomSheet(visible = true, onDismiss = onDismiss) {
        // Header
        Text(
            text = fixture.fixture.name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
            ),
            color = PixelDesign.colors.primary,
        )
        Text(
            text = "U${fixture.fixture.universeId}/${fixture.fixture.channelStart} | ${fixture.fixture.profileId}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 7.sp,
            ),
            color = PixelDesign.colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        // Z-Height slider
        val zStr = (kotlin.math.round(fixture.position.z * 10f) / 10f).toString()
        Text(
            text = "HEIGHT: ${zStr}m",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 7.sp,
            ),
            color = PixelDesign.colors.tertiary.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(4.dp))

        PixelSlider(
            value = fixture.position.z,
            onValueChange = { onEvent(StageEvent.UpdateZHeight(fixtureIndex, it)) },
            valueRange = 0f..10f,
            accentColor = PixelDesign.colors.tertiary,
            modifier = Modifier.fillMaxWidth().height(28.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Group dropdown
        val groupNames = listOf("None") + groups.map { it.name }
        val currentGroupIdx = fixture.groupId?.let { gid ->
            groups.indexOfFirst { it.groupId == gid }.let { if (it >= 0) it + 1 else 0 }
        } ?: 0

        Text(
            text = "GROUP",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 7.sp,
            ),
            color = PixelDesign.colors.info.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))

        PixelDropdown(
            items = groupNames,
            selectedIndex = currentGroupIdx,
            onItemSelected = { idx ->
                val groupId = if (idx == 0) null else groups.getOrNull(idx - 1)?.groupId
                onEvent(StageEvent.AssignGroup(fixtureIndex, groupId))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PixelButton(
                onClick = { onEvent(StageEvent.TestFireFixture(fixtureIndex)) },
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            ) {
                Text("TEST FIRE", fontSize = 8.sp)
            }

            PixelButton(
                onClick = { onEvent(StageEvent.RemoveFixture(fixtureIndex)) },
                variant = PixelButtonVariant.Danger,
                modifier = Modifier.weight(1f),
            ) {
                Text("DELETE", fontSize = 8.sp)
            }
        }
    }
}

// ============================================================================
// IsometricPlaceholder — Placeholder for the isometric renderer.
// Will be replaced by the full IsometricRenderer composable from the
// renderer module once it lands (issue #92).
// ============================================================================

@Composable
private fun IsometricPlaceholder(
    fixtureState: FixtureState,
    viewState: ViewState,
    onFixtureTapped: (Int) -> Unit,
    onBackgroundTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Until the full IsometricRenderer composable is available, render
    // the top-down VenueCanvas with a label overlay indicating ISO mode.
    Box(modifier = modifier) {
        VenueCanvas(
            fixtures = fixtureState.fixtures,
            fixtureColors = fixtureState.fixtureColors,
            selectedFixtureIndex = fixtureState.selectedFixtureIndex,
            isEditMode = fixtureState.isEditMode,
            onFixtureTapped = onFixtureTapped,
            onBackgroundTapped = onBackgroundTapped,
            modifier = Modifier.fillMaxSize(),
        )
        // Overlay badge indicating isometric mode
        PixelCard(
            backgroundColor = PixelDesign.colors.surface.copy(alpha = 0.85f),
            borderColor = PixelDesign.colors.primary.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Text(
                text = "ISO ${viewState.isoAngle.name}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                ),
                color = PixelDesign.colors.primary,
            )
        }
    }
}

// ============================================================================
// Helper composables (preserved from original StagePreviewScreen.kt)
// ============================================================================

@Composable
private fun MasterDimmerCompact(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "DIM",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 7.sp,
            ),
            color = PixelDesign.colors.secondary.copy(alpha = 0.8f),
        )
        PixelSlider(
            value = value,
            onValueChange = onValueChange,
            accentColor = PixelDesign.colors.secondary,
            modifier = Modifier.weight(1f).height(24.dp),
        )
        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 8.sp,
            ),
            color = PixelDesign.colors.secondary,
        )
    }
}

/**
 * Fixture info overlay shown when a fixture is tapped (non-edit mode).
 */
@Composable
internal fun FixtureInfoOverlay(
    fixture: Fixture3D,
    fixtureIndex: Int,
    color: com.chromadmx.core.model.Color?,
    onDismiss: () -> Unit,
) {
    val profile = BuiltInProfiles.findById(fixture.fixture.profileId)
    val profileName = profile?.name ?: "Unknown"
    val displayColor = color ?: com.chromadmx.core.model.Color.BLACK

    PixelCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        borderColor = PixelDesign.colors.primary.copy(alpha = 0.5f),
        backgroundColor = PixelDesign.colors.surface,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fixture.fixture.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 12.sp,
                    ),
                    color = PixelDesign.colors.primary,
                )
                Text(
                    text = "#${fixtureIndex + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                    color = PixelDesign.colors.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InfoLabel("TYPE", profileName)
                InfoLabel(
                    "ADDR",
                    "U${fixture.fixture.universeId}/${fixture.fixture.channelStart}",
                )
                InfoLabel(
                    "COLOR",
                    "R${(displayColor.r * 255).toInt()} G${(displayColor.g * 255).toInt()} B${(displayColor.b * 255).toInt()}",
                )
            }
        }
    }
}

@Composable
private fun InfoLabel(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 6.sp,
            ),
            color = PixelDesign.colors.tertiary.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 9.sp,
            ),
            color = PixelDesign.colors.onSurface,
        )
    }
}

// ============================================================================
// Utility functions
// ============================================================================

/** Cycle to the next isometric angle. */
private fun nextAngle(current: IsoAngle): IsoAngle = when (current) {
    IsoAngle.ZERO -> IsoAngle.FORTY_FIVE
    IsoAngle.FORTY_FIVE -> IsoAngle.NINETY
    IsoAngle.NINETY -> IsoAngle.ZERO
}

// ============================================================================
// Backward-compatible entry point.
// ChromaDmxApp.kt still resolves StageViewModel from DI and passes it here.
// This wrapper bridges to the old direct-StateFlow consumption pattern until
// the DI module is updated to provide StageViewModelV2 (issue #99).
// ============================================================================

/**
 * Legacy entry point preserved for backward compatibility with [ChromaDmxApp].
 *
 * Consumes state directly from the original [StageViewModel] using the same
 * pattern as the pre-rewrite screen. Once DI wiring is updated to provide
 * [StageViewModelV2], callers should migrate to [StageScreen].
 */
@Composable
fun StagePreviewScreen(
    viewModel: StageViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixtures by viewModel.fixtures.collectAsState()
    val fixtureColors by viewModel.fixtureColors.collectAsState()
    val beatState by viewModel.beatState.collectAsState()
    val masterDimmer by viewModel.masterDimmer.collectAsState()
    val selectedFixtureIndex by viewModel.selectedFixtureIndex.collectAsState()
    val scenes by viewModel.allScenes.collectAsState()
    val activeSceneName by viewModel.activeSceneName.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val simPresetName by viewModel.simulationPresetName.collectAsState()
    val simFixtureCount by viewModel.simulationFixtureCount.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isNodeListOpen by viewModel.isNodeListOpen.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var showSimTooltip by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    // Local view mode state (new tri-state mode: TOP_DOWN / ISO / AUDIENCE)
    var viewMode by remember { mutableStateOf(ViewMode.TOP_DOWN) }
    var isoAngle by remember { mutableStateOf(IsoAngle.FORTY_FIVE) }

    // Build sliced state objects for sub-composables
    val perfState = PerformanceState(
        beatState = beatState,
        bpm = beatState.bpm,
        isRunning = beatState.bpm > 0f,
        masterDimmer = masterDimmer,
        activeSceneName = activeSceneName,
    )
    val fixtureState = FixtureState(
        fixtures = fixtures,
        fixtureColors = fixtureColors,
        selectedFixtureIndex = selectedFixtureIndex,
        groups = groups,
        isEditMode = isEditMode,
    )
    val networkState = NetworkState(
        nodes = nodes,
        currentTimeMs = currentTimeMs,
        isNodeListOpen = isNodeListOpen,
    )
    val viewState = ViewState(
        mode = viewMode,
        isoAngle = isoAngle,
        isSimulationMode = isSimulationMode,
        simulationPresetName = simPresetName,
        simulationFixtureCount = simFixtureCount,
    )

    PixelScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            StageTopBar(
                perfState = perfState,
                networkState = networkState,
                viewState = viewState,
                isEditMode = isEditMode,
                onEvent = { event ->
                    when (event) {
                        is StageEvent.TapTempo -> viewModel.tap()
                        is StageEvent.SetMasterDimmer -> viewModel.setMasterDimmer(event.value)
                        is StageEvent.ToggleEditMode -> viewModel.toggleEditMode()
                        is StageEvent.ToggleNodeList -> viewModel.toggleNodeList()
                        else -> {} // other events not used in top bar
                    }
                },
                onSettings = onSettingsClick,
            )
        },
        bottomBar = {
            PresetStrip(
                scenes = scenes,
                activeSceneName = activeSceneName,
                onSceneTap = { name -> viewModel.applyScene(name) },
                onSceneLongPress = { name -> viewModel.previewScene(name) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Main stage view — switches between modes
            when (viewMode) {
                ViewMode.TOP_DOWN -> VenueCanvas(
                    fixtures = fixtures,
                    fixtureColors = fixtureColors,
                    selectedFixtureIndex = selectedFixtureIndex,
                    isEditMode = isEditMode,
                    onFixtureTapped = { index -> viewModel.selectFixture(index) },
                    onBackgroundTapped = { viewModel.selectFixture(null) },
                    onFixtureDragged = { index, newPos -> viewModel.updateFixturePosition(index, newPos) },
                    onDragEnd = { index -> viewModel.persistFixturePosition(index) },
                    modifier = Modifier.fillMaxSize(),
                )

                ViewMode.ISO -> IsometricPlaceholder(
                    fixtureState = fixtureState,
                    viewState = viewState,
                    onFixtureTapped = { index -> viewModel.selectFixture(index) },
                    onBackgroundTapped = { viewModel.selectFixture(null) },
                    modifier = Modifier.fillMaxSize(),
                )

                ViewMode.AUDIENCE -> AudienceView(
                    fixtures = fixtures,
                    fixtureColors = fixtureColors,
                    onBackgroundTapped = { viewModel.selectFixture(null) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Camera controls
            CameraControls(
                viewState = viewState,
                onEvent = { event ->
                    when (event) {
                        is StageEvent.ToggleViewMode -> {
                            viewMode = when (viewMode) {
                                ViewMode.TOP_DOWN -> ViewMode.ISO
                                ViewMode.ISO -> ViewMode.AUDIENCE
                                ViewMode.AUDIENCE -> ViewMode.TOP_DOWN
                            }
                        }
                        is StageEvent.SetIsoAngle -> { isoAngle = event.angle }
                        else -> {}
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp),
            )

            // Simulation badge
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp),
            ) {
                AnimatedVisibility(
                    visible = isSimulationMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SimulationBadge(onTap = { showSimTooltip = !showSimTooltip })
                }
                AnimatedVisibility(
                    visible = showSimTooltip && isSimulationMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    PixelCard(
                        backgroundColor = PixelDesign.colors.surface.copy(alpha = 0.95f),
                        borderColor = PixelDesign.colors.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            text = "SIMULATION: ${simPresetName ?: "Custom"} ($simFixtureCount fixtures)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PixelDesign.colors.onSurface,
                            ),
                        )
                    }
                }
            }

            // Edit mode indicator
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = "EDIT MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 8.sp,
                    ),
                    color = PixelDesign.colors.warning,
                    modifier = Modifier
                        .pixelBorder(
                            width = 1.dp,
                            color = PixelDesign.colors.warning.copy(alpha = 0.4f),
                            pixelSize = 1.dp,
                        )
                        .background(Color(0x88000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Edit mode FAB
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            ) {
                FloatingActionButton(
                    onClick = { viewModel.rescanFixtures() },
                    containerColor = PixelDesign.colors.secondary,
                    contentColor = PixelDesign.colors.onSecondary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-scan Fixtures",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Fixture info/edit overlay
            AnimatedVisibility(
                visible = selectedFixtureIndex != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                selectedFixtureIndex?.let { index ->
                    val fixture = fixtures.getOrNull(index)
                    if (fixture != null) {
                        if (isEditMode) {
                            FixtureEditOverlay(
                                fixture = fixture,
                                fixtureIndex = index,
                                groups = groups,
                                onZHeightChanged = { z -> viewModel.updateZHeight(index, z) },
                                onGroupAssigned = { groupId -> viewModel.assignGroup(index, groupId) },
                                onCreateGroup = {
                                    newGroupName = ""
                                    showNewGroupDialog = true
                                },
                                onTestFire = { viewModel.testFireFixture(index) },
                                onDismiss = { viewModel.selectFixture(null) },
                            )
                        } else {
                            FixtureInfoOverlay(
                                fixture = fixture,
                                fixtureIndex = index,
                                color = fixtureColors.getOrNull(index),
                                onDismiss = { viewModel.selectFixture(null) },
                            )
                        }
                    }
                }
            }

            // New group dialog
            if (showNewGroupDialog) {
                AlertDialog(
                    onDismissRequest = { showNewGroupDialog = false },
                    title = { Text("New Group") },
                    text = {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text("Group name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val name = newGroupName.trim()
                                if (name.isNotEmpty()) {
                                    viewModel.createGroup(name)
                                }
                                showNewGroupDialog = false
                            },
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewGroupDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }

            // Node list overlay
            if (isNodeListOpen) {
                NodeListOverlay(
                    nodes = nodes,
                    currentTimeMs = currentTimeMs,
                    onDiagnose = { viewModel.diagnoseNode(it) },
                    onClose = { viewModel.toggleNodeList() },
                )
            }
        }
    }
}

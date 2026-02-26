package com.chromadmx.ui.screen.stage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
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
import com.chromadmx.ui.components.AudienceView
import com.chromadmx.ui.components.NodeHealthCompact
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelScaffold
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.PresetStrip
import com.chromadmx.ui.components.SimulationBadge
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.components.beat.BeatBar
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.screen.network.NodeListOverlay
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.viewmodel.StageViewModel

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
    val isTopDownView by viewModel.isTopDownView.collectAsState()
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

    val pagerState = rememberPagerState(
        initialPage = if (isTopDownView) 0 else 1,
        pageCount = { 2 },
    )

    LaunchedEffect(pagerState.currentPage) {
        val shouldBeTopDown = pagerState.currentPage == 0
        if (viewModel.isTopDownView.value != shouldBeTopDown) {
            viewModel.toggleViewMode()
        }
    }

    PixelScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PixelTopBar(
                beatState = beatState,
                masterDimmer = masterDimmer,
                nodes = nodes,
                currentTimeMs = currentTimeMs,
                isEditMode = isEditMode,
                onTapTempo = { viewModel.tap() },
                onDimmerChange = { viewModel.setMasterDimmer(it) },
                onNodeHealthClick = { viewModel.toggleNodeList() },
                onEditModeClick = { viewModel.toggleEditMode() },
                onSettingsClick = onSettingsClick
            )
        },
        bottomBar = {
            PresetStrip(
                scenes = scenes,
                activeSceneName = activeSceneName,
                onSceneTap = { name -> viewModel.applyScene(name) },
                onSceneLongPress = { name -> viewModel.previewScene(name) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Content Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> VenueCanvas(
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
                    1 -> AudienceView(
                        fixtures = fixtures,
                        fixtureColors = fixtureColors,
                        modifier = Modifier.fillMaxSize(),
                        onBackgroundTapped = { viewModel.selectFixture(null) },
                    )
                }
            }

            // --- Overlays ---

            // Simulation Badge
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
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
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "SIMULATION: ${simPresetName ?: "Custom"} ($simFixtureCount fixtures)",
                            style = MaterialTheme.typography.bodySmall.copy(color = PixelDesign.colors.onSurface),
                        )
                    }
                }
            }

            // Edit Mode Indicator
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp),
            ) {
                Text(
                    text = "EDIT MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 8.sp,
                    ),
                    color = PixelDesign.colors.warning,
                    modifier = Modifier
                        .pixelBorder(width = 1.dp, color = PixelDesign.colors.warning.copy(alpha = 0.4f), pixelSize = 1.dp)
                        .background(Color(0x88000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // View Mode Dots
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ViewModeIndicatorDot(isActive = pagerState.currentPage == 0, label = "TOP")
                ViewModeIndicatorDot(isActive = pagerState.currentPage == 1, label = "FRONT")
            }

            // Edit Mode FAB
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

            // Fixture Info/Edit Overlay
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

            // Dialogs
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

            if (isNodeListOpen) {
                NodeListOverlay(
                    nodes = nodes,
                    currentTimeMs = currentTimeMs,
                    onDiagnose = { viewModel.diagnoseNode(it) },
                    onClose = { viewModel.toggleNodeList() }
                )
            }
        }
    }
}

@Composable
fun PixelTopBar(
    beatState: com.chromadmx.core.model.BeatState,
    masterDimmer: Float,
    nodes: List<com.chromadmx.networking.model.DmxNode>,
    currentTimeMs: Long,
    isEditMode: Boolean,
    onTapTempo: () -> Unit,
    onDimmerChange: (Float) -> Unit,
    onNodeHealthClick: () -> Unit,
    onEditModeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PixelDesign.colors.surface.copy(alpha = 0.95f))
            .pixelBorder(width = 1.dp, color = PixelDesign.colors.outlineVariant, pixelSize = 1.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Beat Bar
            BeatBar(
                beatState = beatState,
                onTapTempo = onTapTempo,
                modifier = Modifier.weight(1f),
            )

            // Dimmer
            MasterDimmerCompact(
                value = masterDimmer,
                onValueChange = onDimmerChange,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )

            // Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                NodeHealthCompact(
                    nodes = nodes,
                    currentTimeMs = currentTimeMs,
                    onClick = onNodeHealthClick
                )

                IconButton(
                    onClick = onEditModeClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Mode",
                        tint = if (isEditMode) PixelDesign.colors.warning else PixelDesign.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
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

@Composable
private fun ViewModeIndicatorDot(isActive: Boolean, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    if (isActive) PixelDesign.colors.primary else Color.White.copy(alpha = 0.2f),
                ),
        )
        if (isActive) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 6.sp,
                ),
                color = PixelDesign.colors.primary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
internal fun FixtureInfoOverlay(
    fixture: com.chromadmx.core.model.Fixture3D,
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
        backgroundColor = PixelDesign.colors.surface
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
                InfoLabel("ADDR", "U${fixture.fixture.universeId}/${fixture.fixture.channelStart}")
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

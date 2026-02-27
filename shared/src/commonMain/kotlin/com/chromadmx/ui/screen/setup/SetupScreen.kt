package com.chromadmx.ui.screen.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.DmxNode
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.simulation.rigs.PixelBarVRig
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelDropdown
import com.chromadmx.ui.components.PixelLoadingSpinner
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.components.PixelScaffold
import com.chromadmx.ui.components.SpinnerSize
import com.chromadmx.ui.state.GenreOption
import com.chromadmx.ui.state.SetupEvent
import com.chromadmx.ui.state.SetupStep
import com.chromadmx.ui.state.SetupUiState
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.util.presetDisplayName
import com.chromadmx.ui.viewmodel.SetupViewModel

/**
 * Consolidated setup screen managing the full onboarding flow.
 *
 * Two phases with animated crossfade:
 * 1. **Discovery** — SPLASH + NETWORK_DISCOVERY: scanning animation, discovered nodes, simulation fallback
 * 2. **Config** — FIXTURE_SCAN → VIBE_CHECK → STAGE_PREVIEW: step-specific content per step
 */
@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    PixelScaffold(
        modifier = modifier,
        topBar = {
            SetupTopBar(currentStep = state.currentStep)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PixelDesign.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = isDiscoveryPhase(state.currentStep),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "setup-phase-crossfade",
                modifier = Modifier.weight(1f),
            ) { isDiscovery ->
                if (isDiscovery) {
                    DiscoveryPhase(
                        state = state,
                        onEvent = viewModel::onEvent,
                    )
                } else {
                    ConfigPhase(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onComplete = onComplete,
                    )
                }
            }
        }
    }
}

// ── Phase Detection ─────────────────────────────────────────────────

private fun isDiscoveryPhase(step: SetupStep): Boolean =
    step in setOf(SetupStep.SPLASH, SetupStep.NETWORK_DISCOVERY)

// ── Top Bar ─────────────────────────────────────────────────────────

@Composable
private fun SetupTopBar(
    currentStep: SetupStep,
    modifier: Modifier = Modifier,
) {
    val steps = SetupStep.entries.filter { it != SetupStep.COMPLETE }
    val currentIndex = steps.indexOf(currentStep).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixelDesign.colors.surface.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SETUP",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 4.sp,
            ),
            color = PixelDesign.colors.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, _ ->
                val dotColor = when {
                    index < currentIndex -> PixelDesign.colors.success
                    index == currentIndex -> PixelDesign.colors.primary
                    else -> PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor),
                )
            }
        }
    }
}

// ── Phase 1: Discovery ──────────────────────────────────────────────

@Composable
private fun DiscoveryPhase(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.isScanning) {
            ScanningContent()
        } else if (state.discoveredNodes.isNotEmpty()) {
            NodesFoundContent(
                nodes = state.discoveredNodes,
                onEvent = onEvent,
            )
        } else {
            NoNodesContent(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun ScanningContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLoadingSpinner(
            spinnerSize = SpinnerSize.Large,
            color = PixelDesign.colors.info,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        Text(
            text = "Scanning for lights...",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.info,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        PixelProgressBar(
            progress = 0f,
            indeterminate = true,
            modifier = Modifier.fillMaxWidth(0.7f),
            progressColor = PixelDesign.colors.info,
        )
    }
}

@Composable
private fun NodesFoundContent(
    nodes: List<DmxNode>,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${nodes.size} node${if (nodes.size != 1) "s" else ""} found!",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.success,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(nodes, key = { it.nodeKey }) { node ->
                NodeCard(node)
            }
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        PixelButton(
            onClick = { onEvent(SetupEvent.Advance) },
            variant = PixelButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("CONTINUE")
        }
    }
}

@Composable
private fun NoNodesContent(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No lights found",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.warning,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        PixelCard(
            borderColor = PixelDesign.colors.secondary.copy(alpha = 0.8f),
        ) {
            Text(
                text = "No Art-Net nodes detected on the network. Try again or launch a virtual stage instead.",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelDesign.colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PixelButton(
                onClick = { onEvent(SetupEvent.RetryNetworkScan) },
                variant = PixelButtonVariant.Surface,
                modifier = Modifier.weight(1f),
            ) {
                Text("RETRY SCAN")
            }
            PixelButton(
                onClick = { onEvent(SetupEvent.EnterSimulationMode) },
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            ) {
                Text("VIRTUAL STAGE")
            }
        }
    }
}

// ── Phase 2: Config (step-specific content) ─────────────────────────

@Composable
private fun ConfigPhase(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.currentStep,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "config-step-crossfade",
        modifier = modifier.fillMaxSize(),
    ) { step ->
        when (step) {
            SetupStep.FIXTURE_SCAN -> FixtureScanContent(state = state, onEvent = onEvent)
            SetupStep.VIBE_CHECK -> VibeCheckContent(state = state, onEvent = onEvent)
            SetupStep.STAGE_PREVIEW -> StagePreviewContent(
                state = state,
                onEvent = onEvent,
                onComplete = onComplete,
            )
            else -> { /* SPLASH, NETWORK_DISCOVERY, COMPLETE handled elsewhere */ }
        }
    }
}

// ── FIXTURE_SCAN Step ───────────────────────────────────────────────

@Composable
private fun FixtureScanContent(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixtureCount = if (state.isSimulationMode) {
        state.simulationFixtureCount
    } else {
        state.fixturesLoadedCount
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "YOUR RIG",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 2.sp,
            ),
            color = PixelDesign.colors.info,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

        Text(
            text = "$fixtureCount fixture${if (fixtureCount != 1) "s" else ""} configured",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        // Stage preview — V-formation for PIXEL_BAR_V, generic grid otherwise
        PixelCard(borderColor = PixelDesign.colors.info.copy(alpha = 0.4f)) {
            if (state.selectedRigPreset == RigPreset.PIXEL_BAR_V) {
                VFormationCanvas(
                    activeFixtures = state.scanActiveFixtures,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            } else {
                FixturePreviewCanvas(
                    fixtureCount = fixtureCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        // Rig preset selector (simulation mode only)
        if (state.isSimulationMode) {
            RigPresetDropdown(
                selectedPreset = state.selectedRigPreset,
                onEvent = onEvent,
            )
            Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))
        }

        // Scan button for V-rig
        if (state.isSimulationMode && state.selectedRigPreset == RigPreset.PIXEL_BAR_V && !state.scanComplete) {
            PixelButton(
                onClick = { onEvent(SetupEvent.StartFixtureScan) },
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isScanningFixtures,
            ) {
                Text(if (state.isScanningFixtures) "SCANNING..." else "SCAN FIXTURES")
            }
            Spacer(modifier = Modifier.height(PixelDesign.spacing.small))
        }

        // Continue button
        PixelButton(
            onClick = { onEvent(SetupEvent.Advance) },
            variant = PixelButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("CONTINUE")
        }
    }
}

// ── VIBE_CHECK Step ─────────────────────────────────────────────────

@Composable
private fun VibeCheckContent(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "VIBE CHECK",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 2.sp,
            ),
            color = PixelDesign.colors.secondary,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

        Text(
            text = "Pick your sound",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        // Genre grid (2 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 340.dp),
        ) {
            items(state.availableGenres) { genre ->
                val isSelected = state.selectedGenre?.id == genre.id
                PixelCard(
                    borderColor = Color(genre.color).copy(alpha = if (isSelected) 0.9f else 0.3f),
                    glowing = isSelected,
                    onClick = { onEvent(SetupEvent.SelectGenre(genre)) },
                ) {
                    Text(
                        text = genre.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
                        color = if (isSelected) Color(genre.color) else PixelDesign.colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        PixelButton(
            onClick = { onEvent(SetupEvent.ConfirmGenre) },
            variant = PixelButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedGenre != null,
        ) {
            Text("CONTINUE")
        }
    }
}

// ── STAGE_PREVIEW Step ──────────────────────────────────────────────

@Composable
private fun StagePreviewContent(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixtureCount = if (state.isSimulationMode) {
        state.simulationFixtureCount
    } else {
        state.fixturesLoadedCount
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "YOUR STAGE",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 2.sp,
            ),
            color = PixelDesign.colors.info,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        PixelCard(borderColor = PixelDesign.colors.info.copy(alpha = 0.4f)) {
            if (state.selectedRigPreset == RigPreset.PIXEL_BAR_V) {
                VFormationCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            } else {
                FixturePreviewCanvas(
                    fixtureCount = fixtureCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        // Genre preset generation progress
        if (state.isGenerating || state.generationProgress > 0f) {
            val genreName = state.selectedGenre?.displayName ?: "Custom"
            val statusText = if (state.isGenerating) {
                "Generating $genreName presets..."
            } else {
                "$genreName presets ready!"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PixelFontFamily),
                color = if (state.isGenerating) PixelDesign.colors.secondary else PixelDesign.colors.success,
            )

            Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

            PixelProgressBar(
                progress = state.generationProgress,
                indeterminate = false,
                modifier = Modifier.fillMaxWidth(0.7f),
                progressColor = if (state.isGenerating) PixelDesign.colors.secondary else PixelDesign.colors.success,
            )

            Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

            Text(
                text = "${state.matchingPresetCount} presets",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
                color = PixelDesign.colors.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))
        }

        // Show generation error if any
        if (state.generationError != null) {
            Text(
                text = state.generationError,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
                color = PixelDesign.colors.warning,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(PixelDesign.spacing.small))
        }

        PixelButton(
            onClick = {
                onEvent(SetupEvent.SkipToComplete)
                onComplete()
            },
            variant = PixelButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("START SHOW")
        }
    }
}

// ── V-Formation Canvas ──────────────────────────────────────────────

/**
 * V-formation fixture preview showing 8 vertical pixel bars.
 * Active bars (from scan) glow brightly; inactive bars are dim.
 */
@Composable
private fun VFormationCanvas(
    activeFixtures: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    // Derive bar IDs and x-positions from the rig definition
    val barPositions = remember {
        PixelBarVRig.createFixtures().map { it.fixture.fixtureId to it.position.x }
    }

    // Capture theme colors for use inside Canvas DrawScope (non-composable)
    val infoColor = PixelDesign.colors.info
    val secondaryColor = PixelDesign.colors.secondary
    val successColor = PixelDesign.colors.success
    val scanlineColor = PixelDesign.colors.scanlineColor

    Canvas(
        modifier = modifier.background(PixelDesign.colors.background.copy(alpha = 0.8f)),
    ) {
        val pad = 24f
        val canvasW = size.width - 2 * pad
        val canvasH = size.height - 2 * pad
        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        // CRT scanlines
        var scanY = 0f
        while (scanY < size.height) {
            drawLine(
                color = scanlineColor,
                start = Offset(0f, scanY),
                end = Offset(size.width, scanY),
                strokeWidth = 1f,
            )
            scanY += 4f
        }

        // World-space ranges matching SimulatedFrameCapture
        val xMin = -3f
        val xMax = 3f
        val zMin = 0f
        val zMax = 2.5f
        val barBottomZ = 0.5f
        val barTopZ = 1.7f

        for ((barId, worldX) in barPositions) {
            val isActive = barId in activeFixtures
            val xFrac = (worldX - xMin) / (xMax - xMin)
            val screenX = pad + xFrac * canvasW

            // Draw 24 pixels bottom to top
            for (px in 0 until 24) {
                val t = px.toFloat() / 23f
                val worldZ = barBottomZ + t * (barTopZ - barBottomZ)
                val zFrac = (worldZ - zMin) / (zMax - zMin)
                val screenY = pad + (1f - zFrac) * canvasH

                val baseColor = when (px % 3) {
                    0 -> infoColor
                    1 -> secondaryColor
                    else -> successColor
                }

                val alpha = if (isActive) 0.95f else 0.25f
                val glowAlpha = if (isActive) 0.3f else 0.05f

                // Outer glow
                drawCircle(
                    color = baseColor.copy(alpha = glowAlpha),
                    radius = if (isActive) 10f else 6f,
                    center = Offset(screenX, screenY),
                )
                // Inner dot
                drawCircle(
                    color = baseColor.copy(alpha = alpha),
                    radius = 3f,
                    center = Offset(screenX, screenY),
                )
            }
        }
    }
}

// ── Shared Sub-composables ──────────────────────────────────────────

@Composable
private fun NodeCard(
    node: DmxNode,
    modifier: Modifier = Modifier,
) {
    PixelCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = PixelDesign.colors.success.copy(alpha = 0.4f),
        glowing = true,
    ) {
        Column {
            Text(
                text = node.shortName.ifBlank { node.ipAddress },
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
                color = PixelDesign.colors.info,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
            ) {
                Text(
                    text = node.ipAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelDesign.colors.onSurfaceVariant,
                )
                if (node.universes.isNotEmpty()) {
                    Text(
                        text = "U${node.universes.joinToString(",")}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
                        color = PixelDesign.colors.secondary.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RigPresetDropdown(
    selectedPreset: RigPreset,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = RigPreset.entries
    val presetNames = remember { presets.map { it.presetDisplayName() } }
    val selectedIndex = presets.indexOf(selectedPreset)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "RIG PRESET",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 2.sp,
            ),
            color = PixelDesign.colors.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

        PixelDropdown(
            items = presetNames,
            selectedIndex = selectedIndex,
            onItemSelected = { index ->
                onEvent(SetupEvent.SelectRigPreset(presets[index]))
            },
            modifier = Modifier.fillMaxWidth(0.6f),
        )

        val rig = remember(selectedPreset) { SimulatedFixtureRig(selectedPreset) }
        Spacer(modifier = Modifier.height(PixelDesign.spacing.extraSmall))
        Text(
            text = "${rig.fixtureCount} fixtures",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun FixturePreviewCanvas(
    fixtureCount: Int,
    modifier: Modifier = Modifier,
) {
    // Capture theme colors for use inside Canvas DrawScope (non-composable)
    val infoColor = PixelDesign.colors.info
    val secondaryColor = PixelDesign.colors.secondary
    val successColor = PixelDesign.colors.success
    val scanlineColor = PixelDesign.colors.scanlineColor

    Canvas(
        modifier = modifier.background(PixelDesign.colors.background.copy(alpha = 0.8f)),
    ) {
        val pad = 20f
        val canvasW = size.width - 2 * pad
        val canvasH = size.height - 2 * pad
        if (canvasW <= 0f || canvasH <= 0f || fixtureCount <= 0) return@Canvas

        val scanLineSpacing = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = scanlineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += scanLineSpacing
        }

        val cols = when {
            fixtureCount <= 8 -> fixtureCount
            fixtureCount <= 30 -> 10
            else -> 12
        }
        val rows = (fixtureCount + cols - 1) / cols

        for (i in 0 until fixtureCount) {
            val col = i % cols
            val row = i / cols

            val cx = pad + (col + 0.5f) * canvasW / cols
            val cy = pad + (row + 0.5f) * canvasH / rows.coerceAtLeast(1)

            val fixtureColor = when (i % 3) {
                0 -> infoColor
                1 -> secondaryColor
                else -> successColor
            }

            drawCircle(
                color = fixtureColor.copy(alpha = 0.2f),
                radius = 14f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = fixtureColor.copy(alpha = 0.8f),
                radius = 5f,
                center = Offset(cx, cy),
            )
        }
    }
}

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelDropdown
import com.chromadmx.ui.components.PixelLoadingSpinner
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.components.PixelScaffold
import com.chromadmx.ui.components.SpinnerSize
import com.chromadmx.ui.state.SetupEvent
import com.chromadmx.ui.state.SetupStep
import com.chromadmx.ui.state.SetupUiState
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.NeonYellow
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.util.presetDisplayName
import com.chromadmx.ui.viewmodel.SetupViewModel

/**
 * Consolidated setup screen that replaces the multi-screen onboarding flow.
 *
 * Two phases with animated crossfade:
 * 1. **Discovery** — scanning animation, discovered nodes list, simulation fallback
 * 2. **Fixture Config** — fixture preview, rig preset selector, "Start Show" button
 *
 * @param viewModel The [SetupViewModel] managing the setup state.
 * @param onComplete Called when the user taps "Start Show" to enter the main app.
 * @param modifier Optional [Modifier] for the root layout.
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
                    FixtureConfigPhase(
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
    step in setOf(SetupStep.SPLASH, SetupStep.NETWORK_DISCOVERY, SetupStep.FIXTURE_SCAN)

// ── Top Bar ─────────────────────────────────────────────────────────

/**
 * Simple centered title with step indicator dots.
 */
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

        // Step indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, _ ->
                val dotColor = when {
                    index < currentIndex -> NeonGreen
                    index == currentIndex -> PixelDesign.colors.primary
                    else -> PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.3f)
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor),
                )
            }
        }
    }
}

// ── Phase 1: Discovery ──────────────────────────────────────────────

/**
 * Discovery phase content: scanning animation, node list, and simulation fallback.
 */
@Composable
private fun DiscoveryPhase(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        if (state.isScanning) {
            // Scanning state
            ScanningContent()
        } else if (state.discoveredNodes.isNotEmpty()) {
            // Nodes found
            NodesFoundContent(
                nodes = state.discoveredNodes,
                onEvent = onEvent,
            )
        } else {
            // No nodes found / idle
            NoNodesContent(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

/**
 * Active scanning state: spinner, status text, and indeterminate progress bar.
 */
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
            color = NeonCyan,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        Text(
            text = "Scanning for lights...",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = NeonCyan,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        PixelProgressBar(
            progress = 0f,
            indeterminate = true,
            modifier = Modifier.fillMaxWidth(0.7f),
            progressColor = NeonCyan,
        )
    }
}

/**
 * Nodes discovered: count header + scrollable node list + advance button.
 */
@Composable
private fun NodesFoundContent(
    nodes: List<DmxNode>,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${nodes.size} node${if (nodes.size != 1) "s" else ""} found!",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = NeonGreen,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
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

/**
 * No nodes found: prompt to retry or enter simulation mode.
 */
@Composable
private fun NoNodesContent(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No lights found",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = NeonYellow,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        PixelCard(
            borderColor = NeonMagenta.copy(alpha = 0.5f),
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

        // Simulation mode rig preset selector (visible when in simulation mode)
        if (state.isSimulationMode) {
            RigPresetSelector(
                selectedPreset = state.selectedRigPreset,
                onEvent = onEvent,
            )

            Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

            PixelButton(
                onClick = { onEvent(SetupEvent.Advance) },
                variant = PixelButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("CONTINUE WITH SIMULATION")
            }
        } else {
            // Action buttons: Retry + Enter Simulation
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
}

// ── Phase 2: Fixture Config ─────────────────────────────────────────

/**
 * Fixture configuration phase: preview canvas, fixture count, rig selector, and start button.
 */
@Composable
private fun FixtureConfigPhase(
    state: SetupUiState,
    onEvent: (SetupEvent) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        Text(
            text = "YOUR STAGE",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 2.sp,
            ),
            color = NeonCyan,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

        // Fixture count indicator
        val fixtureCount = if (state.isSimulationMode) {
            state.simulationFixtureCount
        } else {
            state.fixturesLoadedCount
        }
        Text(
            text = "$fixtureCount fixture${if (fixtureCount != 1) "s" else ""} configured",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        // Stage preview canvas — simplified fixture dot visualization
        FixturePreviewCanvas(
            fixtureCount = fixtureCount,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(220.dp),
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        // Rig preset selector (simulation mode only)
        if (state.isSimulationMode) {
            RigPresetSelector(
                selectedPreset = state.selectedRigPreset,
                onEvent = onEvent,
            )
            Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))
        }

        // Start Show button
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

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))
    }
}

// ── Shared Sub-composables ──────────────────────────────────────────

/**
 * A PixelCard displaying a discovered DMX node's name, IP, and universe info.
 */
@Composable
private fun NodeCard(
    node: DmxNode,
    modifier: Modifier = Modifier,
) {
    PixelCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = NeonGreen.copy(alpha = 0.4f),
        glowing = true,
    ) {
        Column {
            Text(
                text = node.shortName.ifBlank { node.ipAddress },
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
                color = NeonCyan,
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
                        color = NeonMagenta.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/**
 * Dropdown selector for rig presets (Small DJ, Truss Rig, Festival Stage).
 */
@Composable
private fun RigPresetSelector(
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

        // Fixture count for selected preset
        val rig = remember(selectedPreset) { SimulatedFixtureRig(selectedPreset) }
        Spacer(modifier = Modifier.height(PixelDesign.spacing.extraSmall))
        Text(
            text = "${rig.fixtureCount} fixtures",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
            color = PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

/**
 * Simplified fixture preview canvas showing fixture dots in a grid layout.
 * This serves as the top-down fixture preview for setup; the full interactive
 * VenueCanvas is used on the main stage screen.
 */
@Composable
private fun FixturePreviewCanvas(
    fixtureCount: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.background(PixelDesign.colors.background.copy(alpha = 0.8f)),
    ) {
        val pad = 20f
        val canvasW = size.width - 2 * pad
        val canvasH = size.height - 2 * pad
        if (canvasW <= 0f || canvasH <= 0f || fixtureCount <= 0) return@Canvas

        // CRT scanlines for pixel aesthetic
        val scanLineSpacing = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += scanLineSpacing
        }

        // Grid layout for fixtures
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
                0 -> Color(0xFF00FBFF)
                1 -> Color(0xFFFF00FF)
                else -> Color(0xFF00FF00)
            }

            // Outer glow
            drawCircle(
                color = fixtureColor.copy(alpha = 0.2f),
                radius = 14f,
                center = Offset(cx, cy),
            )
            // Inner bright point
            drawCircle(
                color = fixtureColor.copy(alpha = 0.8f),
                radius = 5f,
                center = Offset(cx, cy),
            )
        }
    }
}

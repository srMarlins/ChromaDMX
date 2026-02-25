package com.chromadmx.ui.screen.stage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.SimulationBadge
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.components.beat.BeatBar
import com.chromadmx.ui.viewmodel.StageViewModel

/**
 * Main stage preview screen -- the primary screen users interact with.
 *
 * Shows the venue canvas with fixture colors, preset strip at bottom,
 * BPM/network info at top, and settings gear icon.
 *
 * When simulation mode is active, a pulsing "SIMULATION" badge appears
 * in the top-left corner. Tapping it shows an info tooltip.
 */
@Composable
fun StagePreviewScreen(
    viewModel: StageViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixtures by viewModel.fixtures.collectAsState()
    val beatState by viewModel.beatState.collectAsState()
    val masterDimmer by viewModel.masterDimmer.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val simPresetName by viewModel.simulationPresetName.collectAsState()
    val simFixtureCount by viewModel.simulationFixtureCount.collectAsState()

    var showSimTooltip by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Main venue canvas
        VenueCanvas(
            fixtures = fixtures,
            fixtureColors = emptyList(), // TODO: wire to engine output
            modifier = Modifier.fillMaxSize()
        )

        // Top bar: beat visualization + settings
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Beat bar with BPM, phase indicators (takes most of the width)
                BeatBar(
                    beatState = beatState,
                    onTapTempo = { viewModel.tap() },
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Simulation badge + tooltip (top-left, below BPM)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 56.dp)
        ) {
            AnimatedVisibility(
                visible = isSimulationMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SimulationBadge(
                    onTap = { showSimTooltip = !showSimTooltip },
                )
            }
            AnimatedVisibility(
                visible = showSimTooltip && isSimulationMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val tooltipText = buildString {
                    append("Running with virtual fixtures")
                    if (simPresetName != null) {
                        append(" ($simPresetName, $simFixtureCount fixtures)")
                    }
                }
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        // Master dimmer (bottom-left)
        Text(
            text = "Dimmer: ${(masterDimmer * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}

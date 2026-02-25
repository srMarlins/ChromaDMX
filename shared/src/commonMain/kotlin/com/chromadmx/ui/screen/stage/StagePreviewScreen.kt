package com.chromadmx.ui.screen.stage

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.components.beat.BeatBar
import com.chromadmx.ui.viewmodel.StageViewModel

/**
 * Main stage preview screen -- the primary screen users interact with.
 *
 * Shows the venue canvas with fixture colors, preset strip at bottom,
 * BPM/network info at top, and settings gear icon.
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

        // Master dimmer (bottom-left)
        Text(
            text = "Dimmer: ${(masterDimmer * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}

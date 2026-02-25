package com.chromadmx.ui.screen.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
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
import com.chromadmx.ui.screen.settings.SettingsScreen
import com.chromadmx.ui.viewmodel.MapViewModel
import com.chromadmx.ui.viewmodel.SettingsViewModel
import org.koin.compose.getKoin

/**
 * Map screen with fixture position editor and fixture list.
 *
 * On wider screens, shows a split layout: canvas editor on left,
 * fixture list on right. On narrow screens, stacks vertically.
 *
 * For the initial build, uses a vertical layout (top: editor, bottom: list).
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
) {
    val fixtures by viewModel.fixtures.collectAsState()
    val selectedIndex by viewModel.selectedFixtureIndex.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fixture Map",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )

                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }

            // Editor (top half)
            FixturePositionEditor(
                fixtures = fixtures,
                selectedIndex = selectedIndex,
                onUpdatePosition = { index, pos ->
                    viewModel.updateFixturePosition(index, pos)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp,
            )

            // Fixture list (bottom half)
            FixtureListPanel(
                fixtures = fixtures,
                selectedIndex = selectedIndex,
                onSelectFixture = { viewModel.selectFixture(it) },
                onRemoveFixture = { viewModel.removeFixture(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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

            SettingsScreen(
                viewModel = settingsVm,
                onClose = { showSettings = false }
            )
        }
    }
}

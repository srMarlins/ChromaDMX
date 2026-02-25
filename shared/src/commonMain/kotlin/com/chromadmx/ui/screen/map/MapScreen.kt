package com.chromadmx.ui.screen.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.viewmodel.MapViewModel

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

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = "Fixture Map",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

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
}

package com.chromadmx.ui.screen.perform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Horizontal row of scene preset buttons (visual only).
 * TODO: Wire to SceneStore for load/save. Currently tracks selection state locally.
 */
@Composable
fun ScenePresetRow(
    presetCount: Int = 8,
    activePreset: Int? = null,
    onPresetTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed((0 until presetCount).toList()) { index, _ ->
            val isActive = index == activePreset
            OutlinedButton(
                onClick = { onPresetTap(index) },
                modifier = Modifier.width(56.dp).height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

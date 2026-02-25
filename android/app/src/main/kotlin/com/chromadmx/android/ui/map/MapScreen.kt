package com.chromadmx.android.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Map tab — camera preview, scan controls, fixture placement editor.
 *
 * Currently a placeholder; will host the CameraX preview and OpenGL venue
 * visualization once those modules are integrated.
 */
@Composable
fun MapScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Map",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Camera preview, scan controls, fixture editor",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

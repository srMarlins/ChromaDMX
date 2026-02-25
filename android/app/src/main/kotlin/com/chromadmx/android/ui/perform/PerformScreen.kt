package com.chromadmx.android.ui.perform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Perform tab — effect controls, beat visualization, master dimmer, scene presets.
 *
 * Currently a placeholder; will be wired to EffectEngine and BeatClock once shared modules
 * are integrated.
 */
@Composable
fun PerformScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Perform",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Effect controls, beat viz, master dimmer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package com.chromadmx.android.ui.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Network tab — Art-Net node list, connection status, universe mapping.
 *
 * Currently a placeholder; will be wired to NodeDiscovery and DMXTransport once
 * the networking module is integrated.
 */
@Composable
fun NetworkScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Network",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Node list, status, universe mapping",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

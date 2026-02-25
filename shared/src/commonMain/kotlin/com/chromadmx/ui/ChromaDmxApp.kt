package com.chromadmx.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.chromadmx.ui.navigation.Screen
import com.chromadmx.ui.theme.ChromaDmxTheme

/**
 * Root composable for the ChromaDMX application.
 *
 * Hosts the bottom navigation bar and delegates to per-screen composables.
 * Uses simple enum-based navigation (no Jetpack Navigation) for KMP compatibility.
 */
@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        var currentScreen by remember { mutableStateOf(Screen.PERFORM) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screenIcon(screen),
                                    contentDescription = screen.title,
                                )
                            },
                            label = { Text(screen.title) },
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                when (currentScreen) {
                    Screen.PERFORM -> ScreenPlaceholder("Perform", "Effect controls, beat viz, master dimmer")
                    Screen.NETWORK -> ScreenPlaceholder("Network", "Node list, status, universe mapping")
                    Screen.MAP -> ScreenPlaceholder("Map", "Camera preview, scan controls, fixture editor")
                    Screen.AGENT -> ScreenPlaceholder("Agent", "Chat interface, tool visualization")
                }
            }
        }
    }
}

@Composable
private fun ScreenPlaceholder(title: String, subtitle: String) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun screenIcon(screen: Screen): ImageVector = when (screen) {
    Screen.PERFORM -> Icons.Default.PlayArrow
    Screen.NETWORK -> Icons.Default.Settings
    Screen.MAP -> Icons.Default.Place
    Screen.AGENT -> Icons.Default.Build
}

package com.chromadmx.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing each top-level screen / tab in the app.
 *
 * @property route Navigation route string.
 * @property title Display label shown in the bottom bar.
 * @property icon Material icon for the tab.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    /** Effect controls, beat viz, master dimmer, scene presets. */
    data object Perform : Screen(
        route = "perform",
        title = "Perform",
        icon = Icons.Default.PlayArrow,
    )

    /** Node list, status, universe mapping. */
    data object Network : Screen(
        route = "network",
        title = "Network",
        icon = Icons.Default.Settings,
    )

    /** Camera preview, scan controls, fixture editor. */
    data object Map : Screen(
        route = "map",
        title = "Map",
        icon = Icons.Default.Place,
    )

    /** Chat interface, tool visualization. */
    data object Agent : Screen(
        route = "agent",
        title = "Agent",
        icon = Icons.Default.Build,
    )

    companion object {
        /** Ordered list of all tabs for the bottom navigation bar. */
        val tabs: List<Screen> = listOf(Perform, Network, Map, Agent)
    }
}

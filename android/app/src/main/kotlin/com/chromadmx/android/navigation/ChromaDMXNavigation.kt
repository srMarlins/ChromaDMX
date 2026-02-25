package com.chromadmx.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chromadmx.android.ui.agent.AgentScreen
import com.chromadmx.android.ui.map.MapScreen
import com.chromadmx.android.ui.network.NetworkScreen
import com.chromadmx.android.ui.perform.PerformScreen

/**
 * Top-level composable that hosts the bottom navigation bar and screen content.
 *
 * Uses Jetpack Navigation Compose with four tabs: Perform, Network, Map, Agent.
 */
@Composable
fun ChromaDMXNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.tabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination to avoid building up a large stack
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Perform.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Perform.route) { PerformScreen() }
            composable(Screen.Network.route) { NetworkScreen() }
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Agent.route) { AgentScreen() }
        }
    }
}

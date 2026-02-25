package com.chromadmx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.chromadmx.ui.navigation.Screen
import com.chromadmx.ui.screen.agent.AgentScreen
import com.chromadmx.ui.screen.agent.MascotAlert
import com.chromadmx.ui.screen.map.MapScreen
import com.chromadmx.ui.screen.network.NetworkHealthTopBar
import com.chromadmx.ui.screen.network.NetworkScreen
import com.chromadmx.ui.screen.perform.PerformScreen
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.viewmodel.AgentViewModel
import com.chromadmx.ui.viewmodel.MapViewModel
import com.chromadmx.ui.viewmodel.NetworkViewModel
import com.chromadmx.ui.viewmodel.PerformViewModel
import org.koin.compose.getKoin

/**
 * Root composable for the ChromaDMX application.
 *
 * Hosts the bottom navigation bar and delegates to per-screen composables.
 * Uses simple enum-based navigation (no Jetpack Navigation) for KMP compatibility.
 *
 * ViewModels are resolved from Koin. Screens whose ViewModel dependencies
 * are not yet registered (EffectEngine, NodeDiscovery) will show a placeholder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaDmxApp() {
    ChromaDmxTheme {
        var currentScreen by remember { mutableStateOf(Screen.PERFORM) }
        val networkVm = resolveOrNull<NetworkViewModel>()
        if (networkVm != null) {
            DisposableEffect(networkVm) {
                onDispose { networkVm.onCleared() }
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "ChromaDMX",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    actions = {
                        if (networkVm != null) {
                            NetworkHealthTopBar(viewModel = networkVm)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            },
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
            ) {
                when (currentScreen) {
                    Screen.PERFORM -> {
                        val vm = resolveOrNull<PerformViewModel>()
                        if (vm != null) {
                            DisposableEffect(vm) {
                                onDispose { vm.onCleared() }
                            }
                            PerformScreen(viewModel = vm)
                        } else {
                            ScreenPlaceholder("Perform", "Engine services not yet registered in DI.")
                        }
                    }
                    Screen.NETWORK -> {
                        if (networkVm != null) {
                            NetworkScreen(viewModel = networkVm)
                        } else {
                            ScreenPlaceholder("Network", "Networking services not yet registered in DI.")
                        }
                    }
                    Screen.MAP -> {
                        val vm = resolveOrNull<MapViewModel>()
                        if (vm != null) {
                            DisposableEffect(vm) {
                                onDispose { vm.onCleared() }
                            }
                            MapScreen(viewModel = vm)
                        } else {
                            ScreenPlaceholder("Map", "Map services not yet registered in DI.")
                        }
                    }
                    Screen.AGENT -> {
                        val vm = resolveOrNull<AgentViewModel>()
                        if (vm != null) {
                            DisposableEffect(vm) {
                                onDispose { vm.onCleared() }
                            }
                            AgentScreen(viewModel = vm)
                        } else {
                            ScreenPlaceholder("Agent", "Agent services not yet registered in DI.")
                        }
                    }
                }

                // Mascot Alerts (Global overlay)
                if (networkVm != null) {
                    val alert by networkVm.activeAlert.collectAsState()
                    MascotAlert(
                        alert = alert,
                        onDiagnose = { networkVm.diagnoseNode(it) },
                        onDismiss = { networkVm.dismissAlert() }
                    )
                }
            }
        }
    }
}

/**
 * Safely resolve a dependency from Koin, returning null if not available.
 * The result is remembered so the same ViewModel instance is reused
 * across recompositions within the same composition.
 */
@Composable
private inline fun <reified T : Any> resolveOrNull(): T? {
    val koin = getKoin()
    return remember { runCatching { koin.get<T>() }.getOrNull() }
}

@Composable
private fun ScreenPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
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

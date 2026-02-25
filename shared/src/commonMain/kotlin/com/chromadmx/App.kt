package com.chromadmx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Root composable for ChromaDMX, shared across Android and iOS via Compose Multiplatform.
 *
 * This composable provides the top-level Material theme and the initial screen content.
 * As the app evolves, this will host the bottom tab navigation (Perform, Network, Map, Agent)
 * and the Koin dependency injection scope.
 *
 * On Android, this is called from MainActivity via setContent.
 * On iOS, this is called from MainViewController via ComposeUIViewController.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Greeting().greet(),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

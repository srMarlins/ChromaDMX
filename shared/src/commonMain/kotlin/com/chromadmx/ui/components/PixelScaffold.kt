package com.chromadmx.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity

/**
 * A layout structure that mimics the standard Scaffold but with pixel-art specific constraints.
 * It places the top bar and bottom bar, and fills the remaining space with content.
 * Respects system bar insets (status bar at top, navigation bar at bottom).
 */
@Composable
fun PixelScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val navBarBottom = WindowInsets.navigationBars.getBottom(density)

    PixelSurface(modifier = modifier.fillMaxSize()) {
        SubcomposeLayout { constraints ->
            val layoutWidth = constraints.maxWidth
            val layoutHeight = constraints.maxHeight

            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

            // Measure Top Bar
            val topBarPlaceables = subcompose(ScaffoldLayoutContent.TopBar, topBar).map {
                it.measure(looseConstraints)
            }
            val topBarHeight = topBarPlaceables.maxOfOrNull { it.height } ?: 0

            // Measure Bottom Bar
            val bottomBarPlaceables = subcompose(ScaffoldLayoutContent.BottomBar, bottomBar).map {
                it.measure(looseConstraints)
            }
            val bottomBarHeight = bottomBarPlaceables.maxOfOrNull { it.height } ?: 0

            // Total occupied height includes system bar insets
            val topOccupied = statusBarTop + topBarHeight
            val bottomOccupied = navBarBottom + bottomBarHeight

            val contentConstraints = constraints.copy(minHeight = 0)

            val contentPlaceables = subcompose(ScaffoldLayoutContent.MainContent) {
                content(PaddingValues(top = topOccupied.toDp(), bottom = bottomOccupied.toDp()))
            }.map { it.measure(contentConstraints) }

            layout(layoutWidth, layoutHeight) {
                contentPlaceables.forEach { it.place(0, 0) }

                // Place Top Bar below the status bar
                topBarPlaceables.forEach { it.place(0, statusBarTop) }

                // Place Bottom Bar above the navigation bar
                bottomBarPlaceables.forEach {
                    it.place(0, layoutHeight - bottomBarHeight - navBarBottom)
                }
            }
        }
    }
}

private enum class ScaffoldLayoutContent { TopBar, MainContent, BottomBar }

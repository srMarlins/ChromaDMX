package com.chromadmx.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp

/**
 * A layout structure that mimics the standard Scaffold but with pixel-art specific constraints.
 * It places the top bar and bottom bar, and fills the remaining space with content.
 */
@Composable
fun PixelScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
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

            // Measure Content
            // We pass full height constraints but provide padding values so content *knows* where to avoid.
            // This mimics Scaffold behavior where content is placed at (0,0) usually.
            val contentConstraints = constraints.copy(minHeight = 0)

            val contentPlaceables = subcompose(ScaffoldLayoutContent.MainContent) {
                content(PaddingValues(top = topBarHeight.toDp(), bottom = bottomBarHeight.toDp()))
            }.map { it.measure(contentConstraints) }

            layout(layoutWidth, layoutHeight) {
                // Place Content at (0,0) because it has internal padding applied via PaddingValues
                contentPlaceables.forEach { it.place(0, 0) }

                // Place Top Bar on top of content (z-order wise, though order here is draw order)
                topBarPlaceables.forEach { it.place(0, 0) }

                // Place Bottom Bar
                bottomBarPlaceables.forEach { it.place(0, layoutHeight - bottomBarHeight) }
            }
        }
    }
}

private enum class ScaffoldLayoutContent { TopBar, MainContent, BottomBar }

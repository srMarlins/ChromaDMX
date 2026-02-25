package com.chromadmx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DmxDarkColorScheme = darkColorScheme(
    primary = DmxPrimary,
    onPrimary = DmxOnPrimary,
    primaryContainer = DmxPrimaryContainer,
    secondary = DmxSecondary,
    onSecondary = DmxOnSecondary,
    background = DmxBackground,
    surface = DmxSurface,
    surfaceVariant = DmxSurfaceVariant,
    onBackground = DmxOnBackground,
    onSurface = DmxOnSurface,
    error = DmxError,
    onError = DmxOnError,
)

@Composable
fun ChromaDmxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DmxDarkColorScheme,
        typography = DmxTypography,
        content = content,
    )
}

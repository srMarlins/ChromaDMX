package com.chromadmx.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme — optimized for dark stage environments (DMX/Stage themed)
val DmxBackground = Color(0xFF000000)     // Pure black
val DmxSurface = Color(0xFF121212)        // Very dark gray
val DmxSurfaceVariant = Color(0xFF1E1E1E) // Dark gray

// Neon Pixel Colors (DMX Vibes)
val NeonCyan = Color(0xFF00FBFF)
val NeonMagenta = Color(0xFFFF00FF)
val NeonGreen = Color(0xFF00FF00)
val NeonYellow = Color(0xFFFFFF00)
val NeonPurple = Color(0xFF6C63FF)

// Semantic colors
val DmxPrimary = NeonCyan
val DmxOnPrimary = Color(0xFF000000)
val DmxPrimaryContainer = Color(0xFF003F40)

val DmxSecondary = NeonMagenta
val DmxOnSecondary = Color(0xFF000000)

val DmxAccentGreen = NeonGreen
val DmxAccentYellow = NeonYellow

val DmxOnBackground = Color(0xFFE0E0E0)
val DmxOnSurface = Color(0xFFE0E0E0)
val DmxError = Color(0xFFFF5252)
val DmxOnError = Color(0xFFFFFFFF)

// Status colors for node health
val NodeOnline = NeonGreen
val NodeWarning = NeonYellow
val NodeOffline = Color(0xFFFF5252)
val NodeUnknown = Color(0xFF9E9E9E)

// Beat visualization colors
val BeatActive = NeonCyan
val BeatInactive = Color(0xFF1E1E1E)

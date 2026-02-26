package com.chromadmx.ui.navigation

/**
 * The 4 screens in the app navigation.
 */
sealed interface AppScreen {
    data object Setup : AppScreen
    data object Stage : AppScreen
    data object Settings : AppScreen
    data object Provisioning : AppScreen
}

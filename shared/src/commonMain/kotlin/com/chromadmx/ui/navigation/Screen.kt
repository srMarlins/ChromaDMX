package com.chromadmx.ui.navigation

/**
 * Top-level screens in the ChromaDMX app, used for bottom navigation.
 */
enum class Screen(val route: String, val title: String) {
    PERFORM("perform", "Perform"),
    NETWORK("network", "Network"),
    MAP("map", "Map"),
    AGENT("agent", "Agent"),
}

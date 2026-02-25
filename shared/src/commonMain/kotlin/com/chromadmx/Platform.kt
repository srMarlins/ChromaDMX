package com.chromadmx

/**
 * Platform abstraction for ChromaDMX.
 * Each platform provides its own implementation via `actual` declarations.
 */
expect class Platform() {
    val name: String
}

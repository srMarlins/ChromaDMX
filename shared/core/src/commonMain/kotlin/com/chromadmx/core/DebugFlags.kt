package com.chromadmx.core

/**
 * Runtime debug flags, safe-by-default (all false).
 *
 * Call [isDebugBuild] = true from your platform Application/AppDelegate
 * init to enable debug-only features (e.g. subscription tier selector).
 */
object DebugFlags {
    /** Set from platform init; defaults to false so release builds are safe. */
    var isDebugBuild: Boolean = false
}

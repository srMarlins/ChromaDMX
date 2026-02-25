package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/**
 * Standard compositing blend modes for layering color effects.
 */
@Serializable
enum class BlendMode {
    /** Use overlay color directly (respecting opacity). */
    NORMAL,
    /** Brighten: clamp(base + overlay). */
    ADDITIVE,
    /** Darken: base * overlay. */
    MULTIPLY,
    /** Contrast-preserving: multiply darks, screen lights. */
    OVERLAY
}

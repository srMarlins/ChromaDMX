package com.chromadmx.ui.mascot

/**
 * Mascot animation states.
 */
enum class MascotState {
    IDLE,      // Breathing animation, beat-reactive pulse
    THINKING,  // Spinning pixel dots (agent processing)
    HAPPY,     // Jump + sparkle (success events)
    ALERT,     // Exclamation bubble (warnings)
    CONFUSED,  // Head tilt + question bubble
    DANCING    // Beat-synced movement (performance mode)
}

/**
 * A single 16x16 pixel frame.
 *
 * Each pixel is an ARGB color int (0 = transparent).
 * Row-major: pixels[row][col], row 0 is top.
 */
data class SpriteFrame(
    val pixels: Array<IntArray>
) {
    val height: Int get() = pixels.size
    val width: Int get() = if (pixels.isNotEmpty()) pixels[0].size else 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpriteFrame) return false
        return pixels.contentDeepEquals(other.pixels)
    }

    override fun hashCode(): Int = pixels.contentDeepHashCode()
}

/**
 * A sequence of frames for one animation state.
 *
 * @property state The mascot state this animation represents.
 * @property frames Ordered list of sprite frames.
 * @property frameDurationMs Milliseconds per frame.
 * @property loop Whether the animation loops or clamps to the last frame.
 */
data class AnimationSequence(
    val state: MascotState,
    val frames: List<SpriteFrame>,
    val frameDurationMs: Long = 200L,
    val loop: Boolean = true
) {
    /**
     * Get the frame at a given index, handling looping or clamping.
     */
    fun frameAt(index: Int): SpriteFrame {
        if (frames.isEmpty()) error("Animation has no frames")
        return if (loop) {
            frames[index % frames.size]
        } else {
            frames[index.coerceAtMost(frames.lastIndex)]
        }
    }

    /** Total duration in milliseconds for one cycle. */
    val cycleDurationMs: Long get() = frames.size * frameDurationMs
}

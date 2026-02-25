package com.chromadmx.ui.util

/**
 * Metadata for the mascot sprite sheet.
 * The sprite sheet is a 6x4 grid of 16x16 frames.
 */
object MascotSpriteData {
    const val FRAME_SIZE = 16
    const val COLS = 6
    const val ROWS = 4

    enum class State(val frameIndices: List<Int>) {
        IDLE(listOf(0, 1, 2, 3)),
        THINKING(listOf(6, 7, 8, 9)),
        HAPPY(listOf(12, 13, 14, 15)),
        ALERT(listOf(18, 19, 20)),
        CONFUSED(listOf(21, 22, 23)),
        DANCING(listOf(4, 5, 10, 11, 16, 17))
    }

    /**
     * Returns the x and y pixel coordinates for a given frame index.
     */
    fun getFrameRect(index: Int): Pair<Int, Int> {
        val x = (index % COLS) * FRAME_SIZE
        val y = (index / COLS) * FRAME_SIZE
        return x to y
    }
}

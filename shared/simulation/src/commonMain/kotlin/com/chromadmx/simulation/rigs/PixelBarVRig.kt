package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * 8 vertical 24-pixel RGB bars in a V-formation.
 *
 * Performer stands at the tip of the V. 4 bars fan out on each side.
 * DMX addresses are intentionally scrambled to test vision-based
 * position mapping.
 *
 * Bar spec: 24 pixels x 3ch (RGB) = 72 DMX channels each.
 * Total: 576 channels across 2 universes.
 */
object PixelBarVRig {

    const val BAR_COUNT = 8
    const val PIXELS_PER_BAR = 24
    const val CHANNELS_PER_PIXEL = 3
    const val CHANNELS_PER_BAR = PIXELS_PER_BAR * CHANNELS_PER_PIXEL

    private const val BAR_BOTTOM_Z = 0.5f
    private const val BAR_TOP_Z = 1.7f

    /**
     * Physical positions (x, y) per bar index 0-7.
     * Left arm: bars 0-3 (far to near performer).
     * Right arm: bars 4-7 (near to far performer).
     */
    private val BAR_POSITIONS = listOf(
        -2.0f to 3.0f,  // bar 1: far left
        -1.5f to 2.3f,  // bar 2
        -1.0f to 1.5f,  // bar 3
        -0.5f to 0.5f,  // bar 4: near performer left
         0.5f to 0.5f,  // bar 5: near performer right
         1.0f to 1.5f,  // bar 6
         1.5f to 2.3f,  // bar 7
         2.0f to 3.0f,  // bar 8: far right
    )

    /**
     * Scrambled DMX assignments: (channelStart, universeId) per physical bar.
     * Intentionally out of order to test vision-based position mapping.
     */
    private val DMX_ASSIGNMENTS = listOf(
        288 to 1,  // bar 1 -> uni 1, ch 288
          0 to 0,  // bar 2 -> uni 0, ch 0
        360 to 1,  // bar 3 -> uni 1, ch 360
        144 to 0,  // bar 4 -> uni 0, ch 144
        432 to 1,  // bar 5 -> uni 1, ch 432
         72 to 0,  // bar 6 -> uni 0, ch 72
        216 to 0,  // bar 7 -> uni 0, ch 216
        504 to 1,  // bar 8 -> uni 1, ch 504
    )

    fun createFixtures(): List<Fixture3D> {
        return (0 until BAR_COUNT).map { i ->
            val (x, y) = BAR_POSITIONS[i]
            val (channelStart, universe) = DMX_ASSIGNMENTS[i]
            val centerZ = (BAR_BOTTOM_Z + BAR_TOP_Z) / 2f

            Fixture3D(
                fixture = Fixture(
                    fixtureId = "vbar-phys-${i + 1}",
                    name = "V-Bar ${i + 1}",
                    channelStart = channelStart,
                    channelCount = CHANNELS_PER_BAR,
                    universeId = universe,
                    profileId = "pixel-bar-24-rgb",
                ),
                position = Vec3(x = x, y = y, z = centerZ),
                groupId = if (i < 4) "v-left" else "v-right",
            )
        }
    }

    /**
     * Per-pixel 3D positions for all bars.
     * Returns map of fixtureId to list of 24 Vec3 (bottom to top).
     */
    fun pixelPositions(): Map<String, List<Vec3>> {
        return (0 until BAR_COUNT).associate { i ->
            val (x, y) = BAR_POSITIONS[i]
            val id = "vbar-phys-${i + 1}"
            val positions = (0 until PIXELS_PER_BAR).map { px ->
                val t = px.toFloat() / (PIXELS_PER_BAR - 1).toFloat()
                Vec3(x = x, y = y, z = BAR_BOTTOM_Z + t * (BAR_TOP_Z - BAR_BOTTOM_Z))
            }
            id to positions
        }
    }
}

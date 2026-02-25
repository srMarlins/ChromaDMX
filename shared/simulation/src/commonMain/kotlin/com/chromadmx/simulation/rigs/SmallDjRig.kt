package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Small DJ rig: 8 RGB PAR fixtures evenly spaced along a single truss.
 *
 * Layout:
 * - Single truss at height z=2.5m (using z as up-axis)
 * - 8 PARs spread across 7m width (centered at x=0)
 * - Each PAR is 3 channels (R, G, B)
 * - All on universe 0, channels 0-23
 *
 * Coordinate system:
 * - x = left/right (audience perspective)
 * - y = depth (upstage/downstage)
 * - z = height
 */
object SmallDjRig {

    /** Total fixture count. */
    const val FIXTURE_COUNT = 8

    /** Channels per fixture (RGB). */
    const val CHANNELS_PER_FIXTURE = 3

    /** Total DMX channels used. */
    const val TOTAL_CHANNELS = FIXTURE_COUNT * CHANNELS_PER_FIXTURE

    /** Truss height in meters. */
    const val TRUSS_HEIGHT = 2.5f

    /** Total width of the rig in meters. */
    const val RIG_WIDTH = 7.0f

    /** Depth position of the truss (upstage offset from center). */
    const val TRUSS_DEPTH = 1.0f

    /**
     * Generate the fixture list for a small DJ rig.
     */
    fun createFixtures(): List<Fixture3D> {
        val fixtures = mutableListOf<Fixture3D>()
        val spacing = RIG_WIDTH / (FIXTURE_COUNT - 1)
        val startX = -RIG_WIDTH / 2f

        for (i in 0 until FIXTURE_COUNT) {
            val x = startX + i * spacing
            val channelStart = i * CHANNELS_PER_FIXTURE

            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "dj-par-$i",
                        name = "DJ PAR ${i + 1}",
                        channelStart = channelStart,
                        channelCount = CHANNELS_PER_FIXTURE,
                        universeId = 0
                    ),
                    position = Vec3(x = x, y = TRUSS_DEPTH, z = TRUSS_HEIGHT),
                    groupId = "dj-truss"
                )
            )
        }

        return fixtures
    }
}

package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Truss rig: 30 pixel bars (8 pixels each, RGB) on two trusses.
 *
 * Layout:
 * - Front truss at height z=3.0m, depth y=0.5m: 15 pixel bars
 * - Rear truss at height z=4.0m, depth y=3.0m: 15 pixel bars
 * - Each pixel bar has 8 pixels, each pixel is 3 channels (RGB) = 24 channels per bar
 * - Total: 30 bars x 24 channels = 720 channels across 2 universes
 *
 * Universe allocation:
 * - Universe 0: Front truss bars 0-14 (15 x 24 = 360 channels) +
 *               Rear truss bars 0-5 (6 x 24 = 144 channels) = 504 channels
 * - Universe 1: Rear truss bars 6-14 (9 x 24 = 216 channels)
 *
 * Coordinate system:
 * - x = left/right (audience perspective)
 * - y = depth (upstage/downstage)
 * - z = height
 */
object TrussRig {

    /** Pixel bars per truss. */
    const val BARS_PER_TRUSS = 15

    /** Total fixture count. */
    const val FIXTURE_COUNT = BARS_PER_TRUSS * 2

    /** Pixels per bar. */
    const val PIXELS_PER_BAR = 8

    /** Channels per pixel (RGB). */
    const val CHANNELS_PER_PIXEL = 3

    /** Channels per bar. */
    const val CHANNELS_PER_BAR = PIXELS_PER_BAR * CHANNELS_PER_PIXEL

    /** Total DMX channels. */
    const val TOTAL_CHANNELS = FIXTURE_COUNT * CHANNELS_PER_BAR

    /** Front truss height. */
    const val FRONT_TRUSS_HEIGHT = 3.0f

    /** Rear truss height. */
    const val REAR_TRUSS_HEIGHT = 4.0f

    /** Front truss depth. */
    const val FRONT_TRUSS_DEPTH = 0.5f

    /** Rear truss depth. */
    const val REAR_TRUSS_DEPTH = 3.0f

    /** Total width of each truss. */
    const val TRUSS_WIDTH = 10.0f

    /**
     * Generate the fixture list for a truss rig.
     * Each fixture represents one pixel bar (not individual pixels).
     */
    fun createFixtures(): List<Fixture3D> {
        val fixtures = mutableListOf<Fixture3D>()
        val spacing = TRUSS_WIDTH / (BARS_PER_TRUSS - 1)
        val startX = -TRUSS_WIDTH / 2f
        var currentChannel = 0
        var currentUniverse = 0

        // Front truss
        for (i in 0 until BARS_PER_TRUSS) {
            val x = startX + i * spacing

            // Check if we need to move to next universe
            if (currentChannel + CHANNELS_PER_BAR > 512) {
                currentUniverse++
                currentChannel = 0
            }

            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "truss-front-bar-$i",
                        name = "Front Bar ${i + 1}",
                        channelStart = currentChannel,
                        channelCount = CHANNELS_PER_BAR,
                        universeId = currentUniverse,
                        profileId = "pixel-bar-8"
                    ),
                    position = Vec3(x = x, y = FRONT_TRUSS_DEPTH, z = FRONT_TRUSS_HEIGHT),
                    groupId = "front-truss"
                )
            )
            currentChannel += CHANNELS_PER_BAR
        }

        // Rear truss
        for (i in 0 until BARS_PER_TRUSS) {
            val x = startX + i * spacing

            // Check if we need to move to next universe
            if (currentChannel + CHANNELS_PER_BAR > 512) {
                currentUniverse++
                currentChannel = 0
            }

            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "truss-rear-bar-$i",
                        name = "Rear Bar ${i + 1}",
                        channelStart = currentChannel,
                        channelCount = CHANNELS_PER_BAR,
                        universeId = currentUniverse,
                        profileId = "pixel-bar-8"
                    ),
                    position = Vec3(x = x, y = REAR_TRUSS_DEPTH, z = REAR_TRUSS_HEIGHT),
                    groupId = "rear-truss"
                )
            )
            currentChannel += CHANNELS_PER_BAR
        }

        return fixtures
    }
}

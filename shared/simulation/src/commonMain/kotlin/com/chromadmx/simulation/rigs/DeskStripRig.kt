package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Desk strip rig: 3 WLED pixel strips (60 pixels each) for a streamer desk.
 *
 * Layout:
 * - "back-strip-lower": 60 pixels across the back of the desk at z=0.7m
 * - "back-strip-upper": 60 pixels across the back of the desk at z=1.2m
 * - "desk-under": 60 pixels under the desk front edge at z=0.3m
 * - Each pixel is 3 channels (RGB) = 180 channels per strip
 * - Total: 3 strips (180 pixels), 540 DMX channels, 2 universes
 * - Universe rollover at 512 channels
 *
 * Coordinate system:
 * - x = left/right (audience perspective)
 * - y = depth (toward/away from user)
 * - z = height
 */
object DeskStripRig {

    /** Number of strips. */
    const val STRIP_COUNT = 3

    /** Pixels per strip. */
    const val PIXELS_PER_STRIP = 60

    /** Total pixel count. */
    const val TOTAL_PIXELS = STRIP_COUNT * PIXELS_PER_STRIP

    /** Channels per pixel (RGB). */
    const val CHANNELS_PER_PIXEL = 3

    /** Channels per strip. */
    const val CHANNELS_PER_STRIP = PIXELS_PER_STRIP * CHANNELS_PER_PIXEL

    /** Total DMX channels. */
    const val TOTAL_CHANNELS = TOTAL_PIXELS * CHANNELS_PER_PIXEL

    /** Desk width in meters. */
    const val DESK_WIDTH = 1.5f

    private data class StripConfig(
        val id: String,
        val name: String,
        val groupId: String,
        val y: Float,
        val z: Float
    )

    private val strips = listOf(
        StripConfig("desk-back-lower", "Back Strip Lower", "back-strip-lower", y = 0.8f, z = 0.7f),
        StripConfig("desk-back-upper", "Back Strip Upper", "back-strip-upper", y = 0.8f, z = 1.2f),
        StripConfig("desk-under", "Desk Under Strip", "desk-under", y = 0.3f, z = 0.3f)
    )

    /**
     * Generate the fixture list for a desk strip rig.
     * Each fixture represents one pixel strip.
     */
    fun createFixtures(): List<Fixture3D> {
        val fixtures = mutableListOf<Fixture3D>()
        var currentChannel = 0
        var currentUniverse = 0

        for ((index, strip) in strips.withIndex()) {
            // Check if we need to move to next universe
            if (currentChannel + CHANNELS_PER_STRIP > 512) {
                currentUniverse++
                currentChannel = 0
            }

            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "${strip.id}-$index",
                        name = strip.name,
                        channelStart = currentChannel,
                        channelCount = CHANNELS_PER_STRIP,
                        universeId = currentUniverse,
                        profileId = "wled-pixel"
                    ),
                    position = Vec3(x = 0.0f, y = strip.y, z = strip.z),
                    groupId = strip.groupId
                )
            )
            currentChannel += CHANNELS_PER_STRIP
        }

        return fixtures
    }
}

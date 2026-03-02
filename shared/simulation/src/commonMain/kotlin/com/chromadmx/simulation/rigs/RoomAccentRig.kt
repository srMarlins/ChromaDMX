package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Room accent rig: 4 WLED pixel strips (75 pixels each) on ceiling/floor edges.
 *
 * Layout:
 * - "ceiling-back": 75 pixels along the ceiling back wall at z=2.4m, y=4.0m
 * - "ceiling-right": 75 pixels along the ceiling right side at z=2.4m, x=3.0m
 * - "floor-left": 75 pixels along the floor left side at z=0.05m, x=-3.0m
 * - "floor-front": 75 pixels along the floor front edge at z=0.05m, y=0.0m
 * - Each pixel is 3 channels (RGB) = 225 channels per strip
 * - Total: 4 strips (300 pixels), 900 DMX channels, 2 universes
 * - Universe rollover at 512 channels
 *
 * Coordinate system:
 * - x = left/right (audience perspective)
 * - y = depth (front/back of room)
 * - z = height
 */
object RoomAccentRig {

    /** Number of strips. */
    const val STRIP_COUNT = 4

    /** Pixels per strip. */
    const val PIXELS_PER_STRIP = 75

    /** Total pixel count. */
    const val TOTAL_PIXELS = STRIP_COUNT * PIXELS_PER_STRIP

    /** Channels per pixel (RGB). */
    const val CHANNELS_PER_PIXEL = 3

    /** Channels per strip. */
    const val CHANNELS_PER_STRIP = PIXELS_PER_STRIP * CHANNELS_PER_PIXEL

    /** Total DMX channels. */
    const val TOTAL_CHANNELS = TOTAL_PIXELS * CHANNELS_PER_PIXEL

    /** Room width in meters. */
    const val ROOM_WIDTH = 6.0f

    /** Room depth in meters. */
    const val ROOM_DEPTH = 4.0f

    /** Ceiling height in meters. */
    const val CEILING_HEIGHT = 2.4f

    /** Floor height in meters. */
    const val FLOOR_HEIGHT = 0.05f

    private data class StripConfig(
        val id: String,
        val name: String,
        val groupId: String,
        val position: Vec3
    )

    private val strips = listOf(
        StripConfig("room-ceiling-back", "Ceiling Back", "ceiling-back",
            Vec3(x = 0.0f, y = ROOM_DEPTH, z = CEILING_HEIGHT)),
        StripConfig("room-ceiling-right", "Ceiling Right", "ceiling-right",
            Vec3(x = ROOM_WIDTH / 2f, y = ROOM_DEPTH / 2f, z = CEILING_HEIGHT)),
        StripConfig("room-floor-left", "Floor Left", "floor-left",
            Vec3(x = -ROOM_WIDTH / 2f, y = ROOM_DEPTH / 2f, z = FLOOR_HEIGHT)),
        StripConfig("room-floor-front", "Floor Front", "floor-front",
            Vec3(x = 0.0f, y = 0.0f, z = FLOOR_HEIGHT))
    )

    /**
     * Generate the fixture list for a room accent rig.
     * Each fixture represents one pixel strip.
     * Uses universe rollover at 512 channels.
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
                    position = strip.position,
                    groupId = strip.groupId
                )
            )
            currentChannel += CHANNELS_PER_STRIP
        }

        return fixtures
    }
}

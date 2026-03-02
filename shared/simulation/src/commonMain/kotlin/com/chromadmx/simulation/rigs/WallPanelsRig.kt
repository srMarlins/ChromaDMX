package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Wall panels rig: 9 hexagonal RGB panels in a honeycomb pattern (Nanoleaf-style).
 *
 * Layout:
 * - 9 panels arranged in a 3-row honeycomb on a wall at z=1.5m (center)
 * - Each panel is 1 RGB fixture (3 channels)
 * - Total: 9 panels, 27 DMX channels, 1 universe
 * - All on universe 0
 *
 * Honeycomb pattern (rows from top):
 * - Row 0 (top):    3 panels
 * - Row 1 (middle): 3 panels (offset right by half spacing)
 * - Row 2 (bottom): 3 panels
 *
 * Hex spacing: 0.25m horizontal, 0.22m vertical.
 *
 * Coordinate system:
 * - x = left/right (audience perspective)
 * - y = depth (distance from wall, constant for all panels)
 * - z = height
 */
object WallPanelsRig {

    /** Total panel count. */
    const val PANEL_COUNT = 9

    /** Channels per panel (RGB). */
    const val CHANNELS_PER_PANEL = 3

    /** Total DMX channels. */
    const val TOTAL_CHANNELS = PANEL_COUNT * CHANNELS_PER_PANEL

    /** Panels per row. */
    const val PANELS_PER_ROW = 3

    /** Number of rows. */
    const val ROW_COUNT = 3

    /** Horizontal spacing between panel centers in meters. */
    const val HEX_SPACING_H = 0.25f

    /** Vertical spacing between row centers in meters. */
    const val HEX_SPACING_V = 0.22f

    /** Wall depth position in meters. */
    const val WALL_DEPTH = 3.0f

    /** Center height of the honeycomb in meters. */
    const val CENTER_HEIGHT = 1.5f

    /**
     * Generate the fixture list for a wall panels rig.
     * Each fixture represents one hexagonal RGB panel.
     */
    fun createFixtures(): List<Fixture3D> {
        val fixtures = mutableListOf<Fixture3D>()
        var currentChannel = 0
        var panelIndex = 0

        // Center the honeycomb horizontally
        val totalWidth = (PANELS_PER_ROW - 1) * HEX_SPACING_H
        val startX = -totalWidth / 2f

        // Center the honeycomb vertically around CENTER_HEIGHT
        val totalHeight = (ROW_COUNT - 1) * HEX_SPACING_V
        val startZ = CENTER_HEIGHT + totalHeight / 2f

        for (row in 0 until ROW_COUNT) {
            // Odd rows are offset right by half the horizontal spacing
            val xOffset = if (row % 2 == 1) HEX_SPACING_H / 2f else 0.0f
            val z = startZ - row * HEX_SPACING_V

            for (col in 0 until PANELS_PER_ROW) {
                val x = startX + col * HEX_SPACING_H + xOffset

                fixtures.add(
                    Fixture3D(
                        fixture = Fixture(
                            fixtureId = "wall-panel-$panelIndex",
                            name = "Panel ${panelIndex + 1}",
                            channelStart = currentChannel,
                            channelCount = CHANNELS_PER_PANEL,
                            universeId = 0,
                            profileId = "generic-rgb-par"
                        ),
                        position = Vec3(x = x, y = WALL_DEPTH, z = z),
                        groupId = "wall-panels"
                    )
                )
                currentChannel += CHANNELS_PER_PANEL
                panelIndex++
            }
        }

        return fixtures
    }
}

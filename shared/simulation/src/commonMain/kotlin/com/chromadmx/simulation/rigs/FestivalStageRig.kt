package com.chromadmx.simulation.rigs

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Festival stage rig: 108 fixtures across multiple heights and types.
 *
 * Layout:
 * - Ground level (z=0.3m): 16 uplighting PARs along the stage front
 * - Low truss (z=3.0m): 24 pixel bars (8 pixels each) across stage width
 * - Mid truss (z=5.0m): 24 pixel bars + 8 moving heads
 * - High truss (z=7.0m): 20 PARs for wash + 8 strobes
 * - Side towers (z=4.0m): 4 PARs per side = 8 PARs total
 *
 * Fixture breakdown:
 * - 16 ground PARs (3ch each = 48 channels)
 * - 48 pixel bars (24ch each = 1152 channels)
 * - 8 moving heads (16ch each = 128 channels)
 * - 20 wash PARs (3ch each = 60 channels)
 * - 8 strobes (2ch each = 16 channels)
 * - 8 side PARs (3ch each = 24 channels)
 * Total: 108 fixtures, 1428 channels, ~3 universes
 *
 * Coordinate system:
 * - x = left/right (audience perspective), stage is ~20m wide
 * - y = depth (upstage/downstage), stage is ~12m deep
 * - z = height
 */
object FestivalStageRig {

    /** Stage width in meters. */
    const val STAGE_WIDTH = 20.0f

    /** Stage depth in meters. */
    const val STAGE_DEPTH = 12.0f

    /**
     * Generate the complete festival stage fixture list.
     */
    fun createFixtures(): List<Fixture3D> {
        val fixtures = mutableListOf<Fixture3D>()
        var currentChannel = 0
        var currentUniverse = 0

        // Helper to advance channel, wrapping to next universe at 512
        fun advanceChannel(count: Int): Pair<Int, Int> {
            if (currentChannel + count > 512) {
                currentUniverse++
                currentChannel = 0
            }
            val start = currentChannel
            val uni = currentUniverse
            currentChannel += count
            return Pair(uni, start)
        }

        // ---- Ground PARs (z=0.3m) ----
        val groundParCount = 16
        val groundSpacing = STAGE_WIDTH / (groundParCount - 1)
        val groundStartX = -STAGE_WIDTH / 2f
        for (i in 0 until groundParCount) {
            val (uni, ch) = advanceChannel(3)
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "ground-par-$i",
                        name = "Ground PAR ${i + 1}",
                        channelStart = ch,
                        channelCount = 3,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = groundStartX + i * groundSpacing,
                        y = 0.2f,
                        z = 0.3f
                    ),
                    groupId = "ground-pars"
                )
            )
        }

        // ---- Low truss pixel bars (z=3.0m) ----
        val lowBarCount = 24
        val lowSpacing = STAGE_WIDTH / (lowBarCount - 1)
        val lowStartX = -STAGE_WIDTH / 2f
        for (i in 0 until lowBarCount) {
            val (uni, ch) = advanceChannel(24) // 8 pixels x 3ch
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "low-bar-$i",
                        name = "Low Bar ${i + 1}",
                        channelStart = ch,
                        channelCount = 24,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = lowStartX + i * lowSpacing,
                        y = 2.0f,
                        z = 3.0f
                    ),
                    groupId = "low-truss"
                )
            )
        }

        // ---- Mid truss pixel bars (z=5.0m) ----
        val midBarCount = 24
        val midSpacing = STAGE_WIDTH / (midBarCount - 1)
        val midStartX = -STAGE_WIDTH / 2f
        for (i in 0 until midBarCount) {
            val (uni, ch) = advanceChannel(24)
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "mid-bar-$i",
                        name = "Mid Bar ${i + 1}",
                        channelStart = ch,
                        channelCount = 24,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = midStartX + i * midSpacing,
                        y = 5.0f,
                        z = 5.0f
                    ),
                    groupId = "mid-truss"
                )
            )
        }

        // ---- Mid truss moving heads (z=5.0m) ----
        val movingHeadCount = 8
        val mhSpacing = STAGE_WIDTH / (movingHeadCount - 1)
        val mhStartX = -STAGE_WIDTH / 2f
        for (i in 0 until movingHeadCount) {
            val (uni, ch) = advanceChannel(16) // pan, tilt, speed, dimmer, R,G,B,W, strobe, + reserved
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "moving-head-$i",
                        name = "Moving Head ${i + 1}",
                        channelStart = ch,
                        channelCount = 16,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = mhStartX + i * mhSpacing,
                        y = 5.5f,
                        z = 5.0f
                    ),
                    groupId = "mid-truss-movers"
                )
            )
        }

        // ---- High truss wash PARs (z=7.0m) ----
        val highParCount = 20
        val highSpacing = STAGE_WIDTH / (highParCount - 1)
        val highStartX = -STAGE_WIDTH / 2f
        for (i in 0 until highParCount) {
            val (uni, ch) = advanceChannel(3)
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "high-par-$i",
                        name = "High PAR ${i + 1}",
                        channelStart = ch,
                        channelCount = 3,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = highStartX + i * highSpacing,
                        y = 4.0f,
                        z = 7.0f
                    ),
                    groupId = "high-truss"
                )
            )
        }

        // ---- High truss strobes (z=7.0m) ----
        val strobeCount = 8
        val strobeSpacing = STAGE_WIDTH / (strobeCount - 1)
        val strobeStartX = -STAGE_WIDTH / 2f
        for (i in 0 until strobeCount) {
            val (uni, ch) = advanceChannel(2) // intensity + speed
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "strobe-$i",
                        name = "Strobe ${i + 1}",
                        channelStart = ch,
                        channelCount = 2,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = strobeStartX + i * strobeSpacing,
                        y = 3.5f,
                        z = 7.0f
                    ),
                    groupId = "high-truss-strobes"
                )
            )
        }

        // ---- Side tower PARs (z=4.0m) ----
        // Left side: 4 PARs stacked vertically at x = -STAGE_WIDTH/2 - 1
        for (i in 0 until 4) {
            val (uni, ch) = advanceChannel(3)
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "side-left-par-$i",
                        name = "Side Left PAR ${i + 1}",
                        channelStart = ch,
                        channelCount = 3,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = -STAGE_WIDTH / 2f - 1.0f,
                        y = 1.0f,
                        z = 2.0f + i * 1.0f
                    ),
                    groupId = "side-tower-left"
                )
            )
        }

        // Right side: 4 PARs stacked vertically at x = STAGE_WIDTH/2 + 1
        for (i in 0 until 4) {
            val (uni, ch) = advanceChannel(3)
            fixtures.add(
                Fixture3D(
                    fixture = Fixture(
                        fixtureId = "side-right-par-$i",
                        name = "Side Right PAR ${i + 1}",
                        channelStart = ch,
                        channelCount = 3,
                        universeId = uni
                    ),
                    position = Vec3(
                        x = STAGE_WIDTH / 2f + 1.0f,
                        y = 1.0f,
                        z = 2.0f + i * 1.0f
                    ),
                    groupId = "side-tower-right"
                )
            )
        }

        return fixtures
    }
}

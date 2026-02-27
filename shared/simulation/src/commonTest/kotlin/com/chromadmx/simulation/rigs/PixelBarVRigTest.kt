package com.chromadmx.simulation.rigs

import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelBarVRigTest {

    @Test
    fun rigHasEightFixtures() {
        val rig = SimulatedFixtureRig(RigPreset.PIXEL_BAR_V)
        assertEquals(8, rig.fixtureCount)
    }

    @Test
    fun eachBarHas72Channels() {
        val fixtures = PixelBarVRig.createFixtures()
        fixtures.forEach { f3d ->
            assertEquals(72, f3d.fixture.channelCount, "Bar ${f3d.fixture.fixtureId} should have 72 channels")
        }
    }

    @Test
    fun totalChannelsIs576() {
        val rig = SimulatedFixtureRig(RigPreset.PIXEL_BAR_V)
        assertEquals(576, rig.totalChannels)
    }

    @Test
    fun usesThreeUniverses() {
        val rig = SimulatedFixtureRig(RigPreset.PIXEL_BAR_V)
        assertEquals(3, rig.universeCount)
        assertTrue(rig.universeIds.containsAll(setOf(0, 1, 2)))
    }

    @Test
    fun dmxAddressesAreScrambled() {
        val fixtures = PixelBarVRig.createFixtures()
        val channelStarts = fixtures.map { it.fixture.channelStart }
        assertTrue(channelStarts != channelStarts.sorted(), "DMX addresses should be scrambled")
    }

    @Test
    fun physicalPositionsFormVShape() {
        val fixtures = PixelBarVRig.createFixtures()
        val leftArm = fixtures.filter { it.groupId == "v-left" }
        assertEquals(4, leftArm.size, "Should be 4 fixtures in the left arm")
        leftArm.forEach { assertTrue(it.position.x < 0f, "${it.fixture.fixtureId} should be on left (negative x)") }

        val rightArm = fixtures.filter { it.groupId == "v-right" }
        assertEquals(4, rightArm.size, "Should be 4 fixtures in the right arm")
        rightArm.forEach { assertTrue(it.position.x > 0f, "${it.fixture.fixtureId} should be on right (positive x)") }
    }

    @Test
    fun pixelPositionsAreVertical() {
        val pixelPositions = PixelBarVRig.pixelPositions()
        pixelPositions.forEach { (barId, positions) ->
            assertEquals(24, positions.size, "$barId should have 24 pixel positions")
            val firstX = positions.first().x
            val firstY = positions.first().y
            positions.forEach { pos ->
                assertEquals(firstX, pos.x, 0.001f, "$barId pixels should share x")
                assertEquals(firstY, pos.y, 0.001f, "$barId pixels should share y")
            }
            for (i in 1 until positions.size) {
                assertTrue(positions[i].z > positions[i - 1].z, "$barId pixel $i should be above pixel ${i - 1}")
            }
        }
    }
}

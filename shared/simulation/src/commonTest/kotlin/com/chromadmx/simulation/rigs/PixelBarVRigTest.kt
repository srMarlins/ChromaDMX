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
    fun usesTwoUniverses() {
        val rig = SimulatedFixtureRig(RigPreset.PIXEL_BAR_V)
        assertEquals(2, rig.universeCount)
        assertTrue(rig.universeIds.containsAll(setOf(0, 1)))
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
        val leftArm = fixtures.filter {
            it.fixture.fixtureId.contains("phys-1") ||
                it.fixture.fixtureId.contains("phys-2") ||
                it.fixture.fixtureId.contains("phys-3") ||
                it.fixture.fixtureId.contains("phys-4")
        }
        leftArm.forEach { assertTrue(it.position.x < 0f, "${it.fixture.fixtureId} should be on left (negative x)") }

        val rightArm = fixtures.filter {
            it.fixture.fixtureId.contains("phys-5") ||
                it.fixture.fixtureId.contains("phys-6") ||
                it.fixture.fixtureId.contains("phys-7") ||
                it.fixture.fixtureId.contains("phys-8")
        }
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

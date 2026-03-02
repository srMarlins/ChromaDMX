package com.chromadmx.simulation.rigs

import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeskStripRigTest {

    @Test
    fun creates180Pixels() {
        val rig = SimulatedFixtureRig(RigPreset.DESK_STRIP)
        assertEquals(3, rig.fixtureCount)
        // 3 strips x 60 pixels x 3 channels = 540 total channels
        assertEquals(DeskStripRig.TOTAL_CHANNELS, rig.totalChannels)
        // Each strip has 60 pixels
        val fixtures = DeskStripRig.createFixtures()
        fixtures.forEach { f3d ->
            assertEquals(
                DeskStripRig.CHANNELS_PER_STRIP,
                f3d.fixture.channelCount,
                "${f3d.fixture.fixtureId} should have ${DeskStripRig.CHANNELS_PER_STRIP} channels (60 pixels x 3)"
            )
        }
    }

    @Test
    fun uses2Universes() {
        val rig = SimulatedFixtureRig(RigPreset.DESK_STRIP)
        // 2 strips fit in universe 0 (360 channels), 3rd rolls to universe 1
        assertEquals(2, rig.universeCount)
        assertTrue(rig.universeIds.containsAll(setOf(0, 1)))
    }

    @Test
    fun has3Groups() {
        val rig = SimulatedFixtureRig(RigPreset.DESK_STRIP)
        assertEquals(3, rig.groupIds.size)
        assertTrue(rig.groupIds.containsAll(setOf("back-strip-lower", "back-strip-upper", "desk-under")))
    }

    @Test
    fun noChannelOverlap() {
        val fixtures = DeskStripRig.createFixtures()
        for (i in fixtures.indices) {
            for (j in i + 1 until fixtures.size) {
                val a = fixtures[i].fixture
                val b = fixtures[j].fixture
                if (a.universeId == b.universeId) {
                    val aEnd = a.channelStart + a.channelCount
                    val bEnd = b.channelStart + b.channelCount
                    assertTrue(
                        aEnd <= b.channelStart || bEnd <= a.channelStart,
                        "Channels overlap between ${a.fixtureId} (${ a.channelStart}..<$aEnd) " +
                                "and ${b.fixtureId} (${b.channelStart}..<$bEnd)"
                    )
                }
            }
        }
    }
}

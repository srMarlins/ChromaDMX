package com.chromadmx.simulation.rigs

import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomAccentRigTest {

    @Test
    fun creates300Pixels() {
        val rig = SimulatedFixtureRig(RigPreset.ROOM_ACCENT)
        assertEquals(4, rig.fixtureCount)
        // 4 strips x 75 pixels x 3 channels = 900 total channels
        assertEquals(RoomAccentRig.TOTAL_CHANNELS, rig.totalChannels)
        // Each strip has 75 pixels
        val fixtures = RoomAccentRig.createFixtures()
        fixtures.forEach { f3d ->
            assertEquals(
                RoomAccentRig.CHANNELS_PER_STRIP,
                f3d.fixture.channelCount,
                "${f3d.fixture.fixtureId} should have ${RoomAccentRig.CHANNELS_PER_STRIP} channels (75 pixels x 3)"
            )
        }
    }

    @Test
    fun uses2Universes() {
        val rig = SimulatedFixtureRig(RigPreset.ROOM_ACCENT)
        assertEquals(2, rig.universeCount)
        assertTrue(rig.universeIds.containsAll(setOf(0, 1)))
    }

    @Test
    fun has4Groups() {
        val rig = SimulatedFixtureRig(RigPreset.ROOM_ACCENT)
        assertEquals(4, rig.groupIds.size)
        assertTrue(
            rig.groupIds.containsAll(
                setOf("ceiling-back", "ceiling-right", "floor-left", "floor-front")
            )
        )
    }
}

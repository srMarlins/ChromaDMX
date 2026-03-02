package com.chromadmx.simulation.rigs

import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WallPanelsRigTest {

    @Test
    fun creates9Panels() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(9, rig.fixtureCount)
    }

    @Test
    fun allOnUniverse0() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(1, rig.universeCount)
        assertTrue(rig.universeIds.contains(0))
    }

    @Test
    fun uses27Channels() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(27, rig.totalChannels)
    }

    @Test
    fun singleGroup() {
        val rig = SimulatedFixtureRig(RigPreset.WALL_PANELS)
        assertEquals(1, rig.groupIds.size)
        assertTrue(rig.groupIds.contains("wall-panels"))
    }
}

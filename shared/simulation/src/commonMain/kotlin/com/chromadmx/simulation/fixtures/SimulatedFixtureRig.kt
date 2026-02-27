package com.chromadmx.simulation.fixtures

import com.chromadmx.core.model.Fixture3D
import com.chromadmx.simulation.rigs.FestivalStageRig
import com.chromadmx.simulation.rigs.PixelBarVRig
import com.chromadmx.simulation.rigs.SmallDjRig
import com.chromadmx.simulation.rigs.TrussRig

/**
 * Simulated fixture rig providing preset layouts for testing.
 *
 * Returns [Fixture3D] lists with realistic 3D positions (in meters)
 * for use in testing spatial effects, DMX output, and vision modules
 * without physical hardware.
 *
 * Usage:
 * ```
 * val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
 * val fixtures = rig.fixtures
 * // Use fixtures for testing...
 * ```
 *
 * @param preset The rig preset to load
 */
class SimulatedFixtureRig(val preset: RigPreset) {

    /** The fixture list for this rig preset. */
    val fixtures: List<Fixture3D> = when (preset) {
        RigPreset.SMALL_DJ -> SmallDjRig.createFixtures()
        RigPreset.TRUSS_RIG -> TrussRig.createFixtures()
        RigPreset.FESTIVAL_STAGE -> FestivalStageRig.createFixtures()
        RigPreset.PIXEL_BAR_V -> PixelBarVRig.createFixtures()
    }

    /** Total number of fixtures in the rig. */
    val fixtureCount: Int get() = fixtures.size

    /** Set of universe IDs used by this rig. */
    val universeIds: Set<Int> get() = fixtures.map { it.fixture.universeId }.toSet()

    /** Number of universes required. */
    val universeCount: Int get() = universeIds.size

    /** Total DMX channels used across all universes. */
    val totalChannels: Int get() = fixtures.sumOf { it.fixture.channelCount }

    /** Group IDs present in this rig. */
    val groupIds: Set<String> get() = fixtures.mapNotNull { it.groupId }.toSet()

    /**
     * Get all fixtures in a specific group.
     */
    fun fixturesInGroup(groupId: String): List<Fixture3D> =
        fixtures.filter { it.groupId == groupId }

    /**
     * Get all fixtures on a specific universe.
     */
    fun fixturesOnUniverse(universeId: Int): List<Fixture3D> =
        fixtures.filter { it.fixture.universeId == universeId }

    /**
     * Find a fixture by its ID.
     */
    fun findFixture(fixtureId: String): Fixture3D? =
        fixtures.find { it.fixture.fixtureId == fixtureId }

    companion object {
        /**
         * Create a rig from a preset name string (case-insensitive).
         * Returns null if the name is not recognized.
         */
        fun fromName(name: String): SimulatedFixtureRig? {
            val preset = RigPreset.entries.find {
                it.name.equals(name, ignoreCase = true)
            } ?: return null
            return SimulatedFixtureRig(preset)
        }
    }
}

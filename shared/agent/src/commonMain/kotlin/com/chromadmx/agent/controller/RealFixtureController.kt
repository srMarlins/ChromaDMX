package com.chromadmx.agent.controller

import com.chromadmx.core.model.Fixture3D

/**
 * Real [FixtureController] bridging to the fixture management layer.
 *
 * Wraps a mutable list of [Fixture3D] instances, providing operations
 * for listing, firing (identification flash), and grouping fixtures.
 *
 * @param fixturesProvider Provider for the current fixture list.
 */
class RealFixtureController(
    private val fixturesProvider: () -> List<Fixture3D>,
) : FixtureController {

    /** Group assignments: groupName -> set of fixtureIds. */
    private val groups = mutableMapOf<String, MutableSet<String>>()

    override fun listFixtures(): List<Fixture3D> = fixturesProvider()

    override fun fireFixture(fixtureId: String, colorHex: String): Boolean {
        val fixtures = fixturesProvider()
        val fixture = fixtures.find { it.fixture.fixtureId == fixtureId }
        return fixture != null
        // In a real implementation, this would:
        // 1. Parse colorHex to RGB
        // 2. Write the color directly to the fixture's DMX channels
        // 3. Set a timer to restore the previous state
    }

    override fun setFixtureGroup(groupName: String, fixtureIds: List<String>): Boolean {
        val fixtures = fixturesProvider()
        val validIds = fixtureIds.filter { id ->
            fixtures.any { it.fixture.fixtureId == id }
        }
        if (validIds.isEmpty() && fixtureIds.isNotEmpty()) return false

        groups[groupName] = validIds.toMutableSet()
        return true
    }
}

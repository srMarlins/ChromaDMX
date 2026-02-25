package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.core.model.Fixture3D

/**
 * Fake [FixtureController] for testing fixture tools.
 */
class FakeFixtureController : FixtureController {
    var fixtures: List<Fixture3D> = emptyList()
    var fireResult: Boolean = true
    var groupResult: Boolean = true

    var lastFiredFixtureId: String = ""
    var lastFiredColor: String = ""
    var lastGroupName: String = ""
    var lastGroupFixtureIds: List<String> = emptyList()

    override fun listFixtures(): List<Fixture3D> = fixtures

    override fun fireFixture(fixtureId: String, colorHex: String): Boolean {
        lastFiredFixtureId = fixtureId
        lastFiredColor = colorHex
        return fireResult
    }

    override fun setFixtureGroup(groupName: String, fixtureIds: List<String>): Boolean {
        lastGroupName = groupName
        lastGroupFixtureIds = fixtureIds
        return groupResult
    }
}

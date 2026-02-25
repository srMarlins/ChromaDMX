package com.chromadmx.agent.controller

import com.chromadmx.core.model.Fixture3D

/**
 * Abstraction over fixture management for agent tool operations.
 */
interface FixtureController {
    /** List all known fixtures with their 3D positions. */
    fun listFixtures(): List<Fixture3D>

    /** Fire a single fixture with a specific color. Returns true on success. */
    fun fireFixture(fixtureId: String, colorHex: String): Boolean

    /** Assign fixtures to a named group. Returns true on success. */
    fun setFixtureGroup(groupName: String, fixtureIds: List<String>): Boolean
}

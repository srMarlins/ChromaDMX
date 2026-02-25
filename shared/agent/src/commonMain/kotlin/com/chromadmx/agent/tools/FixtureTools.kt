package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.FixtureController
import kotlinx.serialization.Serializable

/**
 * Tool: List all known fixtures with their positions and groups.
 */
class ListFixturesTool(private val controller: FixtureController) {

    fun execute(): String {
        val fixtures = controller.listFixtures()
        if (fixtures.isEmpty()) {
            return "0 fixtures configured. Use vision mapping or manual setup to add fixtures."
        }
        val listing = fixtures.joinToString("\n") { f ->
            val pos = f.position
            "  - ${f.fixture.name} (${f.fixture.fixtureId}): " +
                "pos=(${pos.x}, ${pos.y}, ${pos.z}), " +
                "ch=${f.fixture.channelStart}-${f.fixture.channelStart + f.fixture.channelCount - 1}, " +
                "universe=${f.fixture.universeId}" +
                (f.groupId?.let { ", group=$it" } ?: "")
        }
        return "${fixtures.size} fixtures:\n$listing"
    }
}

/**
 * Tool: Fire a single fixture with a specific color for identification.
 */
class FireFixtureTool(private val controller: FixtureController) {
    @Serializable
    data class Args(val fixtureId: String, val colorHex: String)

    fun execute(args: Args): String {
        val success = controller.fireFixture(args.fixtureId, args.colorHex)
        return if (success) {
            "Fired fixture '${args.fixtureId}' with color ${args.colorHex}"
        } else {
            "Failed to fire fixture '${args.fixtureId}'. Check fixture ID."
        }
    }
}

/**
 * Tool: Assign fixtures to a named group.
 */
class SetFixtureGroupTool(private val controller: FixtureController) {
    @Serializable
    data class Args(val groupName: String, val fixtureIds: List<String>)

    fun execute(args: Args): String {
        val success = controller.setFixtureGroup(args.groupName, args.fixtureIds)
        return if (success) {
            "Created group '${args.groupName}' with ${args.fixtureIds.size} fixtures"
        } else {
            "Failed to create group '${args.groupName}'. Check fixture IDs."
        }
    }
}

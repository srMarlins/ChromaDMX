package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.agent.controller.FixtureController
import kotlinx.serialization.Serializable

class ListFixturesTool(private val controller: FixtureController) : SimpleTool<ListFixturesTool.Args>(
    argsSerializer = Args.serializer(),
    name = "listFixtures",
    description = "List all known DMX fixtures with their 3D positions, channels, universes, and group assignments."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
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

class FireFixtureTool(private val controller: FixtureController) : SimpleTool<FireFixtureTool.Args>(
    argsSerializer = Args.serializer(),
    name = "fireFixture",
    description = "Flash a single fixture with a specific color for identification purposes."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The fixture ID to flash")
        val fixtureId: String,
        @property:LLMDescription("Hex color to flash (e.g. #FF0000 for red, #FFFFFF for white)")
        val colorHex: String
    )

    override suspend fun execute(args: Args): String {
        val success = controller.fireFixture(args.fixtureId, args.colorHex)
        return if (success) {
            "Identified fixture '${args.fixtureId}' with color ${args.colorHex}"
        } else {
            "Fixture '${args.fixtureId}' not found. Check fixture ID."
        }
    }
}

class SetFixtureGroupTool(private val controller: FixtureController) : SimpleTool<SetFixtureGroupTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setFixtureGroup",
    description = "Assign one or more fixtures to a named group for batch control (e.g. 'stage_left', 'wash_lights')."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Name for the fixture group")
        val groupName: String,
        @property:LLMDescription("List of fixture IDs to include in the group")
        val fixtureIds: List<String>
    )

    override suspend fun execute(args: Args): String {
        val success = controller.setFixtureGroup(args.groupName, args.fixtureIds)
        return if (success) {
            "Created group '${args.groupName}' with ${args.fixtureIds.size} fixtures"
        } else {
            "Failed to create group '${args.groupName}'. Check fixture IDs."
        }
    }
}

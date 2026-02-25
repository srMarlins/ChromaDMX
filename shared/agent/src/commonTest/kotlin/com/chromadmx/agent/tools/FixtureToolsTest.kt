package com.chromadmx.agent.tools

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixtureToolsTest {
    private val controller = FakeFixtureController()

    @Test
    fun listFixturesReturnsAll() = runTest {
        controller.fixtures = listOf(
            Fixture3D(
                fixture = Fixture("fix-1", "Par Left", 1, 6, 0),
                position = Vec3(0f, 2f, 0f),
                groupId = "front"
            ),
            Fixture3D(
                fixture = Fixture("fix-2", "Par Right", 7, 6, 0),
                position = Vec3(3f, 2f, 0f),
                groupId = "front"
            )
        )
        val tool = ListFixturesTool(controller)
        val result = tool.execute(ListFixturesTool.Args())
        assertContains(result, "2 fixtures")
        assertContains(result, "Par Left")
        assertContains(result, "Par Right")
    }

    @Test
    fun listFixturesEmptyReturnsMessage() = runTest {
        controller.fixtures = emptyList()
        val tool = ListFixturesTool(controller)
        val result = tool.execute(ListFixturesTool.Args())
        assertContains(result, "0 fixtures")
    }

    @Test
    fun fireFixtureSuccess() = runTest {
        controller.fireResult = true
        val tool = FireFixtureTool(controller)
        val result = tool.execute(FireFixtureTool.Args(fixtureId = "fix-1", colorHex = "#FF0000"))
        assertContains(result, "fix-1")
        assertContains(result, "#FF0000")
        assertEquals("fix-1", controller.lastFiredFixtureId)
        assertEquals("#FF0000", controller.lastFiredColor)
    }

    @Test
    fun fireFixtureFailure() = runTest {
        controller.fireResult = false
        val tool = FireFixtureTool(controller)
        val result = tool.execute(FireFixtureTool.Args(fixtureId = "unknown", colorHex = "#FF0000"))
        assertContains(result, "not found")
    }

    @Test
    fun setFixtureGroupSuccess() = runTest {
        controller.groupResult = true
        val tool = SetFixtureGroupTool(controller)
        val result = tool.execute(SetFixtureGroupTool.Args(
            groupName = "front-truss",
            fixtureIds = listOf("fix-1", "fix-2", "fix-3")
        ))
        assertContains(result, "front-truss")
        assertContains(result, "3")
        assertEquals("front-truss", controller.lastGroupName)
        assertEquals(3, controller.lastGroupFixtureIds.size)
    }

    @Test
    fun setFixtureGroupFailure() = runTest {
        controller.groupResult = false
        val tool = SetFixtureGroupTool(controller)
        val result = tool.execute(SetFixtureGroupTool.Args(
            groupName = "front-truss",
            fixtureIds = listOf("fix-1")
        ))
        assertContains(result, "Failed")
    }
}

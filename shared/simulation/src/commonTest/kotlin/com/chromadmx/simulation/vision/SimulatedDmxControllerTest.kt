package com.chromadmx.simulation.vision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SimulatedDmxControllerTest {

    @Test
    fun initialStateIsEmpty() {
        val controller = SimulatedDmxController()
        assertTrue(controller.activeFixtures.value.isEmpty())
        assertTrue(controller.activePixels.value.isEmpty())
    }

    @Test
    fun fireFixtureActivatesAllPixels() = runTest {
        val controller = SimulatedDmxController()
        controller.fireFixture("vbar-phys-1")

        assertTrue(controller.activeFixtures.value.contains("vbar-phys-1"))
        assertEquals(24, controller.activePixels.value["vbar-phys-1"]?.size)
    }

    @Test
    fun fireEndPixelsActivatesOnlyEndpoints() = runTest {
        val controller = SimulatedDmxController()
        controller.fireEndPixels("vbar-phys-1")

        assertTrue(controller.activeFixtures.value.contains("vbar-phys-1"))
        val pixels = controller.activePixels.value["vbar-phys-1"]!!
        assertEquals(2, pixels.size)
        assertTrue(pixels.contains(0))
        assertTrue(pixels.contains(23))
    }

    @Test
    fun turnOffFixtureRemovesIt() = runTest {
        val controller = SimulatedDmxController()
        controller.fireFixture("vbar-phys-1")
        controller.fireFixture("vbar-phys-2")
        controller.turnOffFixture("vbar-phys-1")

        assertTrue(!controller.activeFixtures.value.contains("vbar-phys-1"))
        assertTrue(controller.activeFixtures.value.contains("vbar-phys-2"))
    }

    @Test
    fun blackoutClearsEverything() = runTest {
        val controller = SimulatedDmxController()
        controller.fireFixture("vbar-phys-1")
        controller.fireFixture("vbar-phys-2")
        controller.blackout()

        assertTrue(controller.activeFixtures.value.isEmpty())
        assertTrue(controller.activePixels.value.isEmpty())
    }
}

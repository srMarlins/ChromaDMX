package com.chromadmx.simulation.vision

import com.chromadmx.vision.calibration.DmxController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Simulated DMX controller that tracks active fixtures and pixels
 * via observable state flows. Used by [SimulatedFrameCapture] to
 * know what to render and by the UI to animate fixture flashing.
 *
 * @param pixelsPerFixture number of addressable pixels per fixture (default 24)
 */
class SimulatedDmxController(
    private val pixelsPerFixture: Int = 24,
) : DmxController {

    private val _activeFixtures = MutableStateFlow<Set<String>>(emptySet())
    val activeFixtures: StateFlow<Set<String>> = _activeFixtures.asStateFlow()

    private val _activePixels = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val activePixels: StateFlow<Map<String, Set<Int>>> = _activePixels.asStateFlow()

    override suspend fun fireFixture(fixtureId: String) {
        val allPixels = (0 until pixelsPerFixture).toSet()
        _activeFixtures.update { it + fixtureId }
        _activePixels.update { it + (fixtureId to allPixels) }
    }

    override suspend fun fireEndPixels(fixtureId: String) {
        val endPixels = setOf(0, pixelsPerFixture - 1)
        _activeFixtures.update { it + fixtureId }
        _activePixels.update { it + (fixtureId to endPixels) }
    }

    override suspend fun turnOffFixture(fixtureId: String) {
        _activeFixtures.update { it - fixtureId }
        _activePixels.update { it - fixtureId }
    }

    override suspend fun blackout() {
        _activeFixtures.value = emptySet()
        _activePixels.value = emptyMap()
    }
}

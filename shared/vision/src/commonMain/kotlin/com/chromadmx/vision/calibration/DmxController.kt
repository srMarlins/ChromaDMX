package com.chromadmx.vision.calibration

/**
 * Interface for controlling DMX fixtures during the scan pipeline.
 *
 * The scan orchestrator uses this to fire individual fixtures at full white
 * and turn them off. Implementations connect to the actual DMX output layer.
 */
interface DmxController {
    /**
     * Fire a fixture at full white (all channels to 255).
     * @param fixtureId the fixture to activate
     */
    suspend fun fireFixture(fixtureId: String)

    /**
     * Turn off a fixture (all channels to 0).
     * @param fixtureId the fixture to deactivate
     */
    suspend fun turnOffFixture(fixtureId: String)

    /**
     * Fire only the two end-pixels of a multi-cell fixture at full white.
     * Used for endpoint detection of LED bars.
     * @param fixtureId the fixture to partially activate
     */
    suspend fun fireEndPixels(fixtureId: String)

    /**
     * Turn off all fixtures (blackout).
     */
    suspend fun blackout()
}

package com.chromadmx.vision

import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.vision.calibration.DmxController
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Real implementation of [DmxController] that uses the [EffectEngine]
 * to activate fixtures during the vision scan process.
 *
 * It temporarily stops the engine loop and writes directly to the
 * [EffectEngine.colorOutput] buffer to ensure only the target fixture
 * is illuminated.
 *
 * @param engine          The effect engine to control
 * @param fixturesProvider Provider for the current fixture list
 */
class RealDmxController(
    private val engine: EffectEngine,
    private val fixturesProvider: () -> List<Fixture3D>
) : DmxController {

    private val mutex = Mutex()
    private var wasRunning: Boolean = false

    override suspend fun fireFixture(fixtureId: String) = mutex.withLock {
        prepareForCalibration()

        val fixtures = fixturesProvider()
        val index = fixtures.indexOfFirst { it.fixture.fixtureId == fixtureId }

        if (index != -1) {
            val writeSlot = engine.colorOutput.writeSlot()
            writeSlot.fill(Color.BLACK)
            writeSlot[index] = Color.WHITE
            engine.colorOutput.swapWrite()
        }
    }

    override suspend fun turnOffFixture(fixtureId: String) = mutex.withLock {
        val writeSlot = engine.colorOutput.writeSlot()
        writeSlot.fill(Color.BLACK)
        engine.colorOutput.swapWrite()
    }

    override suspend fun fireEndPixels(fixtureId: String) = mutex.withLock {
        // For simple integration tests, we treat this the same as fireFixture.
        // In a full implementation, this would look up the fixture's pixel count
        // and only set the first/last elements if it's a pixel bar.
        fireFixture(fixtureId)
    }

    override suspend fun blackout() = mutex.withLock {
        val writeSlot = engine.colorOutput.writeSlot()
        writeSlot.fill(Color.BLACK)
        engine.colorOutput.swapWrite()

        if (wasRunning) {
            engine.start()
        }
    }

    private fun prepareForCalibration() {
        if (engine.isRunning) {
            wasRunning = true
            engine.stop()
        }

        // Ensure buffer is clear
        val writeSlot = engine.colorOutput.writeSlot()
        writeSlot.fill(Color.BLACK)
        engine.colorOutput.swapWrite()
    }
}

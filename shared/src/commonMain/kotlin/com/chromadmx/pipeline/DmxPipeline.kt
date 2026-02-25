package com.chromadmx.pipeline

import com.chromadmx.core.model.Fixture3D
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.networking.output.DmxOutputService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-frequency bridge between the [EffectEngine] and [DmxOutputService].
 *
 * Runs a loop (default 40Hz) that:
 * 1. Collects the latest [Fixture3D] list.
 * 2. Reads the current color frame from the engine's triple buffer.
 * 3. Maps each fixture's color to the corresponding DMX universe and channels.
 * 4. Updates the [DmxOutputService] with the new multi-universe frame.
 *
 * @param scope           Coroutine scope for the pipeline loop
 * @param engine          The effect engine providing color data
 * @param dmxOutput       The DMX output service sending packets
 * @param fixturesProvider Provider for the current mapped fixture list
 * @param syncRateHz      How often to sync engine to DMX (default 40Hz)
 */
class DmxPipeline(
    private val scope: CoroutineScope,
    private val engine: EffectEngine,
    private val dmxOutput: DmxOutputService,
    private val fixturesProvider: () -> List<Fixture3D>,
    private val syncRateHz: Int = 40
) {
    private var pipelineJob: Job? = null

    /** Interval between sync frames in milliseconds. */
    private val syncIntervalMs: Long get() = (1000L / syncRateHz)

    /** Whether the pipeline is currently running. */
    val isRunning: Boolean get() = pipelineJob?.isActive == true

    /**
     * Start the pipeline loop.
     */
    fun start() {
        if (isRunning) return

        pipelineJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val startTime = monotonicTimeMs()

                syncFrame()

                val elapsed = monotonicTimeMs() - startTime
                val remaining = syncIntervalMs - elapsed
                if (remaining > 0) {
                    delay(remaining)
                }
            }
        }
    }

    /**
     * Stop the pipeline loop.
     */
    fun stop() {
        pipelineJob?.cancel()
        pipelineJob = null
    }

    /**
     * Execute a single sync frame.
     */
    internal fun syncFrame() {
        val fixtures = fixturesProvider()
        if (fixtures.isEmpty()) return

        val colors = engine.colorOutput.readSlot()
        val universes = mutableMapOf<Int, ByteArray>()

        // Map colors to DMX channels
        for (i in fixtures.indices) {
            if (i >= colors.size) break

            val fixture3D = fixtures[i]
            val fixture = fixture3D.fixture
            val color = colors[i]

            val universeData = universes.getOrPut(fixture.universeId) { ByteArray(512) }

            // Map RGB channels (assuming standard RGB for now)
            val r = (color.r * 255f).toInt().coerceIn(0, 255).toByte()
            val g = (color.g * 255f).toInt().coerceIn(0, 255).toByte()
            val b = (color.b * 255f).toInt().coerceIn(0, 255).toByte()

            val start = fixture.channelStart - 1 // 1-based to 0-based
            if (start in 0..509) {
                universeData[start] = r
                universeData[start + 1] = g
                universeData[start + 2] = b
            }
        }

        if (universes.isNotEmpty()) {
            dmxOutput.updateFrame(universes)
        }
    }
}

/**
 * Platform-agnostic monotonic clock in milliseconds.
 * In a full KMP project, this would be an 'expect' fun.
 * For now, we use a simple implementation or alias.
 */
private fun monotonicTimeMs(): Long =
    (kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds)

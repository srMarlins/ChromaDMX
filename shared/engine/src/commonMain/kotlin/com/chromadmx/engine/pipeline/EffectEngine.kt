package com.chromadmx.engine.pipeline

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.engine.effect.EffectStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * The main effect engine loop.
 *
 * Each frame (targeting 60 fps / ~16.67 ms):
 * 1. Read the current [BeatState] from [beatStateProvider].
 * 2. Evaluate the [effectStack] at every fixture position.
 * 3. Write the resulting color array into the [colorOutput] triple buffer.
 *
 * The engine runs on [Dispatchers.Default] to avoid blocking the UI thread.
 * The DMX output thread reads from the other side of the triple buffer.
 */
class EffectEngine(
    private val scope: CoroutineScope,
    private var fixtures: List<Fixture3D> = emptyList()
) {
    /** The compositing effect stack evaluated each frame. */
    val effectStack: EffectStack = EffectStack()

    /** Triple-buffered color output: one [Color] per fixture. */
    private var _colorOutput: TripleBuffer<Array<Color>> = createBuffer(fixtures)

    val colorOutput: TripleBuffer<Array<Color>> get() = _colorOutput

    private fun createBuffer(fixtures: List<Fixture3D>): TripleBuffer<Array<Color>> =
        TripleBuffer(
            initialA = Array(fixtures.size) { Color.BLACK },
            initialB = Array(fixtures.size) { Color.BLACK },
            initialC = Array(fixtures.size) { Color.BLACK }
        )

    /** Update the fixture list and recreate the output buffers. */
    fun updateFixtures(newFixtures: List<Fixture3D>) {
        fixtures = newFixtures
        _colorOutput = createBuffer(newFixtures)
    }

    /** Provider for the current beat state. Defaults to [BeatState.IDLE]. */
    var beatStateProvider: () -> BeatState = { BeatState.IDLE }

    /** Target frame interval in milliseconds. ~60 fps. */
    var frameIntervalMs: Long = 16L

    private var engineJob: Job? = null
    private val timeSource = TimeSource.Monotonic
    private var startMark: TimeSource.Monotonic.ValueTimeMark? = null

    /** True while the engine loop is running. */
    val isRunning: Boolean get() = engineJob?.isActive == true

    /**
     * Start the engine loop. If already running, this is a no-op.
     */
    fun start() {
        if (isRunning) return
        startMark = timeSource.markNow()

        engineJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val frameStartMark = timeSource.markNow()
                tick()
                val elapsedMs = frameStartMark.elapsedNow().inWholeMilliseconds
                val remaining = frameIntervalMs - elapsedMs
                if (remaining > 0) {
                    delay(remaining)
                }
            }
        }
    }

    /**
     * Stop the engine loop.
     */
    fun stop() {
        engineJob?.cancel()
        engineJob = null
    }

    /**
     * Execute one frame of the engine.  Public so it can be called
     * directly in tests without starting the coroutine loop.
     */
    fun tick() {
        val mark = startMark ?: timeSource.markNow().also { startMark = it }
        val time = mark.elapsedNow().inWholeMilliseconds / 1000f
        val beat = beatStateProvider()

        val writeSlot = colorOutput.writeSlot()
        for (i in fixtures.indices) {
            writeSlot[i] = effectStack.evaluate(fixtures[i].position, time, beat)
        }
        colorOutput.swapWrite()
    }

    /**
     * Evaluate a single frame at the given [time] and [beat], returning
     * the color array directly. Useful for benchmarking and testing
     * without involving the triple buffer.
     */
    fun evaluateFrame(time: Float, beat: BeatState): Array<Color> {
        return Array(fixtures.size) { i ->
            effectStack.evaluate(fixtures[i].position, time, beat)
        }
    }
}

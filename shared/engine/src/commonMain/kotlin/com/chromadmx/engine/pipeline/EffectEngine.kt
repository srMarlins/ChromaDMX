package com.chromadmx.engine.pipeline

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureOutput
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
 * 4. If movement layers are present, write [FixtureOutput] into [fixtureOutputBuffer].
 *
 * The engine runs on [Dispatchers.Default] to avoid blocking the UI thread.
 * The DMX output thread reads from the other side of the triple buffer.
 */
class EffectEngine(
    private val scope: CoroutineScope,
    initialFixtures: List<Fixture3D> = emptyList()
) {
    /** The compositing effect stack evaluated each frame. */
    val effectStack: EffectStack = EffectStack()

    /** Current fixture list. Updated via [updateFixtures]. */
    var fixtures: List<Fixture3D> = initialFixtures
        private set

    /** Triple-buffered color output: one [Color] per fixture. */
    var colorOutput: TripleBuffer<Array<Color>> = TripleBuffer(
        initialA = Array(fixtures.size) { Color.BLACK },
        initialB = Array(fixtures.size) { Color.BLACK },
        initialC = Array(fixtures.size) { Color.BLACK }
    )
        private set

    /** Triple-buffered fixture output: one [FixtureOutput] per fixture (includes movement data). */
    var fixtureOutputBuffer: TripleBuffer<Array<FixtureOutput>> = TripleBuffer(
        initialA = Array(fixtures.size) { FixtureOutput.DEFAULT },
        initialB = Array(fixtures.size) { FixtureOutput.DEFAULT },
        initialC = Array(fixtures.size) { FixtureOutput.DEFAULT }
    )
        private set

    /**
     * Replace the fixture list and reinitialize triple buffers to match the new size.
     * Safe to call while the engine is running — the next tick will use the new fixtures.
     */
    fun updateFixtures(newFixtures: List<Fixture3D>) {
        // Order matters: resize buffers BEFORE updating fixtures so that a
        // concurrent tick() never sees more fixtures than buffer slots.
        colorOutput = TripleBuffer(
            initialA = Array(newFixtures.size) { Color.BLACK },
            initialB = Array(newFixtures.size) { Color.BLACK },
            initialC = Array(newFixtures.size) { Color.BLACK }
        )
        fixtureOutputBuffer = TripleBuffer(
            initialA = Array(newFixtures.size) { FixtureOutput.DEFAULT },
            initialB = Array(newFixtures.size) { FixtureOutput.DEFAULT },
            initialC = Array(newFixtures.size) { FixtureOutput.DEFAULT }
        )
        fixtures = newFixtures
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

        // Snapshot mutable state so a concurrent updateFixtures() can't cause
        // a size mismatch between the fixture list and the buffer arrays.
        val curFixtures = fixtures
        val curColorOutput = colorOutput
        val curFixtureOutput = fixtureOutputBuffer

        if (curFixtures.isEmpty()) return

        // Prepare frame once (O(Layers * Params))
        val evaluator = effectStack.buildFrame(time, beat)

        val colorSlot = curColorOutput.writeSlot()
        val hasMovement = evaluator.hasMovementLayers

        if (hasMovement) {
            val fixtureSlot = curFixtureOutput.writeSlot()
            for (i in curFixtures.indices) {
                val output = evaluator.evaluateFixtureOutput(curFixtures[i].position)
                colorSlot[i] = output.color
                fixtureSlot[i] = output
            }
            curFixtureOutput.swapWrite()
        } else {
            for (i in curFixtures.indices) {
                colorSlot[i] = evaluator.evaluate(curFixtures[i].position)
            }
        }
        curColorOutput.swapWrite()
    }

    /**
     * Evaluate a single frame at the given [time] and [beat], returning
     * the color array directly. Useful for benchmarking and testing
     * without involving the triple buffer.
     */
    fun evaluateFrame(time: Float, beat: BeatState): Array<Color> {
        val evaluator = effectStack.buildFrame(time, beat)
        return Array(fixtures.size) { i ->
            evaluator.evaluate(fixtures[i].position)
        }
    }

    /**
     * Evaluate a single frame at the given [time] and [beat], returning
     * the full [FixtureOutput] array including movement data.
     */
    fun evaluateFrameOutput(time: Float, beat: BeatState): Array<FixtureOutput> {
        val evaluator = effectStack.buildFrame(time, beat)
        return Array(fixtures.size) { i ->
            evaluator.evaluateFixtureOutput(fixtures[i].position)
        }
    }
}

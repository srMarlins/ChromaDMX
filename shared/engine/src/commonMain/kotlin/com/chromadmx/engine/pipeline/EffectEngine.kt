package com.chromadmx.engine.pipeline

import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureOutput
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectStack
import kotlinx.atomicfu.atomic
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

    /**
     * Immutable snapshot of the engine state: fixtures + matching triple buffers.
     * Replaced atomically by [updateFixtures] so [tick] always reads a
     * consistent triple (fixtures, colorOutput, fixtureOutputBuffer).
     *
     * Using an atomic reference prevents a race where [tick] could see
     * the new TripleBuffer but the old (empty) fixture list, causing a
     * permanent early-return on `curFixtures.isEmpty()`.
     */
    data class Snapshot(
        val fixtures: List<Fixture3D>,
        val normalizedPositions: List<Vec3>,
        val colorOutput: TripleBuffer<Array<Color>>,
        val fixtureOutputBuffer: TripleBuffer<Array<FixtureOutput>>,
    )

    private val _snapshot = atomic(buildSnapshot(initialFixtures))

    /** Current fixture list. Updated via [updateFixtures]. */
    val fixtures: List<Fixture3D> get() = _snapshot.value.fixtures

    /** Triple-buffered color output: one [Color] per fixture. */
    val colorOutput: TripleBuffer<Array<Color>> get() = _snapshot.value.colorOutput

    /** Triple-buffered fixture output: one [FixtureOutput] per fixture (includes movement data). */
    val fixtureOutputBuffer: TripleBuffer<Array<FixtureOutput>> get() = _snapshot.value.fixtureOutputBuffer

    /**
     * Replace the fixture list and reinitialize triple buffers to match the new size.
     * Safe to call while the engine is running — the next tick will use the new fixtures.
     *
     * All three fields are updated atomically via a single [Snapshot] swap,
     * so [tick] never observes a buffer whose size doesn't match the fixture list.
     */
    fun updateFixtures(newFixtures: List<Fixture3D>) {
        _snapshot.value = buildSnapshot(newFixtures)
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

        // Read one atomic snapshot — fixtures and buffers are always consistent.
        val snap = _snapshot.value
        val curFixtures = snap.fixtures
        val positions = snap.normalizedPositions
        val curColorOutput = snap.colorOutput
        val curFixtureOutput = snap.fixtureOutputBuffer

        if (curFixtures.isEmpty()) return

        // Prepare frame once (O(Layers * Params))
        val evaluator = effectStack.buildFrame(time, beat)

        val colorSlot = curColorOutput.writeSlot()
        val hasMovement = evaluator.hasMovementLayers

        // Guard against buffer/fixture size mismatch that could occur if
        // a concurrent updateFixtures() sneaks in between the atomic read
        // and here (impossible with the snapshot design, but defensive).
        val count = minOf(curFixtures.size, colorSlot.size)

        if (hasMovement) {
            val fixtureSlot = curFixtureOutput.writeSlot()
            val fixtureCount = minOf(count, fixtureSlot.size)
            for (i in 0 until fixtureCount) {
                val output = evaluator.evaluateFixtureOutput(positions[i])
                colorSlot[i] = output.color
                fixtureSlot[i] = output
            }
            curFixtureOutput.swapWrite()
        } else {
            for (i in 0 until count) {
                colorSlot[i] = evaluator.evaluate(positions[i])
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
        val snap = _snapshot.value
        val positions = snap.normalizedPositions
        val evaluator = effectStack.buildFrame(time, beat)
        return Array(positions.size) { i ->
            evaluator.evaluate(positions[i])
        }
    }

    /**
     * Evaluate a single frame at the given [time] and [beat], returning
     * the full [FixtureOutput] array including movement data.
     */
    fun evaluateFrameOutput(time: Float, beat: BeatState): Array<FixtureOutput> {
        val snap = _snapshot.value
        val positions = snap.normalizedPositions
        val evaluator = effectStack.buildFrame(time, beat)
        return Array(positions.size) { i ->
            evaluator.evaluateFixtureOutput(positions[i])
        }
    }

    companion object {
        /**
         * Build a [Snapshot] from a fixture list, normalizing positions to [0, 1]
         * on each axis so spatial effects work at any venue scale.
         *
         * Degenerate axes (all fixtures share the same coordinate) are mapped to 0
         * so effects centered at origin still light them.
         */
        internal fun buildSnapshot(fixtures: List<Fixture3D>): Snapshot {
            val size = fixtures.size
            return Snapshot(
                fixtures = fixtures,
                normalizedPositions = normalizePositions(fixtures),
                colorOutput = TripleBuffer(
                    initialA = Array(size) { Color.BLACK },
                    initialB = Array(size) { Color.BLACK },
                    initialC = Array(size) { Color.BLACK }
                ),
                fixtureOutputBuffer = TripleBuffer(
                    initialA = Array(size) { FixtureOutput.DEFAULT },
                    initialB = Array(size) { FixtureOutput.DEFAULT },
                    initialC = Array(size) { FixtureOutput.DEFAULT }
                ),
            )
        }

        /**
         * Normalize fixture positions to [0, 1] on each active axis.
         * Axes where all fixtures share the same value ("degenerate") are
         * mapped to 0 to keep them at the effect origin.
         */
        internal fun normalizePositions(fixtures: List<Fixture3D>): List<Vec3> {
            if (fixtures.isEmpty()) return emptyList()
            if (fixtures.size == 1) return listOf(Vec3(0f, 0f, 0f))

            val minX = fixtures.minOf { it.position.x }
            val maxX = fixtures.maxOf { it.position.x }
            val minY = fixtures.minOf { it.position.y }
            val maxY = fixtures.maxOf { it.position.y }
            val minZ = fixtures.minOf { it.position.z }
            val maxZ = fixtures.maxOf { it.position.z }

            val rangeX = maxX - minX
            val rangeY = maxY - minY
            val rangeZ = maxZ - minZ

            return fixtures.map { f ->
                Vec3(
                    x = if (rangeX > 0.001f) (f.position.x - minX) / rangeX else 0f,
                    y = if (rangeY > 0.001f) (f.position.y - minY) / rangeY else 0f,
                    z = if (rangeZ > 0.001f) (f.position.z - minZ) / rangeZ else 0f,
                )
            }
        }
    }
}

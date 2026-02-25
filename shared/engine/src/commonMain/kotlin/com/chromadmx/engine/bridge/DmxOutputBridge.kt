package com.chromadmx.engine.bridge

import com.chromadmx.core.model.Color
import com.chromadmx.engine.pipeline.TripleBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Coroutine-based bridge that reads from the engine's triple buffer
 * and feeds converted DMX data to an output callback.
 *
 * Runs at a configurable rate (default 40Hz to match DMX output).
 */
class DmxOutputBridge(
    private val colorOutput: TripleBuffer<Array<Color>>,
    private val dmxBridge: DmxBridge,
    private val onFrame: (Map<Int, ByteArray>) -> Unit,
    private val scope: CoroutineScope,
    private val intervalMs: Long = 25L // 40Hz
) {
    private var job: Job? = null
    val isRunning: Boolean get() = job?.isActive == true

    fun start() {
        if (isRunning) return
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (colorOutput.swapRead()) {
                    val colors = colorOutput.readSlot()
                    val frame = dmxBridge.convert(colors)
                    if (frame.isNotEmpty()) {
                        onFrame(frame)
                    }
                }
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

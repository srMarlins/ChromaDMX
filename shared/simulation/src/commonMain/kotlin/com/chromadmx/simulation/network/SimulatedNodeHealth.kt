package com.chromadmx.simulation.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

/**
 * Event emitted by [SimulatedNodeHealth] to indicate network changes.
 */
sealed interface NodeHealthEvent {
    data class LatencySpike(val nodeIp: String, val latencyMs: Long) : NodeHealthEvent
    data class NodeDropped(val nodeIp: String) : NodeHealthEvent
    data class NodeRecovered(val nodeIp: String) : NodeHealthEvent
    data class PacketLoss(val nodeIp: String, val lossPercent: Float) : NodeHealthEvent
}

/**
 * Simulates network health fluctuations based on a [NetworkProfile].
 *
 * Emits [NodeHealthEvent]s that consumers (like the UI) can observe
 * to test degraded-network scenarios without real hardware.
 *
 * @param profile           Network behavior profile to simulate
 * @param nodeIps           IP addresses of the simulated nodes
 * @param random            Random source (injectable for deterministic tests)
 * @param coroutineContext  Context for launching coroutines (injectable for tests)
 */
class SimulatedNodeHealth(
    private val profile: NetworkProfile,
    private val nodeIps: List<String>,
    private val random: Random = Random.Default,
    private val coroutineContext: CoroutineContext = Dispatchers.Default,
) {
    private val _events = MutableSharedFlow<NodeHealthEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NodeHealthEvent> = _events.asSharedFlow()

    private var scope: CoroutineScope? = null

    fun start() {
        stop()
        if (nodeIps.isEmpty()) return
        scope = CoroutineScope(SupervisorJob() + coroutineContext)
        when (profile) {
            NetworkProfile.Stable -> {} // No events — everything is fine
            NetworkProfile.Flaky -> startFlakyProfile()
            NetworkProfile.PartialFailure -> startPartialFailureProfile()
            NetworkProfile.Overloaded -> startOverloadedProfile()
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
    }

    private fun startFlakyProfile() {
        scope?.launch {
            while (true) {
                delay(random.nextLong(2000, 5000))
                val nodeIp = nodeIps.random(random)
                val event = if (random.nextFloat() < 0.6f) {
                    NodeHealthEvent.LatencySpike(nodeIp, random.nextLong(150, 500))
                } else {
                    NodeHealthEvent.PacketLoss(nodeIp, random.nextFloat() * 0.3f)
                }
                _events.emit(event)
            }
        }
    }

    private fun startPartialFailureProfile() {
        scope?.launch {
            // After 5-15 seconds, drop a random node
            delay(random.nextLong(5000, 15000))
            val droppedIp = nodeIps.random(random)
            _events.emit(NodeHealthEvent.NodeDropped(droppedIp))

            // Maybe recover after 10-30 seconds
            delay(random.nextLong(10000, 30000))
            if (random.nextFloat() < 0.5f) {
                _events.emit(NodeHealthEvent.NodeRecovered(droppedIp))
            }
        }
    }

    private fun startOverloadedProfile() {
        scope?.launch {
            while (true) {
                delay(random.nextLong(1000, 3000))
                for (nodeIp in nodeIps) {
                    _events.emit(
                        NodeHealthEvent.LatencySpike(nodeIp, random.nextLong(200, 1000))
                    )
                }
            }
        }
    }
}

package com.chromadmx.simulation.network

import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.core.model.DmxNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.chromadmx.networking.discovery.currentTimeMillis
import kotlin.coroutines.CoroutineContext

/**
 * Simulated fixture discovery that emits fake nodes with staggered timing
 * to mimic real network discovery behavior.
 *
 * @param nodes           Pre-configured list of nodes to "discover"
 * @param baseDelayMs       Initial delay before first node appears
 * @param perNodeDelayMs    Incremental delay between each node discovery
 * @param keepAliveIntervalMs How often to refresh node timestamps (mimics ArtPoll replies)
 * @param coroutineContext  Context for launching coroutines (injectable for tests)
 */
class SimulatedDiscovery(
    private val nodes: List<DmxNode> = defaultNodes(),
    private val baseDelayMs: Long = 150L,
    private val perNodeDelayMs: Long = 80L,
    private val keepAliveIntervalMs: Long = 3_000L,
    private val coroutineContext: CoroutineContext = Dispatchers.Default,
) : FixtureDiscovery {

    private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
    override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scope: CoroutineScope? = null

    override fun startScan() {
        stopScan()
        _discoveredNodes.value = emptyList()
        _isScanning.value = true
        scope = CoroutineScope(SupervisorJob() + coroutineContext)

        // Discovery phase — emits nodes with staggered timing, then completes.
        scope!!.launch {
            delay(baseDelayMs)
            val now = currentTimeMillis()
            val discovered = mutableListOf<DmxNode>()
            for ((index, node) in nodes.withIndex()) {
                delay(perNodeDelayMs)
                discovered.add(node.copy(
                    lastSeenMs = currentTimeMillis(),
                    firstSeenMs = now,
                    latencyMs = 5L + (index * 3L)
                ))
                _discoveredNodes.value = discovered.toList()
            }
            _isScanning.value = false
        }

        // Keep-alive phase — periodically refreshes node timestamps to prevent
        // health checks from marking simulated nodes as lost. Runs as a separate
        // coroutine so the discovery phase completes independently.
        // Set keepAliveIntervalMs <= 0 to disable (e.g. in unit tests).
        if (keepAliveIntervalMs > 0) {
            scope!!.launch {
                // Wait for discovery to finish before starting keep-alive.
                delay(baseDelayMs + (nodes.size * perNodeDelayMs) + keepAliveIntervalMs)
                while (isActive) {
                    val refreshTime = currentTimeMillis()
                    _discoveredNodes.value = _discoveredNodes.value.map { n ->
                        n.copy(lastSeenMs = refreshTime)
                    }
                    delay(keepAliveIntervalMs)
                }
            }
        }
    }

    override fun stopScan() {
        scope?.cancel()
        scope = null
        _isScanning.value = false
    }

    companion object {
        fun defaultNodes(): List<DmxNode> = listOf(
            DmxNode(
                ipAddress = "192.168.1.100",
                macAddress = "de:ad:be:ef:00:01",
                shortName = "SimNode-1",
                longName = "Simulated Art-Net Node 1",
                numPorts = 1,
                universes = listOf(0),
            ),
            DmxNode(
                ipAddress = "192.168.1.101",
                macAddress = "de:ad:be:ef:00:02",
                shortName = "SimNode-2",
                longName = "Simulated Art-Net Node 2",
                numPorts = 1,
                universes = listOf(1),
            ),
            DmxNode(
                ipAddress = "192.168.1.102",
                macAddress = "de:ad:be:ef:00:03",
                shortName = "SimNode-3",
                longName = "Simulated Art-Net Node 3",
                numPorts = 2,
                universes = listOf(2, 3),
            ),
        )
    }
}

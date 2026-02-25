package com.chromadmx.ui.components.network

import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.ui.viewmodel.MascotViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel that bridges [NodeDiscovery] to the network-health UI layer.
 *
 * Periodically re-evaluates discovered nodes and maps them to [NodeStatus]
 * objects with derived [NodeHealth]. Exposes summary metrics and an alert
 * flag for mascot integration.
 *
 * @param nodeDiscovery  The discovery service providing live node data.
 * @param mascotViewModel Optional mascot ViewModel for alert bubbles.
 * @param scope          Coroutine scope for background collection.
 * @param refreshIntervalMs How often to recompute health (default 2 seconds).
 */
class NetworkHealthViewModel(
    private val nodeDiscovery: NodeDiscovery,
    private val mascotViewModel: MascotViewModel? = null,
    private val scope: CoroutineScope,
    private val refreshIntervalMs: Long = DEFAULT_REFRESH_INTERVAL_MS,
) {
    private val _nodes = MutableStateFlow<List<NodeStatus>>(emptyList())

    /** Live list of node statuses with health classification. */
    val nodes: StateFlow<List<NodeStatus>> = _nodes.asStateFlow()

    private val _healthySummary = MutableStateFlow("No nodes")

    /** Human-readable summary string, e.g. "3/5 online". */
    val healthySummary: StateFlow<String> = _healthySummary.asStateFlow()

    private val _hasAlert = MutableStateFlow(false)

    /** True when any node is in [NodeHealth.LOST] state. */
    val hasAlert: StateFlow<Boolean> = _hasAlert.asStateFlow()

    private var refreshJob: Job? = null

    /**
     * Track which node keys have already triggered a mascot LOST alert,
     * so we don't spam the user with repeated alerts for the same node.
     */
    private val alertedNodeKeys = mutableSetOf<String>()

    init {
        startRefreshLoop()
    }

    private fun startRefreshLoop() {
        refreshJob = scope.launch {
            while (isActive) {
                refresh()
                delay(refreshIntervalMs)
            }
        }
    }

    /**
     * Recompute node health from the current discovery snapshot.
     *
     * Visible for testing — the refresh loop calls this automatically.
     */
    internal fun refresh(currentTimeMs: Long = systemTimeMs()) {
        val discoveredNodes: Map<String, DmxNode> = nodeDiscovery.nodes.value
        val statuses = discoveredNodes.values.map { it.toNodeStatus(currentTimeMs) }

        _nodes.value = statuses

        val healthyCount = statuses.count { it.health == NodeHealth.HEALTHY }
        val total = statuses.size
        _healthySummary.value = if (total == 0) {
            "No nodes"
        } else {
            "$healthyCount/$total online"
        }

        val lostNodes = statuses.filter { it.health == NodeHealth.LOST }
        _hasAlert.value = lostNodes.isNotEmpty()

        // Mascot integration: alert for newly lost nodes.
        if (mascotViewModel != null) {
            for (lost in lostNodes) {
                if (alertedNodeKeys.add(lost.nodeKey)) {
                    mascotViewModel.triggerAlert("Node ${lost.name} lost connection!")
                }
            }
            // Clear alert tracking for nodes that have recovered.
            val currentLostKeys = lostNodes.map { it.nodeKey }.toSet()
            alertedNodeKeys.retainAll(currentLostKeys)
        }
    }

    fun onCleared() {
        refreshJob?.cancel()
    }

    companion object {
        /** Default refresh interval for health recalculation. */
        const val DEFAULT_REFRESH_INTERVAL_MS: Long = 2_000L
    }
}

/**
 * Platform-agnostic current time accessor.
 *
 * Uses kotlin.time for monotonic approximation; callers in tests can
 * override via the [NetworkHealthViewModel.refresh] currentTimeMs parameter.
 */
internal expect fun systemTimeMs(): Long

package com.chromadmx.ui.components.network

import com.chromadmx.core.model.DmxNode

/**
 * Health classification for a discovered DMX node based on its last-seen timestamp.
 */
enum class NodeHealth {
    /** Node responded within the last 5 seconds. */
    HEALTHY,
    /** Node responded between 5 and 15 seconds ago. */
    DEGRADED,
    /** Node has not responded for more than 15 seconds. */
    LOST
}

/**
 * UI-facing status model for a single DMX node.
 *
 * @property nodeKey   Unique key from [DmxNode.nodeKey] (MAC or IP fallback).
 * @property name      Display name (shortName or IP fallback).
 * @property ip        Node IP address.
 * @property universes Universes handled by this node.
 * @property health    Derived health classification.
 * @property lastSeen  Epoch milliseconds when the node was last seen.
 */
data class NodeStatus(
    val nodeKey: String,
    val name: String,
    val ip: String,
    val universes: List<Int>,
    val health: NodeHealth,
    val lastSeen: Long,
)

/**
 * Derive [NodeHealth] from a [DmxNode]'s lastSeenMs relative to [currentTimeMs].
 *
 * Thresholds:
 * - HEALTHY:  last seen < 5 000 ms ago
 * - DEGRADED: last seen < 15 000 ms ago
 * - LOST:     last seen >= 15 000 ms ago
 */
fun DmxNode.toNodeHealth(currentTimeMs: Long): NodeHealth {
    val elapsed = currentTimeMs - lastSeenMs
    return when {
        elapsed < HEALTHY_THRESHOLD_MS -> NodeHealth.HEALTHY
        elapsed < DEGRADED_THRESHOLD_MS -> NodeHealth.DEGRADED
        else -> NodeHealth.LOST
    }
}

/**
 * Convert a [DmxNode] to a UI-friendly [NodeStatus].
 */
fun DmxNode.toNodeStatus(currentTimeMs: Long): NodeStatus = NodeStatus(
    nodeKey = nodeKey,
    name = shortName.ifEmpty { ipAddress },
    ip = ipAddress,
    universes = universes,
    health = toNodeHealth(currentTimeMs),
    lastSeen = lastSeenMs,
)

/**
 * Compute the compact display text when there are more than [maxVisible] nodes.
 *
 * @param totalNodes  Total number of nodes.
 * @param maxVisible  Maximum hearts to show before truncating.
 * @return "+N" text if overflow, or null if all fit.
 */
fun compactOverflowText(totalNodes: Int, maxVisible: Int = MAX_VISIBLE_HEARTS): String? {
    val overflow = totalNodes - maxVisible
    return if (overflow > 0) "+$overflow" else null
}

/** Threshold below which a node is considered HEALTHY (5 seconds). */
const val HEALTHY_THRESHOLD_MS: Long = 5_000L

/** Threshold below which a node is considered DEGRADED (15 seconds). */
const val DEGRADED_THRESHOLD_MS: Long = 15_000L

/** Maximum number of heart icons shown in the compact bar. */
const val MAX_VISIBLE_HEARTS: Int = 3

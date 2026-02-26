package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/**
 * Persisted snapshot of a discovered DMX node.
 * Used for comparison on subsequent launches and to maintain a device history.
 */
@Serializable
data class KnownNode(
    val nodeKey: String,
    val ipAddress: String,
    val shortName: String,
    val longName: String,
    val lastSeenMs: Long
)

/**
 * Result of comparing current network state against known/previous state.
 */
data class TopologyDiff(
    val newNodes: List<DmxNode>,
    val lostNodes: List<KnownNode>
)

/**
 * Extension to convert a [DmxNode] to its persistent [KnownNode] form.
 */
fun DmxNode.toKnownNode(): KnownNode = KnownNode(
    nodeKey = nodeKey,
    ipAddress = ipAddress,
    shortName = shortName,
    longName = longName,
    lastSeenMs = lastSeenMs
)

package com.chromadmx.core.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.model.DmxNode
import com.chromadmx.core.model.KnownNode
import com.chromadmx.core.model.TopologyDiff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for persisting network state and detecting topology changes.
 */
class NetworkStateRepository(private val db: ChromaDmxDatabase) {

    private val queries = db.networkQueries

    /**
     * Returns a flow of all known nodes, ordered by last seen time.
     */
    fun knownNodes(): Flow<List<KnownNode>> {
        return queries.selectAllKnownNodes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    KnownNode(
                        nodeKey = row.node_key,
                        ipAddress = row.ip_address,
                        shortName = row.short_name,
                        longName = row.long_name,
                        lastSeenMs = row.last_seen_ms
                    )
                }
            }
    }

    /**
     * Saves the list of currently discovered nodes as known nodes.
     */
    suspend fun saveKnownNodes(nodes: List<KnownNode>) {
        db.transaction {
            nodes.forEach { node ->
                queries.insertOrReplaceKnownNode(
                    node_key = node.nodeKey,
                    ip_address = node.ipAddress,
                    short_name = node.shortName,
                    long_name = node.longName,
                    last_seen_ms = node.lastSeenMs
                )
            }
        }
    }

    /**
     * Compares the current network topology against the persisted known nodes.
     * Returns a [TopologyDiff] containing new and lost nodes.
     */
    suspend fun detectTopologyChanges(currentNodes: List<DmxNode>): TopologyDiff {
        val known = knownNodes().first()
        val knownKeys = known.map { it.nodeKey }.toSet()
        val currentKeys = currentNodes.map { it.nodeKey }.toSet()

        val newNodes = currentNodes.filter { it.nodeKey !in knownKeys }
        val lostNodes = known.filter { it.nodeKey !in currentKeys }

        return TopologyDiff(
            newNodes = newNodes,
            lostNodes = lostNodes
        )
    }
}

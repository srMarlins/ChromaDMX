package com.chromadmx.agent.controller

import com.chromadmx.core.model.*
import com.chromadmx.core.persistence.FixtureLibrary
import com.chromadmx.engine.pipeline.EffectEngine
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real [FixtureController] bridging to the fixture management layer.
 *
 * Wraps a [FixtureLibrary] and [EffectEngine] to provide operations
 * for listing, firing (identification flash), and grouping fixtures.
 *
 * @param library   Persistent fixture storage and state.
 * @param engine    Effect engine for real-time overrides (test fire).
 * @param scope     Coroutine scope for temporary override timers.
 */
class RealFixtureController(
    private val library: FixtureLibrary,
    private val engine: EffectEngine,
    private val scope: CoroutineScope,
) : FixtureController {

    private val lock = SynchronizedObject()

    override fun listFixtures(): List<Fixture3D> = library.fixtures.value

    override fun fireFixture(fixtureId: String, colorHex: String): Boolean {
        val fixtures = library.fixtures.value
        val index = fixtures.indexOfFirst { it.fixture.fixtureId == fixtureId }
        if (index < 0) return false

        // Parse color
        val color = try {
            val r = colorHex.substring(1, 3).toInt(16) / 255f
            val g = colorHex.substring(3, 5).toInt(16) / 255f
            val b = colorHex.substring(5, 7).toInt(16) / 255f
            Color(r, g, b)
        } catch (e: Exception) {
            Color.WHITE
        }

        // Apply temporary override
        engine.setOverride(index, color)
        scope.launch {
            delay(1000L)
            engine.setOverride(index, null)
        }
        return true
    }

    override fun setFixtureGroup(groupName: String, fixtureIds: List<String>): Boolean {
        val fixtures = library.fixtures.value
        val validIds = fixtureIds.filter { id ->
            fixtures.any { it.fixture.fixtureId == id }
        }
        if (validIds.isEmpty() && fixtureIds.isNotEmpty()) return false

        synchronized(lock) {
            // Find or create group
            val existingGroup = library.groups.value.find { it.name == groupName }
            val groupId = existingGroup?.id ?: groupName.lowercase().replace(" ", "_")

            if (existingGroup == null) {
                library.upsertGroup(FixtureGroup(groupId, groupName, Color.WHITE))
            }

            // Update fixtures
            val newFixtures = fixtures.map {
                if (validIds.contains(it.fixture.fixtureId)) {
                    it.copy(groupId = groupId)
                } else if (it.groupId == groupId) {
                    it.copy(groupId = null) // Remove from group if not in validIds
                } else {
                    it
                }
            }
            library.saveFixtures(newFixtures)
            engine.updateFixtures(newFixtures)
        }
        return true
    }
}

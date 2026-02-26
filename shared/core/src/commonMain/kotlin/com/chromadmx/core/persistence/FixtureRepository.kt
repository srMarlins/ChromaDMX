package com.chromadmx.core.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * Persistent grouping of fixtures for spatial effects and UI organization.
 *
 * @property groupId Unique identifier for the group.
 * @property name Human-readable group name (e.g., "Truss Left", "Floor Wash").
 * @property color Integer color value for UI rendering (ARGB).
 */
@Serializable
data class FixtureGroup(
    val groupId: String,
    val name: String,
    val color: Long,
)

/**
 * SQLDelight-backed repository for fixture and group persistence.
 *
 * Provides reactive [Flow]-based reads and synchronous writes for
 * fixture positions, group assignments, and group CRUD.
 */
class FixtureRepository(private val db: ChromaDmxDatabase) {

    private val queries get() = db.fixturesQueries

    /** Observe all fixtures as [Fixture3D] instances, ordered by name. */
    fun allFixtures(): Flow<List<Fixture3D>> {
        return queries.selectAllFixtures()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    Fixture3D(
                        fixture = Fixture(
                            fixtureId = row.fixture_id,
                            name = row.name,
                            channelStart = row.channel_start.toInt(),
                            channelCount = row.channel_count.toInt(),
                            universeId = row.universe_id.toInt(),
                            profileId = row.profile_id,
                        ),
                        position = Vec3(row.pos_x.toFloat(), row.pos_y.toFloat(), row.pos_z.toFloat()),
                        groupId = row.group_id,
                    )
                }
            }
    }

    /** Insert or replace a single fixture. */
    fun saveFixture(fixture: Fixture3D) {
        queries.insertOrReplaceFixture(
            fixture_id = fixture.fixture.fixtureId,
            name = fixture.fixture.name,
            channel_start = fixture.fixture.channelStart.toLong(),
            channel_count = fixture.fixture.channelCount.toLong(),
            universe_id = fixture.fixture.universeId.toLong(),
            profile_id = fixture.fixture.profileId,
            pos_x = fixture.position.x.toDouble(),
            pos_y = fixture.position.y.toDouble(),
            pos_z = fixture.position.z.toDouble(),
            group_id = fixture.groupId,
        )
    }

    /** Insert or replace all fixtures in a single transaction. */
    fun saveAll(fixtures: List<Fixture3D>) {
        db.transaction {
            fixtures.forEach { saveFixture(it) }
        }
    }

    /** Update only the position of an existing fixture. */
    fun updatePosition(fixtureId: String, position: Vec3) {
        queries.updateFixturePosition(
            pos_x = position.x.toDouble(),
            pos_y = position.y.toDouble(),
            pos_z = position.z.toDouble(),
            fixture_id = fixtureId,
        )
    }

    /** Update only the group assignment of an existing fixture. */
    fun updateGroup(fixtureId: String, groupId: String?) {
        queries.updateFixtureGroup(group_id = groupId, fixture_id = fixtureId)
    }

    /** Delete a fixture by its ID. */
    fun deleteFixture(fixtureId: String) {
        queries.deleteFixture(fixtureId)
    }

    /** Delete all fixtures. */
    fun deleteAll() {
        queries.deleteAllFixtures()
    }

    /** Observe all fixture groups, ordered by name. */
    fun allGroups(): Flow<List<FixtureGroup>> {
        return queries.selectAllGroups()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { FixtureGroup(it.group_id, it.name, it.color) }
            }
    }

    /** Insert or replace a fixture group. */
    fun saveGroup(group: FixtureGroup) {
        queries.insertOrReplaceGroup(group.groupId, group.name, group.color)
    }

    /** Delete a fixture group by its ID. */
    fun deleteGroup(groupId: String) {
        queries.deleteGroup(groupId)
    }
}

package com.chromadmx.core.persistence

import com.chromadmx.core.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the library of mapped fixtures and groups.
 *
 * Handles persistence and provides a reactive stream of the current rig.
 */
class FixtureLibrary(
    private val storage: FileStorage
) : SynchronizedObject() {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val fixturesFile = "fixtures.json"
    private val groupsFile = "groups.json"

    private val _fixtures = MutableStateFlow<List<Fixture3D>>(emptyList())
    val fixtures: StateFlow<List<Fixture3D>> = _fixtures.asStateFlow()

    private val _groups = MutableStateFlow<List<FixtureGroup>>(emptyList())
    val groups: StateFlow<List<FixtureGroup>> = _groups.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        synchronized(this) {
            val fixturesContent = storage.readFile(fixturesFile)
            if (fixturesContent != null) {
                try {
                    _fixtures.value = json.decodeFromString<List<Fixture3D>>(fixturesContent)
                } catch (e: Exception) {
                    _fixtures.value = emptyList()
                }
            }

            val groupsContent = storage.readFile(groupsFile)
            if (groupsContent != null) {
                try {
                    _groups.value = json.decodeFromString<List<FixtureGroup>>(groupsContent)
                } catch (e: Exception) {
                    _groups.value = emptyList()
                }
            }
        }
    }

    /** Save current fixtures to disk. */
    fun saveFixtures(newFixtures: List<Fixture3D>) {
        synchronized(this) {
            _fixtures.value = newFixtures
            storage.saveFile(fixturesFile, json.encodeToString(newFixtures))
        }
    }

    /** Save current groups to disk. */
    fun saveGroups(newGroups: List<FixtureGroup>) {
        synchronized(this) {
            _groups.value = newGroups
            storage.saveFile(groupsFile, json.encodeToString(newGroups))
        }
    }

    /** Update a single fixture's position or group. */
    fun updateFixture(index: Int, update: (Fixture3D) -> Fixture3D) {
        synchronized(this) {
            val current = _fixtures.value.toMutableList()
            if (index in current.indices) {
                current[index] = update(current[index])
                saveFixtures(current)
            }
        }
    }

    /** Add or update a group. */
    fun upsertGroup(group: FixtureGroup) {
        synchronized(this) {
            val current = _groups.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == group.id }
            if (existingIndex >= 0) {
                current[existingIndex] = group
            } else {
                current.add(group)
            }
            saveGroups(current)
        }
    }

    /** Remove a group and clear it from all fixtures. */
    fun removeGroup(groupId: String) {
        synchronized(this) {
            val newGroups = _groups.value.filter { it.id != groupId }
            saveGroups(newGroups)

            val newFixtures = _fixtures.value.map {
                if (it.groupId == groupId) it.copy(groupId = null) else it
            }
            saveFixtures(newFixtures)
        }
    }
}

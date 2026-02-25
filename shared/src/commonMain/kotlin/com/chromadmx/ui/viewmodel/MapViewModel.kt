package com.chromadmx.ui.viewmodel

import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Map screen.
 *
 * Manages the list of mapped fixtures and their 3D positions.
 * In a full implementation, this would be backed by a fixture store/repository.
 */
class MapViewModel(
    private val scope: CoroutineScope,
) {
    private val _fixtures = MutableStateFlow<List<Fixture3D>>(emptyList())
    val fixtures: StateFlow<List<Fixture3D>> = _fixtures.asStateFlow()

    private val _selectedFixtureIndex = MutableStateFlow<Int?>(null)
    val selectedFixtureIndex: StateFlow<Int?> = _selectedFixtureIndex.asStateFlow()

    fun selectFixture(index: Int?) {
        _selectedFixtureIndex.value = index
    }

    fun updateFixturePosition(index: Int, newPosition: Vec3) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(position = newPosition)
            _fixtures.value = current
        }
    }

    fun addFixture(fixture: Fixture3D) {
        _fixtures.value = _fixtures.value + fixture
    }

    fun removeFixture(index: Int) {
        val current = _fixtures.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _fixtures.value = current
            if (_selectedFixtureIndex.value == index) {
                _selectedFixtureIndex.value = null
            }
        }
    }

    /** Cancel all coroutines launched by this ViewModel. */
    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
    }
}

package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/**
 * Base fixture: the smallest addressable unit on a DMX universe.
 *
 * A fixture occupies [channelCount] consecutive channels starting at
 * [channelStart] on universe [universeId].
 */
@Serializable
data class Fixture(
    val fixtureId: String,
    val name: String,
    val channelStart: Int,
    val channelCount: Int,
    val universeId: Int
)

/**
 * Fixture with a 3-D position in the venue, used by spatial effects.
 */
@Serializable
data class Fixture3D(
    val fixture: Fixture,
    val position: Vec3,
    val groupId: String? = null
)

/**
 * Raw spatial coordinate produced by the camera-mapping vision pipeline.
 */
@Serializable
data class SpatialCoordinate(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun toVec3(): Vec3 = Vec3(x, y, z)

    companion object {
        fun fromVec3(v: Vec3): SpatialCoordinate = SpatialCoordinate(v.x, v.y, v.z)
    }
}

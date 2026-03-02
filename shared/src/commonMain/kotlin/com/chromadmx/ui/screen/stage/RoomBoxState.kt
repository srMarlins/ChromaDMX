package com.chromadmx.ui.screen.stage

import com.chromadmx.core.model.Vec3

/**
 * Faces of the 3D room box (diorama).
 */
enum class BoxFace {
    BACK_WALL, FRONT_WALL, LEFT_WALL, RIGHT_WALL, CEILING, FLOOR
}

/**
 * A fixture placed on a specific face of the room box.
 *
 * @param fixtureId Identifier linking to the DMX fixture.
 * @param face Which face of the room box this fixture is mounted on.
 * @param positionOnFace Normalized (0-1) UV coordinate on the face.
 *        Only x (u) and y (v) are used; z is ignored.
 * @param lengthOnFace For strip fixtures: fraction of face width covered (0-1).
 */
data class RoomFixturePlacement(
    val fixtureId: String,
    val face: BoxFace,
    val positionOnFace: Vec3,
    val lengthOnFace: Float = 0.5f,
)

/**
 * UI state for the Room Box View — a rotatable 3D diorama room
 * where fixtures glow on the walls, ceiling, and floor.
 *
 * @param rotationY Horizontal rotation angle in degrees.
 * @param rotationX Vertical rotation angle in degrees.
 * @param zoom Zoom scale factor.
 * @param placements Fixture placements within the room.
 * @param placingFixtureId When non-null, a fixture is being placed interactively.
 * @param selectedFace The currently tapped/selected face, if any.
 */
data class RoomBoxState(
    val rotationY: Float = 30f,
    val rotationX: Float = 20f,
    val zoom: Float = 1f,
    val placements: List<RoomFixturePlacement> = emptyList(),
    val placingFixtureId: String? = null,
    val selectedFace: BoxFace? = null,
)

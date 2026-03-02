package com.chromadmx.ui.screen.stage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.chromadmx.core.model.Vec3
import com.chromadmx.ui.theme.PixelDesign
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// RoomBoxView — 3D diorama-style room where fixtures glow on box faces.
//
// Uses simple isometric projection with Canvas rendering. Drag to rotate,
// tap a face to select it. Fixture placements are drawn as glowing circles
// at their mapped world-space position projected to screen.
// ============================================================================

/**
 * Box corner indices (unit cube from -1 to +1):
 *
 * ```
 *     4--------5          Y  Z
 *    /|       /|          | /
 *   7--------6 |          |/
 *   | 0------|-1          +--- X
 *   |/       |/
 *   3--------2
 * ```
 *
 * - Bottom face (floor, z=-1): 0,1,2,3
 * - Top face (ceiling, z=+1): 4,5,6,7
 * - Back wall  (y=-1): 0,1,5,4
 * - Front wall (y=+1): 3,2,6,7
 * - Left wall  (x=-1): 0,3,7,4
 * - Right wall (x=+1): 1,2,6,5
 */
private val BOX_CORNERS = arrayOf(
    Vec3(-1f, -1f, -1f), // 0: back-left-bottom
    Vec3(1f, -1f, -1f),  // 1: back-right-bottom
    Vec3(1f, 1f, -1f),   // 2: front-right-bottom
    Vec3(-1f, 1f, -1f),  // 3: front-left-bottom
    Vec3(-1f, -1f, 1f),  // 4: back-left-top
    Vec3(1f, -1f, 1f),   // 5: back-right-top
    Vec3(1f, 1f, 1f),    // 6: front-right-top
    Vec3(-1f, 1f, 1f),   // 7: front-left-top
)

/**
 * Face definitions: each face is 4 corner indices (winding order for front-face)
 * paired with the face normal (for backface culling / depth sorting).
 */
private data class FaceDef(
    val face: BoxFace,
    val indices: IntArray,
    val normal: Vec3,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceDef) return false
        return face == other.face
    }

    override fun hashCode(): Int = face.hashCode()
}

private val FACE_DEFS = arrayOf(
    FaceDef(BoxFace.BACK_WALL, intArrayOf(0, 1, 5, 4), Vec3(0f, -1f, 0f)),
    FaceDef(BoxFace.FRONT_WALL, intArrayOf(3, 2, 6, 7), Vec3(0f, 1f, 0f)),
    FaceDef(BoxFace.LEFT_WALL, intArrayOf(0, 3, 7, 4), Vec3(-1f, 0f, 0f)),
    FaceDef(BoxFace.RIGHT_WALL, intArrayOf(1, 2, 6, 5), Vec3(1f, 0f, 0f)),
    FaceDef(BoxFace.CEILING, intArrayOf(4, 5, 6, 7), Vec3(0f, 0f, 1f)),
    FaceDef(BoxFace.FLOOR, intArrayOf(0, 1, 2, 3), Vec3(0f, 0f, -1f)),
)

/** Dark room wall color (slightly lighter than background for visibility). */
private val WALL_COLOR = Color(0xFF141428)

/** Slightly different shade for ceiling and floor to add depth. */
private val CEILING_COLOR = Color(0xFF181830)
private val FLOOR_COLOR = Color(0xFF0E0E1C)

/** Selected face highlight overlay. */
private val SELECTED_FACE_OVERLAY = Color(0x30FFFFFF)

/** Edge color for room wireframe. */
private val EDGE_COLOR = Color(0xFF2A2A4A)

/** Glow radius for fixture lights. */
private const val GLOW_RADIUS_BASE = 24f
private const val GLOW_RADIUS_OUTER = 48f

/**
 * Renders a rotatable 3D room box (diorama) with fixture glows on the walls.
 *
 * @param state The current room box state (rotation, zoom, placements).
 * @param fixtureColors Map of fixture ID to its current Compose color.
 * @param onRotate Called with (deltaX, deltaY) in degrees when the user drags.
 * @param onTapFace Called when the user taps on a visible face.
 * @param modifier Optional modifier.
 */
@Composable
fun RoomBoxView(
    state: RoomBoxState,
    fixtureColors: Map<String, Color>,
    onRotate: (deltaX: Float, deltaY: Float) -> Unit,
    onTapFace: (BoxFace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = PixelDesign.colors.stageBackground
    val selectedOverlay = PixelDesign.colors.primary.copy(alpha = 0.15f)

    // Pre-compute projected corners outside the draw lambda to share with gestures
    // (projection is cheap enough to re-derive in draw scope)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Horizontal drag -> rotationY, vertical drag -> rotationX
                    val sensitivity = 0.3f
                    onRotate(dragAmount.x * sensitivity, -dragAmount.y * sensitivity)
                }
            }
            .pointerInput(state.rotationX, state.rotationY, state.zoom) {
                detectTapGestures { tapOffset ->
                    // Simple face hit testing: project face centers and find closest
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val scale = minOf(size.width, size.height) * 0.25f * state.zoom

                    val radY = state.rotationY * (kotlin.math.PI / 180.0)
                    val radX = state.rotationX * (kotlin.math.PI / 180.0)

                    val cosY = cos(radY).toFloat()
                    val sinY = sin(radY).toFloat()
                    val cosX = cos(radX).toFloat()
                    val sinX = sin(radX).toFloat()

                    fun proj(v: Vec3): Offset {
                        val x1 = v.x * cosY - v.y * sinY
                        val z1 = v.x * sinY + v.y * cosY
                        val y1 = v.z * cosX - z1 * sinX
                        return Offset(cx + x1 * scale, cy - y1 * scale)
                    }

                    // Project all corners
                    val projected = BOX_CORNERS.map { proj(it) }

                    // Test each face polygon for containment
                    var tappedFace: BoxFace? = null
                    // Sort faces back-to-front so we pick the front-most hit
                    val sortedFaces = FACE_DEFS.sortedBy { def ->
                        // Depth of face center
                        val center = def.indices.fold(Vec3.ZERO) { acc, i ->
                            acc + BOX_CORNERS[i]
                        } * (1f / def.indices.size)
                        val x1 = center.x * cosY - center.y * sinY
                        val z1 = center.x * sinY + center.y * cosY
                        -(center.z * cosX - z1 * sinX) // negative for back-to-front
                    }

                    for (faceDef in sortedFaces) {
                        val pts = faceDef.indices.map { projected[it] }
                        if (pointInPolygon(tapOffset, pts)) {
                            tappedFace = faceDef.face
                        }
                    }
                    tappedFace?.let { onTapFace(it) }
                }
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = minOf(size.width, size.height) * 0.25f * state.zoom

        val radY = state.rotationY * (kotlin.math.PI / 180.0)
        val radX = state.rotationX * (kotlin.math.PI / 180.0)

        val cosY = cos(radY).toFloat()
        val sinY = sin(radY).toFloat()
        val cosX = cos(radX).toFloat()
        val sinX = sin(radX).toFloat()

        fun project(v: Vec3): Offset {
            val x1 = v.x * cosY - v.y * sinY
            val z1 = v.x * sinY + v.y * cosY
            val y1 = v.z * cosX - z1 * sinX
            return Offset(cx + x1 * scale, cy - y1 * scale)
        }

        // Depth of a face center for sorting (higher = closer to camera)
        fun faceDepth(def: FaceDef): Float {
            val center = def.indices.fold(Vec3.ZERO) { acc, i ->
                acc + BOX_CORNERS[i]
            } * (1f / def.indices.size)
            val x1 = center.x * cosY - center.y * sinY
            val z1 = center.x * sinY + center.y * cosY
            return center.z * cosX - z1 * sinX
        }

        // Project all corners
        val projected = BOX_CORNERS.map { project(it) }

        // Sort faces back-to-front (painter's algorithm)
        val sortedFaces = FACE_DEFS.sortedBy { faceDepth(it) }

        val reusablePath = Path()

        // Draw each face
        for (faceDef in sortedFaces) {
            val pts = faceDef.indices.map { projected[it] }

            // Choose face color
            val faceColor = when (faceDef.face) {
                BoxFace.CEILING -> CEILING_COLOR
                BoxFace.FLOOR -> FLOOR_COLOR
                else -> WALL_COLOR
            }

            // Fill face polygon
            reusablePath.reset()
            reusablePath.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                reusablePath.lineTo(pts[i].x, pts[i].y)
            }
            reusablePath.close()

            drawPath(reusablePath, color = faceColor, style = Fill)

            // Selected face highlight
            if (state.selectedFace == faceDef.face) {
                drawPath(reusablePath, color = selectedOverlay, style = Fill)
            }

            // Edge wireframe
            drawPath(reusablePath, color = EDGE_COLOR, style = Stroke(width = 1.5f))

            // Draw fixture glows on this face
            drawFixtureGlows(
                faceDef = faceDef,
                placements = state.placements,
                fixtureColors = fixtureColors,
                project = ::project,
            )
        }
    }
}

// ============================================================================
// Fixture glow rendering
// ============================================================================

/**
 * Draws glowing circles for all fixture placements on the given face.
 */
private fun DrawScope.drawFixtureGlows(
    faceDef: FaceDef,
    placements: List<RoomFixturePlacement>,
    fixtureColors: Map<String, Color>,
    project: (Vec3) -> Offset,
) {
    for (placement in placements) {
        if (placement.face != faceDef.face) continue

        val color = fixtureColors[placement.fixtureId] ?: Color.Gray
        val worldPos = placementToWorld(placement)
        val screenPos = project(worldPos)

        // Outer glow (diffuse)
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = GLOW_RADIUS_OUTER,
            center = screenPos,
            blendMode = BlendMode.Screen,
        )

        // Mid glow
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = GLOW_RADIUS_BASE,
            center = screenPos,
            blendMode = BlendMode.Screen,
        )

        // Inner bright core
        drawCircle(
            color = color.copy(alpha = 0.9f),
            radius = GLOW_RADIUS_BASE * 0.35f,
            center = screenPos,
        )

        // White-hot center
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = GLOW_RADIUS_BASE * 0.15f,
            center = screenPos,
        )
    }
}

// ============================================================================
// Coordinate mapping
// ============================================================================

/**
 * Maps a fixture placement (face + normalized UV) to a 3D world coordinate
 * on the surface of the unit box (-1 to +1 on each axis).
 *
 * The positionOnFace.x maps to the face's horizontal (u) axis,
 * and positionOnFace.y maps to the face's vertical (v) axis,
 * both remapped from [0,1] to [-1,+1].
 */
private fun placementToWorld(placement: RoomFixturePlacement): Vec3 {
    val u = placement.positionOnFace.x * 2f - 1f
    val v = placement.positionOnFace.y * 2f - 1f
    return when (placement.face) {
        BoxFace.BACK_WALL -> Vec3(u, -1f, v)
        BoxFace.FRONT_WALL -> Vec3(u, 1f, v)
        BoxFace.LEFT_WALL -> Vec3(-1f, u, v)
        BoxFace.RIGHT_WALL -> Vec3(1f, u, v)
        BoxFace.CEILING -> Vec3(u, v, 1f)
        BoxFace.FLOOR -> Vec3(u, v, -1f)
    }
}

// ============================================================================
// Hit testing
// ============================================================================

/**
 * Simple point-in-polygon test using the ray-casting algorithm.
 * Returns true if [point] is inside the polygon defined by [vertices].
 */
private fun pointInPolygon(point: Offset, vertices: List<Offset>): Boolean {
    var inside = false
    val n = vertices.size
    var j = n - 1
    for (i in 0 until n) {
        val vi = vertices[i]
        val vj = vertices[j]
        if ((vi.y > point.y) != (vj.y > point.y) &&
            point.x < (vj.x - vi.x) * (point.y - vi.y) / (vj.y - vi.y) + vi.x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

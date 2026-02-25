package com.chromadmx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureGroup
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.model.Color as DmxColor

/**
 * Canvas-based top-down venue visualization.
 *
 * Renders each fixture as a colored circle at its (x, y) position,
 * with color driven by the engine's latest computed fixture colors.
 * The canvas scales fixture positions to fit the available space.
 *
 * @param fixtures List of fixtures with 3D positions.
 * @param fixtureColors Parallel list of colors, one per fixture.
 * @param selectedIndices Set of currently selected fixture indices.
 * @param groups List of fixture groups for color coding.
 * @param onFixtureSelected Callback when a fixture is tapped.
 * @param onFixtureMoved Callback when a fixture is dragged.
 * @param onRegionSelected Callback for multi-select region.
 */
@Composable
fun VenueCanvas(
    fixtures: List<Fixture3D>,
    fixtureColors: List<DmxColor>,
    selectedIndices: Set<Int> = emptySet(),
    groups: List<FixtureGroup> = emptyList(),
    onFixtureSelected: (Int, multi: Boolean) -> Unit = { _, _ -> },
    onFixtureMoved: (Int, Vec3) -> Unit = { _, _ -> },
    onRegionSelected: (ClosedFloatingPointRange<Float>, ClosedFloatingPointRange<Float>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectionRectStart by remember { mutableStateOf<Offset?>(null) }
    var selectionRectEnd by remember { mutableStateOf<Offset?>(null) }
    var draggingFixtureIndex by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080818))
            .pointerInput(fixtures) {
                detectTapGestures { offset ->
                    val index = findFixtureAt(offset, fixtures, size.toSize())
                    if (index != -1) {
                        onFixtureSelected(index, false)
                    } else {
                        onFixtureSelected(-1, false) // Clear selection
                    }
                }
            }
            .pointerInput(fixtures) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = findFixtureAt(offset, fixtures, size.toSize())
                        if (index != -1) {
                            draggingFixtureIndex = index
                            onFixtureSelected(index, false)
                        } else {
                            selectionRectStart = offset
                            selectionRectEnd = offset
                        }
                    },
                    onDragEnd = {
                        if (selectionRectStart != null && selectionRectEnd != null) {
                            // Compute region in fixture coordinates
                            val (x1, y1) = canvasToFixture(selectionRectStart!!, size.toSize(), fixtures)
                            val (x2, y2) = canvasToFixture(selectionRectEnd!!, size.toSize(), fixtures)
                            onRegionSelected(
                                minOf(x1, x2)..maxOf(x1, x2),
                                minOf(y1, y2)..maxOf(y1, y2)
                            )
                        }
                        selectionRectStart = null
                        selectionRectEnd = null
                        draggingFixtureIndex = null
                    },
                    onDrag = { change, dragAmount ->
                        if (draggingFixtureIndex != null) {
                            val newOffset = change.position
                            val fixturePos = canvasToFixture(newOffset, size.toSize(), fixtures)
                            // Grid snap (0.25m)
                            val snappedX = (fixturePos.x / 0.25f).roundToInt() * 0.25f
                            val snappedY = (fixturePos.y / 0.25f).roundToInt() * 0.25f
                            val currentZ = fixtures[draggingFixtureIndex!!].position.z
                            onFixtureMoved(draggingFixtureIndex!!, Vec3(snappedX, snappedY, currentZ))
                        } else {
                            selectionRectEnd = change.position
                        }
                    }
                )
            },
    ) {
        if (fixtures.isEmpty()) return@Canvas

        drawGrid()

        val bounds = computeBounds(fixtures)
        val padding = 40f
        val canvasW = size.width - 2 * padding
        val canvasH = size.height - 2 * padding

        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        for ((index, fixture) in fixtures.withIndex()) {
            val cx = padding + (fixture.position.x - bounds.minX) / bounds.rangeX * canvasW
            val cy = padding + (1f - (fixture.position.y - bounds.minY) / bounds.rangeY) * canvasH

            val isSelected = selectedIndices.contains(index)
            val dmxColor = fixtureColors.getOrNull(index) ?: DmxColor.BLACK
            val composeColor = dmxColor.toComposeColor()

            val group = groups.find { it.id == fixture.groupId }
            val groupColor = group?.color?.toComposeColor() ?: Color.Gray

            // Group ring
            drawCircle(
                color = groupColor.copy(alpha = 0.4f),
                radius = 24f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            )

            // Selection highlight
            if (isSelected) {
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.5f),
                    radius = 28f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
            }

            // Outer glow
            drawCircle(
                color = composeColor.copy(alpha = 0.3f),
                radius = 18f,
                center = Offset(cx, cy),
            )

            // Inner dot
            drawCircle(
                color = composeColor,
                radius = 10f,
                center = Offset(cx, cy),
            )
        }

        // Draw selection rectangle
        if (selectionRectStart != null && selectionRectEnd != null) {
            drawRect(
                color = Color.Cyan.copy(alpha = 0.2f),
                topLeft = Offset(
                    minOf(selectionRectStart!!.x, selectionRectEnd!!.x),
                    minOf(selectionRectStart!!.y, selectionRectEnd!!.y)
                ),
                size = Size(
                    kotlin.math.abs(selectionRectEnd!!.x - selectionRectStart!!.x),
                    kotlin.math.abs(selectionRectEnd!!.y - selectionRectStart!!.y)
                )
            )
            drawRect(
                color = Color.Cyan,
                topLeft = Offset(
                    minOf(selectionRectStart!!.x, selectionRectEnd!!.x),
                    minOf(selectionRectStart!!.y, selectionRectEnd!!.y)
                ),
                size = Size(
                    kotlin.math.abs(selectionRectEnd!!.x - selectionRectStart!!.x),
                    kotlin.math.abs(selectionRectEnd!!.y - selectionRectStart!!.y)
                ),
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawGrid() {
    val gridColor = Color(0xFF1A1A2E)
    val step = 50f
    for (x in 0..(size.width / step).toInt()) {
        drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, size.height))
    }
    for (y in 0..(size.height / step).toInt()) {
        drawLine(gridColor, Offset(0f, y * step), Offset(size.width, y * step))
    }
}

private data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    val rangeX = (maxX - minX).coerceAtLeast(1f)
    val rangeY = (maxY - minY).coerceAtLeast(1f)
}

private fun computeBounds(fixtures: List<Fixture3D>): Bounds {
    if (fixtures.isEmpty()) return Bounds(-5f, 5f, -5f, 5f)
    var minX = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var minY = Float.MAX_VALUE
    var maxY = Float.MIN_VALUE
    for (f in fixtures) {
        if (f.position.x < minX) minX = f.position.x
        if (f.position.x > maxX) maxX = f.position.x
        if (f.position.y < minY) minY = f.position.y
        if (f.position.y > maxY) maxY = f.position.y
    }
    // Add some buffer
    return Bounds(minX - 1f, maxX + 1f, minY - 1f, maxY + 1f)
}

private fun findFixtureAt(offset: Offset, fixtures: List<Fixture3D>, size: Size): Int {
    val bounds = computeBounds(fixtures)
    val padding = 40f
    val canvasW = size.width - 2 * padding
    val canvasH = size.height - 2 * padding

    for ((index, fixture) in fixtures.withIndex()) {
        val cx = padding + (fixture.position.x - bounds.minX) / bounds.rangeX * canvasW
        val cy = padding + (1f - (fixture.position.y - bounds.minY) / bounds.rangeY) * canvasH
        val distance = (Offset(cx, cy) - offset).getDistance()
        if (distance < 30f) return index
    }
    return -1
}

private fun canvasToFixture(offset: Offset, size: Size, fixtures: List<Fixture3D>): Offset {
    val bounds = computeBounds(fixtures)
    val padding = 40f
    val canvasW = size.width - 2 * padding
    val canvasH = size.height - 2 * padding

    val normX = (offset.x - padding) / canvasW
    val normY = 1f - (offset.y - padding) / canvasH

    val fx = bounds.minX + normX * bounds.rangeX
    val fy = bounds.minY + normY * bounds.rangeY

    return Offset(fx, fy)
}

private fun Float.roundToInt() = kotlin.math.round(this).toInt()

/**
 * Convert a DMX [DmxColor] (0-1 floats) to a Compose [Color].
 */
private fun DmxColor.toComposeColor(): Color {
    return Color(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = 1f,
    )
}

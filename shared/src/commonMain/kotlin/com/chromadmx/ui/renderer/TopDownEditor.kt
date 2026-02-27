package com.chromadmx.ui.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureType
import com.chromadmx.core.model.RenderHint
import com.chromadmx.core.model.Vec3
import com.chromadmx.ui.renderer.TopDownRenderer.drawBarFixture
import com.chromadmx.ui.renderer.TopDownRenderer.drawBeamConeFixture
import com.chromadmx.ui.renderer.TopDownRenderer.drawParFixture
import com.chromadmx.ui.renderer.TopDownRenderer.drawStrobeFixture
import com.chromadmx.ui.renderer.TopDownRenderer.drawWashFixture
import com.chromadmx.core.model.Color as DmxColor
import com.chromadmx.ui.components.toComposeColor

/** Scanline color for the subtle CRT-like horizontal lines. */
private val ScanlineColor = Color.White.copy(alpha = 0.015f)

/** Grid line color for the LED matrix background. */
private val GridLineColor = Color.White.copy(alpha = 0.04f)

/** Background color for the canvas. */
private val CanvasBackground = Color(0xFF060612)

/** Highlight color for selected fixture border. */
private val SelectionColor = Color(0xFF00FBFF)

/** Highlight color for edit-mode drag crosshair. */
private val EditDragColor = Color(0xFFFFFF00)


/**
 * Canvas-based top-down fixture editor with profile-aware rendering.
 *
 * A reusable composable for displaying and editing fixture positions on a
 * 2D grid. Shared between Setup (fixture configuration) and Stage (edit mode).
 *
 * Renders each fixture based on its [com.chromadmx.core.model.FixtureProfile.renderHint]:
 * - **POINT** (pars, washes, strobes): Colored circle with glow halo.
 * - **BAR** (pixel bars): Horizontal row of colored segments.
 * - **BEAM_CONE** (moving heads): Circle with directional indicator line.
 *
 * Supports pinch-to-zoom (0.5x-4x), two-finger pan, tap-to-select, and
 * drag-to-move with 0.5-unit grid snapping when [isEditable] is true.
 * Draws a subtle LED matrix grid background with scanline aesthetic.
 *
 * @param fixtures List of fixtures with 3D positions.
 * @param fixtureColors Parallel list of colors, one per fixture.
 *        If shorter than [fixtures], missing fixtures render as dim gray.
 * @param modifier Optional [Modifier] applied to the canvas.
 * @param selectedFixtureIndex Index of the currently selected fixture, or null.
 * @param isEditable Whether drag-to-move is enabled. When false, only
 *        pinch-to-zoom, pan, and tap-to-select are active.
 * @param onFixtureMoved Callback when a fixture is dragged to a new position;
 *        receives the fixture index and the snapped world-space position.
 * @param onFixtureTapped Callback when a fixture is tapped; receives the fixture index.
 * @param onBackgroundTapped Callback when the canvas background is tapped (deselects).
 * @param onDragEnd Callback when a drag gesture ends; receives the fixture index.
 */
@Composable
fun TopDownEditor(
    fixtures: List<Fixture3D>,
    fixtureColors: List<DmxColor>,
    modifier: Modifier = Modifier,
    selectedFixtureIndex: Int? = null,
    isEditable: Boolean = true,
    onFixtureMoved: ((fixtureIndex: Int, newPosition: Vec3) -> Unit)? = null,
    onFixtureTapped: ((fixtureIndex: Int) -> Unit)? = null,
    onBackgroundTapped: (() -> Unit)? = null,
    onDragEnd: ((fixtureIndex: Int) -> Unit)? = null,
) {
    // Zoom and pan state
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Pre-compute fixture positions for hit testing in tap gesture
    var fixtureScreenPositions by remember { mutableStateOf(emptyList<Offset>()) }

    // Edit mode drag tracking
    var dragTargetIndex by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .pointerInput(isEditable) {
                if (!isEditable) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.5f, 4f)
                        panOffset += pan
                    }
                }
            }
            .then(
                if (isEditable) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(0.5f, 4f)
                                panOffset += pan
                            }
                        }
                        .pointerInput(fixtures.size, zoom, panOffset, fixtureScreenPositions) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    // Find nearest fixture within touch radius
                                    val touchRadius = 40f * zoom
                                    val touchRadiusSq = touchRadius * touchRadius
                                    var closestIndex = -1
                                    var closestDistSq = Float.MAX_VALUE
                                    for ((i, pos) in fixtureScreenPositions.withIndex()) {
                                        val dx = startOffset.x - pos.x
                                        val dy = startOffset.y - pos.y
                                        val distSq = dx * dx + dy * dy
                                        if (distSq < touchRadiusSq && distSq < closestDistSq) {
                                            closestDistSq = distSq
                                            closestIndex = i
                                        }
                                    }
                                    dragTargetIndex = closestIndex
                                    if (closestIndex >= 0) {
                                        onFixtureTapped?.invoke(closestIndex)
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (dragTargetIndex >= 0 && dragTargetIndex < fixtures.size) {
                                        // Convert screen position back to world coordinates
                                        val screenPos = change.position
                                        val pivotX = size.width / 2f
                                        val pivotY = size.height / 2f

                                        // Undo pan and zoom transforms
                                        val canvasX =
                                            (screenPos.x - panOffset.x - pivotX) / zoom + pivotX
                                        val canvasY =
                                            (screenPos.y - panOffset.y - pivotY) / zoom + pivotY

                                        val padding = 48f
                                        val canvasW = size.width - 2 * padding
                                        val canvasH = size.height - 2 * padding

                                        if (canvasW > 0f && canvasH > 0f) {
                                            // Compute bounds from fixtures
                                            var minX = Float.MAX_VALUE
                                            var maxX = -Float.MAX_VALUE
                                            var minY = Float.MAX_VALUE
                                            var maxY = -Float.MAX_VALUE
                                            for (f in fixtures) {
                                                if (f.position.x < minX) minX = f.position.x
                                                if (f.position.x > maxX) maxX = f.position.x
                                                if (f.position.y < minY) minY = f.position.y
                                                if (f.position.y > maxY) maxY = f.position.y
                                            }
                                            val rangeX = (maxX - minX).coerceAtLeast(1f)
                                            val rangeY = (maxY - minY).coerceAtLeast(1f)

                                            // Reverse the normalization
                                            val normX =
                                                ((canvasX - padding) / canvasW).coerceIn(0f, 1f)
                                            val normY =
                                                (1f - (canvasY - padding) / canvasH).coerceIn(
                                                    0f,
                                                    1f,
                                                )
                                            val worldX = minX + normX * rangeX
                                            val worldY = minY + normY * rangeY

                                            // Grid snap to 0.5 units
                                            val snappedX =
                                                (worldX * 2f).let { kotlin.math.round(it) } / 2f
                                            val snappedY =
                                                (worldY * 2f).let { kotlin.math.round(it) } / 2f

                                            val currentZ = fixtures[dragTargetIndex].position.z
                                            onFixtureMoved?.invoke(
                                                dragTargetIndex,
                                                Vec3(snappedX, snappedY, currentZ),
                                            )
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (dragTargetIndex >= 0) onDragEnd?.invoke(dragTargetIndex)
                                    dragTargetIndex = -1
                                },
                                onDragCancel = { dragTargetIndex = -1 },
                            )
                        }
                } else {
                    Modifier
                },
            )
            .pointerInput(fixtures.size, zoom, panOffset) {
                detectTapGestures { tapOffset ->
                    // Hit test: find the closest fixture within touch radius
                    // Compare squared distances to avoid sqrt overhead
                    val touchRadius = 40f * zoom
                    val touchRadiusSq = touchRadius * touchRadius
                    var closestIndex = -1
                    var closestDistSq = Float.MAX_VALUE
                    for ((i, pos) in fixtureScreenPositions.withIndex()) {
                        val dx = tapOffset.x - pos.x
                        val dy = tapOffset.y - pos.y
                        val distSq = dx * dx + dy * dy
                        if (distSq < touchRadiusSq && distSq < closestDistSq) {
                            closestDistSq = distSq
                            closestIndex = i
                        }
                    }
                    if (closestIndex >= 0) {
                        onFixtureTapped?.invoke(closestIndex)
                    } else {
                        onBackgroundTapped?.invoke()
                    }
                }
            },
    ) {
        val gridSpacing = 24f
        drawLedMatrixGrid(gridSpacing)
        drawScanlines()

        if (fixtures.isEmpty()) return@Canvas

        val padding = 48f
        val canvasW = size.width - 2 * padding
        val canvasH = size.height - 2 * padding

        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        // Compute bounds of all fixture positions
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (f in fixtures) {
            if (f.position.x < minX) minX = f.position.x
            if (f.position.x > maxX) maxX = f.position.x
            if (f.position.y < minY) minY = f.position.y
            if (f.position.y > maxY) maxY = f.position.y
        }
        val rawRangeX = maxX - minX
        val rawRangeY = maxY - minY
        val collapsedX = rawRangeX < 0.1f
        val collapsedY = rawRangeY < 0.1f
        val rangeX = if (collapsedX) 1f else rawRangeX
        val rangeY = if (collapsedY) 1f else rawRangeY

        // Scale fixture rendering sizes based on available space per fixture
        val fixtureScale = ((canvasW / fixtures.size.coerceAtLeast(1).toFloat()) / 40f)
            .coerceIn(0.8f, 3f)

        // Reusable Path to avoid allocations inside the fixture loop
        val reusablePath = Path()

        // Apply zoom and pan transform
        withTransform({
            translate(panOffset.x, panOffset.y)
            scale(zoom, zoom, Offset(size.width / 2f, size.height / 2f))
        }) {
            val positions = mutableListOf<Offset>()

            for ((index, fixture) in fixtures.withIndex()) {
                val normX = if (collapsedX) 0.5f else (fixture.position.x - minX) / rangeX
                val normY = if (collapsedY) 0.5f else (fixture.position.y - minY) / rangeY
                val cx = padding + normX * canvasW
                val cy = padding + (1f - normY) * canvasH // Flip Y for top-down

                positions.add(Offset(cx, cy))

                val dmxColor = fixtureColors.getOrNull(index) ?: DmxColor.BLACK
                val composeColor = dmxColor.toComposeColor()
                val isSelected = selectedFixtureIndex == index

                val profile = BuiltInProfiles.findById(fixture.fixture.profileId)
                val renderHint = profile?.renderHint ?: RenderHint.POINT

                when (renderHint) {
                    RenderHint.POINT -> {
                        val fixtureType = profile?.type ?: FixtureType.PAR
                        when (fixtureType) {
                            FixtureType.STROBE -> drawStrobeFixture(cx, cy, composeColor, isSelected, SelectionColor, fixtureScale)
                            FixtureType.WASH -> drawWashFixture(cx, cy, composeColor, isSelected, SelectionColor, fixtureScale)
                            else -> drawParFixture(cx, cy, composeColor, isSelected, SelectionColor, fixtureScale)
                        }
                    }
                    RenderHint.BAR -> {
                        val pixelCount = profile?.physical?.pixelCount ?: 8
                        drawBarFixture(cx, cy, composeColor, pixelCount, isSelected, SelectionColor, fixtureScale)
                    }
                    RenderHint.BEAM_CONE -> drawBeamConeFixture(
                        cx, cy, composeColor, isSelected, reusablePath, SelectionColor, fixtureScale,
                    )
                }
            }

            // Draw edit-mode drag handle on the drag target or selected fixture
            if (isEditable) {
                val editIndex =
                    if (dragTargetIndex >= 0) dragTargetIndex else selectedFixtureIndex
                if (editIndex != null && editIndex in positions.indices) {
                    val pos = positions[editIndex]
                    drawEditDragHandle(pos.x, pos.y)
                }
            }

            // Update screen positions for hit testing (transform-adjusted)
            fixtureScreenPositions = positions.map { pos ->
                val pivotX = size.width / 2f
                val pivotY = size.height / 2f
                Offset(
                    (pos.x - pivotX) * zoom + pivotX + panOffset.x,
                    (pos.y - pivotY) * zoom + pivotY + panOffset.y,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Drawing helpers
// ---------------------------------------------------------------------------

/**
 * Draw subtle LED matrix grid lines on the canvas background.
 */
private fun DrawScope.drawLedMatrixGrid(spacing: Float) {
    var x = 0f
    while (x < size.width) {
        drawLine(GridLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += spacing
    }
    var y = 0f
    while (y < size.height) {
        drawLine(GridLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += spacing
    }
}

/**
 * Draw horizontal scanlines for CRT aesthetic.
 */
private fun DrawScope.drawScanlines() {
    var y = 0f
    while (y < size.height) {
        drawLine(ScanlineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += 3f
    }
}

/**
 * Draw a crosshair drag handle for edit mode on the selected fixture.
 */
private fun DrawScope.drawEditDragHandle(cx: Float, cy: Float) {
    val armLen = 28f
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), phase = 0f)

    // Horizontal crosshair
    drawLine(
        color = EditDragColor,
        start = Offset(cx - armLen, cy),
        end = Offset(cx + armLen, cy),
        strokeWidth = 2f,
        pathEffect = dashEffect,
    )
    // Vertical crosshair
    drawLine(
        color = EditDragColor,
        start = Offset(cx, cy - armLen),
        end = Offset(cx, cy + armLen),
        strokeWidth = 2f,
        pathEffect = dashEffect,
    )

    // Dashed circle border
    drawCircle(
        color = EditDragColor,
        radius = 24f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f, pathEffect = dashEffect),
    )
}

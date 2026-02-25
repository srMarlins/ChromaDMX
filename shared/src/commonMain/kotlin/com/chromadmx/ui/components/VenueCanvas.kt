package com.chromadmx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.RenderHint
import com.chromadmx.core.model.Color as DmxColor

/** Scanline color for the subtle CRT-like horizontal lines. */
private val ScanlineColor = Color.White.copy(alpha = 0.015f)

/** Grid line color for the LED matrix background. */
private val GridLineColor = Color.White.copy(alpha = 0.04f)

/** Background color for the canvas. */
private val CanvasBackground = Color(0xFF060612)

/** Highlight color for selected fixture border. */
private val SelectionColor = Color(0xFF00FBFF)

/**
 * Canvas-based top-down venue visualization with profile-aware rendering.
 *
 * Renders each fixture based on its [FixtureProfile.renderHint]:
 * - **POINT** (pars, washes, strobes): Colored circle with glow halo.
 * - **BAR** (pixel bars): Horizontal row of colored segments.
 * - **BEAM_CONE** (moving heads): Circle with directional indicator line.
 *
 * Supports pinch-to-zoom, pan gesture, and tap-to-select. Draws a subtle
 * LED matrix grid background with scanline aesthetic.
 *
 * @param fixtures List of fixtures with 3D positions.
 * @param fixtureColors Parallel list of colors, one per fixture.
 *        If shorter than [fixtures], missing fixtures render as dim gray.
 * @param selectedFixtureIndex Index of the currently selected fixture, or null.
 * @param onFixtureTapped Callback when a fixture is tapped; receives the fixture index.
 * @param onBackgroundTapped Callback when the canvas background is tapped (deselects).
 */
@Composable
fun VenueCanvas(
    fixtures: List<Fixture3D>,
    fixtureColors: List<DmxColor>,
    modifier: Modifier = Modifier,
    selectedFixtureIndex: Int? = null,
    onFixtureTapped: ((Int) -> Unit)? = null,
    onBackgroundTapped: (() -> Unit)? = null,
) {
    // Zoom and pan state
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Pre-compute fixture positions for hit testing in tap gesture
    var fixtureScreenPositions by remember { mutableStateOf(emptyList<Offset>()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(0.5f, 4f)
                    panOffset += pan
                }
            }
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
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        for (f in fixtures) {
            if (f.position.x < minX) minX = f.position.x
            if (f.position.x > maxX) maxX = f.position.x
            if (f.position.y < minY) minY = f.position.y
            if (f.position.y > maxY) maxY = f.position.y
        }
        val rangeX = (maxX - minX).coerceAtLeast(1f)
        val rangeY = (maxY - minY).coerceAtLeast(1f)

        // Reusable Path to avoid allocations inside the fixture loop
        val reusablePath = Path()

        // Apply zoom and pan transform
        withTransform({
            translate(panOffset.x, panOffset.y)
            scale(zoom, zoom, Offset(size.width / 2f, size.height / 2f))
        }) {
            val positions = mutableListOf<Offset>()

            for ((index, fixture) in fixtures.withIndex()) {
                val normX = (fixture.position.x - minX) / rangeX
                val normY = (fixture.position.y - minY) / rangeY
                val cx = padding + normX * canvasW
                val cy = padding + (1f - normY) * canvasH // Flip Y for top-down

                positions.add(Offset(cx, cy))

                val dmxColor = fixtureColors.getOrNull(index) ?: DmxColor.BLACK
                val composeColor = dmxColor.toComposeColor()
                val isSelected = selectedFixtureIndex == index

                val profile = BuiltInProfiles.findById(fixture.fixture.profileId)
                val renderHint = profile?.renderHint ?: RenderHint.POINT

                when (renderHint) {
                    RenderHint.POINT -> drawPointFixture(cx, cy, composeColor, isSelected)
                    RenderHint.BAR -> {
                        val pixelCount = profile?.physical?.pixelCount ?: 8
                        drawBarFixture(cx, cy, composeColor, pixelCount, isSelected)
                    }
                    RenderHint.BEAM_CONE -> drawBeamConeFixture(cx, cy, composeColor, isSelected, reusablePath)
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
// Drawing helpers — top-level private functions to avoid DrawScope allocations
// ---------------------------------------------------------------------------

/**
 * Draw a POINT fixture (Par, Wash, Strobe): outer glow + inner filled circle.
 */
private fun DrawScope.drawPointFixture(
    cx: Float,
    cy: Float,
    color: Color,
    isSelected: Boolean,
) {
    // Outer glow halo
    drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = 22f,
        center = Offset(cx, cy),
    )
    // Mid glow
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = 14f,
        center = Offset(cx, cy),
    )
    // Inner bright core
    drawCircle(
        color = color,
        radius = 10f,
        center = Offset(cx, cy),
    )

    if (isSelected) {
        drawSelectionBorder(cx, cy, 16f)
    }
}

/**
 * Draw a BAR fixture (Pixel Bar): horizontal row of colored segments.
 */
private fun DrawScope.drawBarFixture(
    cx: Float,
    cy: Float,
    color: Color,
    pixelCount: Int,
    isSelected: Boolean,
) {
    val segmentW = 8f
    val segmentH = 12f
    val gap = 2f
    val totalW = pixelCount * segmentW + (pixelCount - 1) * gap
    val startX = cx - totalW / 2f
    val startY = cy - segmentH / 2f

    // Bar housing background
    drawRect(
        color = Color(0xFF1A1A2E),
        topLeft = Offset(startX - 3f, startY - 3f),
        size = Size(totalW + 6f, segmentH + 6f),
    )

    // Individual pixel segments — all the same color since we have one color per fixture
    for (i in 0 until pixelCount) {
        val segX = startX + i * (segmentW + gap)
        // Slight brightness variation per segment for visual interest
        val brightness = 0.8f + 0.2f * ((i % 3).toFloat() / 2f)
        drawRect(
            color = color.copy(alpha = brightness),
            topLeft = Offset(segX, startY),
            size = Size(segmentW, segmentH),
        )
    }

    // Glow beneath the bar
    drawRect(
        color = color.copy(alpha = 0.15f),
        topLeft = Offset(startX - 4f, cy + segmentH / 2f + 2f),
        size = Size(totalW + 8f, 8f),
    )

    if (isSelected) {
        val selRadius = (totalW / 2f).coerceAtLeast(16f)
        drawSelectionBorder(cx, cy, selRadius)
    }
}

/**
 * Draw a BEAM_CONE fixture (Moving Head): circle with a directional indicator line.
 */
private fun DrawScope.drawBeamConeFixture(
    cx: Float,
    cy: Float,
    color: Color,
    isSelected: Boolean,
    reusablePath: Path,
) {
    // Beam cone (downward triangle-like glow)
    val beamLength = 30f
    val beamHalfWidth = 12f
    reusablePath.reset()
    reusablePath.moveTo(cx, cy)
    reusablePath.lineTo(cx - beamHalfWidth, cy + beamLength)
    reusablePath.lineTo(cx + beamHalfWidth, cy + beamLength)
    reusablePath.close()
    drawPath(
        path = reusablePath,
        color = color.copy(alpha = 0.2f),
    )

    // Outer ring (fixture body)
    drawCircle(
        color = Color(0xFF2A2A3E),
        radius = 12f,
        center = Offset(cx, cy),
    )
    // Inner color
    drawCircle(
        color = color,
        radius = 8f,
        center = Offset(cx, cy),
    )
    // Directional indicator (short line pointing down = default tilt direction)
    drawLine(
        color = color.copy(alpha = 0.7f),
        start = Offset(cx, cy + 8f),
        end = Offset(cx, cy + 18f),
        strokeWidth = 2f,
    )

    if (isSelected) {
        drawSelectionBorder(cx, cy, 16f)
    }
}

/**
 * Draw a pixelated selection border around a fixture.
 */
private fun DrawScope.drawSelectionBorder(cx: Float, cy: Float, radius: Float) {
    val r = radius + 4f
    val pixel = 3f

    // Top edge
    drawRect(SelectionColor, Offset(cx - r, cy - r), Size(2 * r, pixel))
    // Bottom edge
    drawRect(SelectionColor, Offset(cx - r, cy + r - pixel), Size(2 * r, pixel))
    // Left edge
    drawRect(SelectionColor, Offset(cx - r, cy - r), Size(pixel, 2 * r))
    // Right edge
    drawRect(SelectionColor, Offset(cx + r - pixel, cy - r), Size(pixel, 2 * r))
}

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
 * Convert a DMX [DmxColor] (0-1 floats) to a Compose [Color].
 */
internal fun DmxColor.toComposeColor(): Color {
    return Color(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = 1f,
    )
}

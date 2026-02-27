package com.chromadmx.ui.renderer

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
import com.chromadmx.core.model.FixtureType
import com.chromadmx.core.model.RenderHint
import com.chromadmx.ui.renderer.BeamRenderer.drawBeamCone
import com.chromadmx.ui.renderer.BeamRenderer.drawFloorGlow
import com.chromadmx.ui.renderer.FixtureRenderer.drawMovingHead
import com.chromadmx.ui.renderer.FixtureRenderer.drawPar
import com.chromadmx.ui.renderer.FixtureRenderer.drawPixelBar
import com.chromadmx.ui.renderer.FixtureRenderer.drawSelection
import com.chromadmx.ui.renderer.FixtureRenderer.drawStrobe
import com.chromadmx.ui.renderer.FixtureRenderer.drawWash
import com.chromadmx.ui.state.FixtureState
import com.chromadmx.ui.state.IsoAngle
import com.chromadmx.ui.state.ViewState
/** Background color matching VenueCanvas dark aesthetic. */
private val CanvasBackground = Color(0xFF060612)

/** Scanline color for subtle CRT-like horizontal lines. */
private val ScanlineColor = Color.White.copy(alpha = 0.015f)

/** Grid line color for the LED matrix background. */
private val GridLineColor = Color.White.copy(alpha = 0.04f)

/**
 * Isometric stage renderer composable.
 *
 * Renders fixtures in an isometric 3D projection with depth-sorted drawing
 * (painter's algorithm), profile-aware fixture visuals, beam cones for moving
 * heads, and gesture support (pinch-to-zoom, pan, tap-to-select).
 *
 * Visual style matches VenueCanvas: dark background, LED matrix grid, CRT
 * scanlines, glow halos, and pixel-art fixture blocks.
 *
 * @param fixtureState Current fixture rig state (fixtures, colors, selection).
 * @param viewState View configuration including isometric angle.
 * @param fixtureColors Compose colors for each fixture (parallel to fixtureState.fixtures).
 * @param onFixtureTapped Callback when a fixture is tapped; receives the fixture index.
 * @param onBackgroundTapped Callback when the canvas background is tapped (deselects).
 */
@Composable
fun IsometricRenderer(
    fixtureState: FixtureState,
    viewState: ViewState,
    fixtureColors: List<Color>,
    modifier: Modifier = Modifier,
    onFixtureTapped: ((Int) -> Unit)? = null,
    onBackgroundTapped: (() -> Unit)? = null,
) {
    val fixtures = fixtureState.fixtures
    val selectedFixtureIndex = fixtureState.selectedFixtureIndex
    val isoAngle = viewState.isoAngle

    // Zoom and pan state
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Track screen positions for hit testing
    var fixtureScreenPositions by remember { mutableStateOf(emptyList<Pair<Int, Offset>>()) }

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
                    val touchRadius = 40f * zoom
                    val touchRadiusSq = touchRadius * touchRadius
                    var closestIndex = -1
                    var closestDistSq = Float.MAX_VALUE
                    for ((originalIndex, pos) in fixtureScreenPositions) {
                        val dx = tapOffset.x - pos.x
                        val dy = tapOffset.y - pos.y
                        val distSq = dx * dx + dy * dy
                        if (distSq < touchRadiusSq && distSq < closestDistSq) {
                            closestDistSq = distSq
                            closestIndex = originalIndex
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

        // Project all fixture positions to isometric screen coords
        val isoPositions = fixtures.map { f -> IsoMath.worldToIso(f.position, isoAngle) }

        // Compute bounds of projected positions for centering and scaling
        var minIsoX = Float.MAX_VALUE; var maxIsoX = -Float.MAX_VALUE
        var minIsoY = Float.MAX_VALUE; var maxIsoY = -Float.MAX_VALUE
        for (pos in isoPositions) {
            if (pos.x < minIsoX) minIsoX = pos.x
            if (pos.x > maxIsoX) maxIsoX = pos.x
            if (pos.y < minIsoY) minIsoY = pos.y
            if (pos.y > maxIsoY) maxIsoY = pos.y
        }
        val rangeIsoX = (maxIsoX - minIsoX).coerceAtLeast(1f)
        val rangeIsoY = (maxIsoY - minIsoY).coerceAtLeast(1f)

        // Scale to fit canvas while maintaining aspect ratio
        val scaleX = canvasW / rangeIsoX
        val scaleY = canvasH / rangeIsoY
        val fitScale = minOf(scaleX, scaleY)

        // Center offset
        val fittedW = rangeIsoX * fitScale
        val fittedH = rangeIsoY * fitScale
        val offsetX = padding + (canvasW - fittedW) / 2f
        val offsetY = padding + (canvasH - fittedH) / 2f

        // Sort fixtures by depth (back to front)
        val sortedFixtures = IsoMath.sortFixturesByDepth(fixtures, isoAngle)
        val reusablePath = Path()

        // Apply zoom and pan transform
        withTransform({
            translate(panOffset.x, panOffset.y)
            scale(zoom, zoom, Offset(size.width / 2f, size.height / 2f))
        }) {
            val screenPositions = mutableListOf<Pair<Int, Offset>>()

            for ((originalIndex, fixture) in sortedFixtures) {
                val isoPos = isoPositions[originalIndex]
                // Map isometric coords to canvas coords
                val cx = offsetX + (isoPos.x - minIsoX) * fitScale
                val cy = offsetY + (isoPos.y - minIsoY) * fitScale
                val screenPos = Offset(cx, cy)

                screenPositions.add(originalIndex to screenPos)

                // Use fixtureColors parameter (always populated), not fixtureState.fixtureColors (may be empty)
                val displayColor = fixtureColors.getOrNull(originalIndex) ?: Color.DarkGray
                val isSelected = selectedFixtureIndex == originalIndex

                val profile = BuiltInProfiles.findById(fixture.fixture.profileId)
                val renderHint = profile?.renderHint ?: RenderHint.POINT

                when (renderHint) {
                    RenderHint.POINT -> {
                        val fixtureType = profile?.type ?: FixtureType.PAR
                        when (fixtureType) {
                            FixtureType.STROBE -> drawStrobe(screenPos, displayColor)
                            FixtureType.WASH -> drawWash(screenPos, displayColor)
                            else -> drawPar(screenPos, displayColor)
                        }
                    }
                    RenderHint.BAR -> {
                        val pixelCount = profile?.physical?.pixelCount ?: 8
                        drawPixelBar(screenPos, segments = pixelCount, baseColor = displayColor)
                    }
                    RenderHint.BEAM_CONE -> {
                        drawMovingHead(screenPos, displayColor)
                        // Draw beam cone to floor (z=0 projection)
                        val floorIso = IsoMath.worldToIso(
                            fixture.position.x,
                            fixture.position.y,
                            0f,
                            isoAngle,
                        )
                        val floorCx = offsetX + (floorIso.x - minIsoX) * fitScale
                        val floorCy = offsetY + (floorIso.y - minIsoY) * fitScale
                        val floorPos = Offset(floorCx, floorCy)
                        drawBeamCone(screenPos, floorPos, displayColor, reusablePath)
                        drawFloorGlow(floorPos, displayColor)
                    }
                }

                if (isSelected) {
                    drawSelection(screenPos)
                }
            }

            // Update screen positions for hit testing (transform-adjusted)
            fixtureScreenPositions = screenPositions.map { (idx, pos) ->
                val pivotX = size.width / 2f
                val pivotY = size.height / 2f
                idx to Offset(
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

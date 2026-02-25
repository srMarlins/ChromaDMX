package com.chromadmx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.RenderHint
import com.chromadmx.core.model.Color as DmxColor

/** Dark stage background for audience view. */
private val StageBackground = Color(0xFF060612)

/** Truss bar color (structural element). */
private val TrussColor = Color(0xFF2A2A3E)

/** Floor color beneath the stage. */
private val FloorColor = Color(0xFF0A0A14)

/**
 * Front-facing audience perspective of the stage.
 *
 * The audience is at the bottom looking up at the stage.
 * Fixtures are arranged as:
 * - **Trusses**: Horizontal bars across the top for BAR-type fixtures.
 * - **Moving Heads**: Circles with downward beam cones.
 * - **Pars / Washes**: Floating colored dots on the truss.
 * - **Floor reflection**: Subtle color wash below fixtures.
 *
 * @param fixtures List of fixtures with 3D positions.
 * @param fixtureColors Parallel list of colors, one per fixture.
 */
@Composable
fun AudienceView(
    fixtures: List<Fixture3D>,
    fixtureColors: List<DmxColor>,
    modifier: Modifier = Modifier,
    onBackgroundTapped: () -> Unit = {},
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(StageBackground)
            .pointerInput(Unit) {
                detectTapGestures { onBackgroundTapped() }
            },
    ) {
        val stageTop = size.height * 0.08f
        val stageBottom = size.height * 0.55f
        val floorTop = stageBottom + 10f
        val floorBottom = size.height

        // Draw the floor area
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(FloorColor, StageBackground),
                startY = floorTop,
                endY = floorBottom,
            ),
            topLeft = Offset(0f, floorTop),
            size = Size(size.width, floorBottom - floorTop),
        )

        // Horizontal scanlines on floor for depth
        var scanY = floorTop
        while (scanY < floorBottom) {
            val alpha = 0.03f * (1f - (scanY - floorTop) / (floorBottom - floorTop))
            drawLine(
                Color.White.copy(alpha = alpha),
                Offset(0f, scanY),
                Offset(size.width, scanY),
                strokeWidth = 1f,
            )
            scanY += 6f
        }

        if (fixtures.isEmpty()) return@Canvas

        // Compute horizontal bounds for distributing fixtures
        val minX = fixtures.minOf { it.position.x }
        val maxX = fixtures.maxOf { it.position.x }
        val rangeX = (maxX - minX).coerceAtLeast(1f)

        val padding = 60f
        val availableW = size.width - 2 * padding
        val stageH = stageBottom - stageTop

        // Draw truss bars (two horizontal bars across the stage)
        val trussY1 = stageTop + stageH * 0.15f
        val trussY2 = stageTop + stageH * 0.45f
        drawTruss(padding - 20f, trussY1, availableW + 40f)
        drawTruss(padding - 20f, trussY2, availableW + 40f)

        // Reusable Path to avoid allocations inside the fixture loop
        val reusablePath = Path()

        // Render each fixture
        for ((index, fixture) in fixtures.withIndex()) {
            val normX = (fixture.position.x - minX) / rangeX
            val fx = padding + normX * availableW

            val dmxColor = fixtureColors.getOrNull(index) ?: DmxColor.BLACK
            val composeColor = dmxColor.toComposeColor()

            val profile = BuiltInProfiles.findById(fixture.fixture.profileId)
            val renderHint = profile?.renderHint ?: RenderHint.POINT

            // Place fixtures on trusses based on their Z position
            // Higher Z (upstage) goes on the upper truss, lower Z on the lower truss
            val fixtureY = if (fixture.position.z > 0.5f) trussY1 else trussY2

            when (renderHint) {
                RenderHint.POINT -> drawAudiencePoint(fx, fixtureY, composeColor, floorTop, reusablePath)
                RenderHint.BAR -> {
                    val pixelCount = profile?.physical?.pixelCount ?: 8
                    drawAudienceBar(fx, fixtureY, composeColor, pixelCount)
                }
                RenderHint.BEAM_CONE -> {
                    drawAudienceBeamCone(fx, fixtureY, composeColor, floorTop, reusablePath)
                }
            }

            // Floor reflection — color wash below each fixture
            drawFloorReflection(fx, floorTop, composeColor, size.width)
        }
    }
}

/**
 * Draw a horizontal truss bar.
 */
private fun DrawScope.drawTruss(x: Float, y: Float, width: Float) {
    val trussH = 6f
    drawRect(
        color = TrussColor,
        topLeft = Offset(x, y - trussH / 2f),
        size = Size(width, trussH),
    )
    // Pixel details on the truss
    val segWidth = 20f
    var sx = x
    while (sx < x + width) {
        drawRect(
            color = TrussColor.copy(alpha = 0.3f),
            topLeft = Offset(sx, y - trussH / 2f - 2f),
            size = Size(3f, trussH + 4f),
        )
        sx += segWidth
    }
}

/**
 * Draw a POINT fixture from audience perspective — glowing dot on truss.
 */
private fun DrawScope.drawAudiencePoint(
    fx: Float,
    fy: Float,
    color: Color,
    floorY: Float,
    reusablePath: Path,
) {
    // Glow halo
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = 20f,
        center = Offset(fx, fy),
    )
    // Main circle
    drawCircle(
        color = color.copy(alpha = 0.7f),
        radius = 12f,
        center = Offset(fx, fy),
    )
    // Bright core
    drawCircle(
        color = color,
        radius = 7f,
        center = Offset(fx, fy),
    )

    // Downward light wash
    reusablePath.reset()
    reusablePath.moveTo(fx - 5f, fy + 10f)
    reusablePath.lineTo(fx - 25f, floorY)
    reusablePath.lineTo(fx + 25f, floorY)
    reusablePath.lineTo(fx + 5f, fy + 10f)
    reusablePath.close()
    drawPath(
        path = reusablePath,
        color = color.copy(alpha = 0.06f),
    )
}

/**
 * Draw a BAR fixture from audience perspective — row of colored segments on truss.
 */
private fun DrawScope.drawAudienceBar(
    fx: Float,
    fy: Float,
    color: Color,
    pixelCount: Int,
) {
    val segW = 6f
    val segH = 10f
    val gap = 1.5f
    val totalW = pixelCount * segW + (pixelCount - 1) * gap
    val startX = fx - totalW / 2f

    // Bar housing
    drawRect(
        color = Color(0xFF1A1A2E),
        topLeft = Offset(startX - 2f, fy - segH / 2f - 2f),
        size = Size(totalW + 4f, segH + 4f),
    )

    for (i in 0 until pixelCount) {
        val segX = startX + i * (segW + gap)
        val brightness = 0.7f + 0.3f * ((i % 3).toFloat() / 2f)
        drawRect(
            color = color.copy(alpha = brightness),
            topLeft = Offset(segX, fy - segH / 2f),
            size = Size(segW, segH),
        )
    }

    // Downward glow
    drawRect(
        color = color.copy(alpha = 0.1f),
        topLeft = Offset(startX - 4f, fy + segH / 2f + 2f),
        size = Size(totalW + 8f, 12f),
    )
}

/**
 * Draw a BEAM_CONE fixture from audience perspective — circle with visible beam cone down.
 */
private fun DrawScope.drawAudienceBeamCone(
    fx: Float,
    fy: Float,
    color: Color,
    floorY: Float,
    reusablePath: Path,
) {
    val beamLength = floorY - fy - 10f
    val beamTopWidth = 6f
    val beamBottomWidth = 35f

    // Beam cone
    reusablePath.reset()
    reusablePath.moveTo(fx - beamTopWidth, fy + 10f)
    reusablePath.lineTo(fx - beamBottomWidth, fy + beamLength)
    reusablePath.lineTo(fx + beamBottomWidth, fy + beamLength)
    reusablePath.lineTo(fx + beamTopWidth, fy + 10f)
    reusablePath.close()
    drawPath(
        path = reusablePath,
        color = color.copy(alpha = 0.08f),
    )

    // Beam center line (brighter)
    drawLine(
        color = color.copy(alpha = 0.15f),
        start = Offset(fx, fy + 10f),
        end = Offset(fx, fy + beamLength),
        strokeWidth = 3f,
    )

    // Fixture body
    drawCircle(
        color = Color(0xFF2A2A3E),
        radius = 10f,
        center = Offset(fx, fy),
    )
    drawCircle(
        color = color,
        radius = 6f,
        center = Offset(fx, fy),
    )
}

/**
 * Draw a subtle floor reflection for a fixture.
 */
private fun DrawScope.drawFloorReflection(
    fx: Float,
    floorY: Float,
    color: Color,
    canvasWidth: Float,
) {
    val reflectionW = 60f
    val reflectionH = 20f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(fx, floorY + 10f),
            radius = reflectionW,
        ),
        topLeft = Offset(
            (fx - reflectionW).coerceAtLeast(0f),
            floorY,
        ),
        size = Size(
            (reflectionW * 2f).coerceAtMost(canvasWidth),
            reflectionH,
        ),
    )
}

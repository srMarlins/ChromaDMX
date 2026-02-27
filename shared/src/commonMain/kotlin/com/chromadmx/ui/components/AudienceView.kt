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
import com.chromadmx.core.model.FixtureType
import com.chromadmx.core.model.RenderHint
import com.chromadmx.core.model.Color as DmxColor

/** Dark stage background for audience view. */
private val StageBackground = Color(0xFF060612)

/** Truss bar color (structural element). */
private val TrussColor = Color(0xFF2A2A3E)

/** Floor color beneath the stage. */
private val FloorColor = Color(0xFF0A0A14)

/** Standardized dark fixture housing color. */
private val HousingColor = Color(0xFF1A1A2E)

/** Lighter border for fixture housing. */
private val HousingBorderColor = Color(0xFF2A2A3E)

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

        // Compute Z bounds for vertical placement
        val minZ = fixtures.minOf { it.position.z }
        val maxZ = fixtures.maxOf { it.position.z }
        val rangeZ = maxZ - minZ

        // Compute Y bounds for depth scaling (once, outside the fixture loop)
        val minY = fixtures.minOf { it.position.y }
        val maxY = fixtures.maxOf { it.position.y }
        val rangeY = (maxY - minY).coerceAtLeast(1f)

        val padding = 60f
        val availableW = size.width - 2 * padding
        val stageH = stageBottom - stageTop

        // Scale fixture sizes based on available space per fixture
        val fixtureScale = ((availableW / fixtures.size.coerceAtLeast(1).toFloat()) / 50f)
            .coerceIn(0.8f, 2.5f)

        // Place trusses based on actual fixture Z heights
        val trussY1 = stageTop + stageH * 0.15f // upper truss
        val trussY2 = stageTop + stageH * 0.45f // lower truss

        // Collect distinct Z levels for truss drawing (rounded to nearest 0.1)
        val zLevels = fixtures
            .map { (it.position.z * 10f).let { z -> kotlin.math.round(z) } / 10f }
            .toSet()

        // Draw truss bars through actual fixture heights
        if (rangeZ < 0.1f) {
            // All fixtures at same height — one truss
            drawTruss(padding - 20f, trussY1, availableW + 40f)
        } else {
            // Draw a truss for each distinct Z level
            for (z in zLevels) {
                val normZ = ((z - minZ) / rangeZ).coerceIn(0f, 1f)
                val trussY = trussY2 + (trussY1 - trussY2) * normZ
                drawTruss(padding - 20f, trussY, availableW + 40f)
            }
        }

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

            // Place fixtures based on actual Z position
            val fixtureY = if (rangeZ < 0.1f) {
                // All same height — place on single upper truss
                trussY1
            } else {
                // Interpolate between trusses based on Z
                val normZ = ((fixture.position.z - minZ) / rangeZ).coerceIn(0f, 1f)
                trussY2 + (trussY1 - trussY2) * normZ
            }

            // Subtle depth scaling: further from audience (higher Y) = slightly smaller
            val depthScale = if (fixtures.size > 1) {
                val normY = ((fixture.position.y - minY) / rangeY).coerceIn(0f, 1f)
                1f - normY * 0.15f // 0.85x to 1x scale
            } else 1f

            val combinedScale = depthScale * fixtureScale

            when (renderHint) {
                RenderHint.POINT -> {
                    val fixtureType = profile?.type ?: FixtureType.PAR
                    when (fixtureType) {
                        FixtureType.STROBE -> drawAudienceStrobe(fx, fixtureY, composeColor, floorTop, combinedScale)
                        FixtureType.WASH -> drawAudienceWash(fx, fixtureY, composeColor, floorTop, reusablePath, combinedScale)
                        else -> drawAudiencePar(fx, fixtureY, composeColor, floorTop, reusablePath, combinedScale)
                    }
                }
                RenderHint.BAR -> {
                    val pixelCount = profile?.physical?.pixelCount ?: 8
                    drawAudienceBar(fx, fixtureY, composeColor, pixelCount, combinedScale)
                }
                RenderHint.BEAM_CONE -> {
                    drawAudienceBeamCone(fx, fixtureY, composeColor, floorTop, reusablePath, combinedScale)
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
 * Draw a PAR fixture from audience perspective — square housing with lens and downward wash.
 */
private fun DrawScope.drawAudiencePar(
    fx: Float,
    fy: Float,
    color: Color,
    floorY: Float,
    reusablePath: Path,
    scale: Float = 1f,
) {
    val housingSize = 14f * scale
    val half = housingSize / 2f
    val lensInset = 2f * scale

    // Radial glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(fx, fy),
            radius = 20f * scale,
        ),
        radius = 20f * scale,
        center = Offset(fx, fy),
    )
    // Housing
    drawRect(HousingBorderColor, Offset(fx - half - 1f, fy - half - 1f), Size(housingSize + 2f, housingSize + 2f))
    drawRect(HousingColor, Offset(fx - half, fy - half), Size(housingSize, housingSize))
    // Lens
    val lensSize = housingSize - 2 * lensInset
    drawRect(color, Offset(fx - half + lensInset, fy - half + lensInset), Size(lensSize, lensSize))

    // Downward light wash
    reusablePath.reset()
    reusablePath.moveTo(fx - 5f * scale, fy + half + 2f)
    reusablePath.lineTo(fx - 25f * scale, floorY)
    reusablePath.lineTo(fx + 25f * scale, floorY)
    reusablePath.lineTo(fx + 5f * scale, fy + half + 2f)
    reusablePath.close()
    drawPath(reusablePath, color.copy(alpha = 0.06f))
}

/**
 * Draw a STROBE fixture from audience perspective — wide rectangular flash panel.
 */
private fun DrawScope.drawAudienceStrobe(
    fx: Float,
    fy: Float,
    color: Color,
    floorY: Float,
    scale: Float = 1f,
) {
    val width = 20f * scale
    val height = 8f * scale
    val halfW = width / 2f
    val halfH = height / 2f

    // Sharp flash glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(fx, fy),
            radius = 24f * scale,
        ),
        radius = 24f * scale,
        center = Offset(fx, fy),
    )
    // Housing
    drawRect(HousingBorderColor, Offset(fx - halfW - 1f, fy - halfH - 1f), Size(width + 2f, height + 2f))
    drawRect(HousingColor, Offset(fx - halfW, fy - halfH), Size(width, height))
    // Flash panel
    val flashColor = Color(
        red = (color.red + 1f) / 2f,
        green = (color.green + 1f) / 2f,
        blue = (color.blue + 1f) / 2f,
    )
    drawRect(flashColor, Offset(fx - halfW + 2f, fy - halfH + 2f), Size(width - 4f, height - 4f))

    // Downward flash wash (wider, sharper)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
            startY = fy + halfH,
            endY = floorY,
        ),
        topLeft = Offset(fx - halfW, fy + halfH),
        size = Size(width, floorY - fy - halfH),
    )
}

/**
 * Draw a WASH fixture from audience perspective — larger housing with wide soft glow.
 */
private fun DrawScope.drawAudienceWash(
    fx: Float,
    fy: Float,
    color: Color,
    floorY: Float,
    reusablePath: Path,
    scale: Float = 1f,
) {
    val housingSize = 16f * scale
    val half = housingSize / 2f
    val lensRadius = 6f * scale

    // Wide soft glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(fx, fy),
            radius = 30f * scale,
        ),
        radius = 30f * scale,
        center = Offset(fx, fy),
    )
    // Housing
    drawRect(HousingBorderColor, Offset(fx - half - 1f, fy - half - 1f), Size(housingSize + 2f, housingSize + 2f))
    drawRect(HousingColor, Offset(fx - half, fy - half), Size(housingSize, housingSize))
    // Round lens
    drawCircle(color, radius = lensRadius, center = Offset(fx, fy))

    // Wide downward light wash (broader than par)
    reusablePath.reset()
    reusablePath.moveTo(fx - 8f * scale, fy + half + 2f)
    reusablePath.lineTo(fx - 40f * scale, floorY)
    reusablePath.lineTo(fx + 40f * scale, floorY)
    reusablePath.lineTo(fx + 8f * scale, fy + half + 2f)
    reusablePath.close()
    drawPath(reusablePath, color.copy(alpha = 0.05f))
}

/**
 * Draw a BAR fixture from audience perspective — row of colored segments with bidirectional glow.
 */
private fun DrawScope.drawAudienceBar(
    fx: Float,
    fy: Float,
    color: Color,
    pixelCount: Int,
    scale: Float = 1f,
) {
    val segW = 6f * scale
    val segH = 10f * scale
    val gap = 1.5f * scale
    val totalW = pixelCount * segW + (pixelCount - 1) * gap
    val startX = fx - totalW / 2f

    // Upward glow (LED tubes emit light upward too)
    drawRect(
        color = color.copy(alpha = 0.08f),
        topLeft = Offset(startX - 4f, fy - segH / 2f - 14f * scale),
        size = Size(totalW + 8f, 12f * scale),
    )

    // Bar housing
    drawRect(
        color = HousingColor,
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
        size = Size(totalW + 8f, 12f * scale),
    )
}

/**
 * Draw a BEAM_CONE fixture from audience perspective — square housing with visible beam cone down.
 */
private fun DrawScope.drawAudienceBeamCone(
    fx: Float,
    fy: Float,
    color: Color,
    floorY: Float,
    reusablePath: Path,
    scale: Float = 1f,
) {
    val housingSize = 12f * scale
    val half = housingSize / 2f
    val beamLength = floorY - fy - 10f
    val beamTopWidth = 6f * scale
    val beamBottomWidth = 35f * scale

    // Beam cone
    reusablePath.reset()
    reusablePath.moveTo(fx - beamTopWidth, fy + half + 2f)
    reusablePath.lineTo(fx - beamBottomWidth, fy + beamLength)
    reusablePath.lineTo(fx + beamBottomWidth, fy + beamLength)
    reusablePath.lineTo(fx + beamTopWidth, fy + half + 2f)
    reusablePath.close()
    drawPath(reusablePath, color.copy(alpha = 0.08f))

    // Beam center line (brighter)
    drawLine(
        color = color.copy(alpha = 0.15f),
        start = Offset(fx, fy + half + 2f),
        end = Offset(fx, fy + beamLength),
        strokeWidth = 3f * scale,
    )

    // Square housing
    drawRect(HousingBorderColor, Offset(fx - half - 1f, fy - half - 1f), Size(housingSize + 2f, housingSize + 2f))
    drawRect(HousingColor, Offset(fx - half, fy - half), Size(housingSize, housingSize))
    // Lens
    drawRect(color, Offset(fx - half + 2f, fy - half + 2f), Size(housingSize - 4f, housingSize - 4f))
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

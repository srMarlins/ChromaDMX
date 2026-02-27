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
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.core.model.Color as DmxColor

/**
 * Returns true when a fixture color is effectively black (no output).
 * Used to trigger the idle glow indicator so the audience view is never
 * completely dark.
 */
private fun isEffectivelyBlack(color: Color): Boolean {
    return color.red < 0.02f && color.green < 0.02f && color.blue < 0.02f
}

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
    val stageBackground = PixelDesign.colors.stageBackground
    val stageFloor = PixelDesign.colors.stageFloor
    val stageHorizon = PixelDesign.colors.stageHorizon
    val trussColor = PixelDesign.colors.trussColor
    val trussBorder = PixelDesign.colors.trussBorder
    val bracketColor = PixelDesign.colors.trussBorder
    val housingColor = PixelDesign.colors.fixtureHousing
    val housingBorderColor = PixelDesign.colors.fixtureHousingBorder
    val idleGlowColor = PixelDesign.colors.fixtureHousing.copy(alpha = 0.8f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(stageBackground)
            .pointerInput(Unit) {
                detectTapGestures { onBackgroundTapped() }
            },
    ) {
        val stageTop = size.height * 0.08f
        val stageBottom = size.height * 0.55f
        val floorTop = stageBottom + 10f
        val floorBottom = size.height

        // -- Stage backdrop: subtle vertical gradient for depth --
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    stageBackground.copy(alpha = 0.95f),  // slightly lighter at top (ambient sky)
                    stageBackground,    // dark in mid-stage
                ),
                startY = 0f,
                endY = stageBottom,
            ),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, stageBottom),
        )

        // -- Floor area: dark gradient with slight warmth --
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    stageFloor,
                    stageFloor.copy(alpha = 0.85f),  // deeper towards bottom
                    stageBackground,
                ),
                startY = floorTop,
                endY = floorBottom,
            ),
            topLeft = Offset(0f, floorTop),
            size = Size(size.width, floorBottom - floorTop),
        )

        // -- Horizon line: subtle bright edge where stage meets floor --
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    stageHorizon.copy(alpha = 0.5f),
                    stageHorizon.copy(alpha = 0.7f),
                    stageHorizon.copy(alpha = 0.5f),
                    Color.Transparent,
                ),
                startX = 0f,
                endX = size.width,
            ),
            start = Offset(0f, floorTop - 1f),
            end = Offset(size.width, floorTop - 1f),
            strokeWidth = 2f,
        )
        // Secondary softer horizon glow
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    stageHorizon.copy(alpha = 0.15f),
                    Color.Transparent,
                ),
                startY = floorTop - 4f,
                endY = floorTop + 12f,
            ),
            topLeft = Offset(0f, floorTop - 4f),
            size = Size(size.width, 16f),
        )

        // Horizontal scanlines on floor for depth
        var scanY = floorTop
        while (scanY < floorBottom) {
            // Scanlines that are denser near the horizon and fade with distance
            val progress = (scanY - floorTop) / (floorBottom - floorTop)
            val alpha = 0.04f * (1f - progress)
            drawLine(
                Color.White.copy(alpha = alpha),
                Offset(0f, scanY),
                Offset(size.width, scanY),
                strokeWidth = 1f,
            )
            // Increase spacing as we go further from horizon (perspective)
            scanY += 5f + progress * 8f
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

        // Collect fixture X positions for mounting brackets
        val fixtureXPositions = fixtures.map { fixture ->
            val normX = (fixture.position.x - minX) / rangeX
            padding + normX * availableW
        }

        // Draw truss bars through actual fixture heights
        if (rangeZ < 0.1f) {
            // All fixtures at same height — one truss
            drawTruss(padding - 20f, trussY1, availableW + 40f, fixtureXPositions, trussColor, trussBorder, bracketColor)
        } else {
            // Draw a truss for each distinct Z level
            for (z in zLevels) {
                val normZ = ((z - minZ) / rangeZ).coerceIn(0f, 1f)
                val trussY = trussY2 + (trussY1 - trussY2) * normZ
                // Collect fixture X positions at this Z level
                val positionsAtLevel = fixtures.mapIndexedNotNull { i, f ->
                    val fz = (f.position.z * 10f).let { fzr -> kotlin.math.round(fzr) } / 10f
                    if (fz == z) fixtureXPositions[i] else null
                }
                drawTruss(padding - 20f, trussY, availableW + 40f, positionsAtLevel, trussColor, trussBorder, bracketColor)
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

            // Determine the effective draw color — use idle glow if fixture is black
            val isDark = isEffectivelyBlack(composeColor)
            val drawColor = if (isDark) idleGlowColor else composeColor

            when (renderHint) {
                RenderHint.POINT -> {
                    val fixtureType = profile?.type ?: FixtureType.PAR
                    when (fixtureType) {
                        FixtureType.STROBE -> drawAudienceStrobe(fx, fixtureY, drawColor, floorTop, combinedScale, isDark, housingColor, housingBorderColor)
                        FixtureType.WASH -> drawAudienceWash(fx, fixtureY, drawColor, floorTop, reusablePath, combinedScale, isDark, housingColor, housingBorderColor)
                        else -> drawAudiencePar(fx, fixtureY, drawColor, floorTop, reusablePath, combinedScale, isDark, housingColor, housingBorderColor)
                    }
                }
                RenderHint.BAR -> {
                    val pixelCount = profile?.physical?.pixelCount ?: 8
                    drawAudienceBar(fx, fixtureY, drawColor, pixelCount, combinedScale, isDark, housingColor)
                }
                RenderHint.BEAM_CONE -> {
                    drawAudienceBeamCone(fx, fixtureY, drawColor, floorTop, reusablePath, combinedScale, isDark, housingColor, housingBorderColor)
                }
            }

            // Floor reflection — color wash below each fixture
            drawFloorReflection(fx, floorTop, drawColor, size.width, isDark)
        }
    }
}

/**
 * Draw a horizontal truss bar with cross-bracing and mounting brackets.
 *
 * The truss is rendered as a dual-rail structure with diagonal cross-braces
 * between the rails for a realistic look, plus mounting bracket shapes at
 * each fixture position.
 */
private fun DrawScope.drawTruss(
    x: Float,
    y: Float,
    width: Float,
    fixturePositions: List<Float> = emptyList(),
    trussColor: Color,
    trussBorder: Color,
    bracketColor: Color,
) {
    val trussH = 10f
    val railH = 3f
    val halfH = trussH / 2f

    // Top rail
    drawRect(
        color = trussColor,
        topLeft = Offset(x, y - halfH),
        size = Size(width, railH),
    )
    // Bottom rail
    drawRect(
        color = trussColor,
        topLeft = Offset(x, y + halfH - railH),
        size = Size(width, railH),
    )

    // Highlight on top edge of upper rail (light catch)
    drawLine(
        color = trussBorder.copy(alpha = 0.4f),
        start = Offset(x, y - halfH),
        end = Offset(x + width, y - halfH),
        strokeWidth = 1f,
    )

    // Cross-bracing between rails (diagonal lattice)
    val segWidth = 18f
    var sx = x
    var even = true
    while (sx < x + width) {
        val nextX = (sx + segWidth).coerceAtMost(x + width)

        // Vertical struts at each segment boundary
        drawLine(
            color = trussColor.copy(alpha = 0.6f),
            start = Offset(sx, y - halfH + railH),
            end = Offset(sx, y + halfH - railH),
            strokeWidth = 1.5f,
        )

        // Diagonal cross-brace (alternating direction)
        if (even) {
            drawLine(
                color = trussColor.copy(alpha = 0.35f),
                start = Offset(sx, y - halfH + railH),
                end = Offset(nextX, y + halfH - railH),
                strokeWidth = 1f,
            )
        } else {
            drawLine(
                color = trussColor.copy(alpha = 0.35f),
                start = Offset(sx, y + halfH - railH),
                end = Offset(nextX, y - halfH + railH),
                strokeWidth = 1f,
            )
        }
        even = !even
        sx += segWidth
    }
    // Final vertical strut at right end
    drawLine(
        color = trussColor.copy(alpha = 0.6f),
        start = Offset((x + width).coerceAtMost(size.width), y - halfH + railH),
        end = Offset((x + width).coerceAtMost(size.width), y + halfH - railH),
        strokeWidth = 1.5f,
    )

    // Mounting brackets at each fixture position
    for (fxPos in fixturePositions) {
        if (fxPos < x || fxPos > x + width) continue
        drawMountingBracket(fxPos, y + halfH, 8f, bracketColor, trussBorder)
    }
}

/**
 * Draw a small mounting bracket hanging from the truss at a fixture position.
 * Looks like a C-clamp / pipe clamp attachment.
 */
private fun DrawScope.drawMountingBracket(
    cx: Float,
    topY: Float,
    bracketSize: Float,
    bracketColor: Color,
    trussBorder: Color,
) {
    val halfW = bracketSize / 2f
    val height = bracketSize * 0.8f

    // Bracket body (small rectangle hanging from truss)
    drawRect(
        color = bracketColor,
        topLeft = Offset(cx - halfW, topY),
        size = Size(bracketSize, height),
    )
    // Highlight edge
    drawRect(
        color = trussBorder.copy(alpha = 0.3f),
        topLeft = Offset(cx - halfW, topY),
        size = Size(bracketSize, 1.5f),
    )
    // Bolt detail (small dot)
    drawCircle(
        color = trussBorder.copy(alpha = 0.5f),
        radius = 1.5f,
        center = Offset(cx, topY + height / 2f),
    )
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
    isIdle: Boolean = false,
    housingColor: Color,
    housingBorderColor: Color,
) {
    val housingSize = 14f * scale
    val half = housingSize / 2f
    val lensInset = 2f * scale

    // Radial glow (dimmed for idle)
    val glowAlpha = if (isIdle) 0.08f else 0.35f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(fx, fy),
            radius = 24f * scale,
        ),
        radius = 24f * scale,
        center = Offset(fx, fy),
    )
    // Housing
    drawRect(housingBorderColor, Offset(fx - half - 1f, fy - half - 1f), Size(housingSize + 2f, housingSize + 2f))
    drawRect(housingColor, Offset(fx - half, fy - half), Size(housingSize, housingSize))
    // Lens
    val lensSize = housingSize - 2 * lensInset
    val lensColor = if (isIdle) color.copy(alpha = 0.3f) else color
    drawRect(lensColor, Offset(fx - half + lensInset, fy - half + lensInset), Size(lensSize, lensSize))

    // Downward light wash — gradient beam cone
    if (!isIdle) {
        val beamTopHalf = 8f * scale
        val beamBottomHalf = 35f * scale
        val beamTop = fy + half + 2f
        val beamBottom = floorY

        // Outer beam (subtle fill)
        reusablePath.reset()
        reusablePath.moveTo(fx - beamTopHalf, beamTop)
        reusablePath.lineTo(fx - beamBottomHalf, beamBottom)
        reusablePath.lineTo(fx + beamBottomHalf, beamBottom)
        reusablePath.lineTo(fx + beamTopHalf, beamTop)
        reusablePath.close()
        drawPath(
            reusablePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.15f),
                    color.copy(alpha = 0.03f),
                ),
                startY = beamTop,
                endY = beamBottom,
            ),
        )

        // Inner beam core (brighter center)
        val coreTopHalf = 3f * scale
        val coreBottomHalf = 15f * scale
        reusablePath.reset()
        reusablePath.moveTo(fx - coreTopHalf, beamTop)
        reusablePath.lineTo(fx - coreBottomHalf, beamBottom)
        reusablePath.lineTo(fx + coreBottomHalf, beamBottom)
        reusablePath.lineTo(fx + coreTopHalf, beamTop)
        reusablePath.close()
        drawPath(
            reusablePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.12f),
                    color.copy(alpha = 0.01f),
                ),
                startY = beamTop,
                endY = beamBottom,
            ),
        )
    }
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
    isIdle: Boolean = false,
    housingColor: Color,
    housingBorderColor: Color,
) {
    val width = 20f * scale
    val height = 8f * scale
    val halfW = width / 2f
    val halfH = height / 2f

    // Sharp flash glow (dimmed for idle)
    val flashGlowAlpha = if (isIdle) 0.06f else 0.3f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = flashGlowAlpha), Color.Transparent),
            center = Offset(fx, fy),
            radius = 28f * scale,
        ),
        radius = 28f * scale,
        center = Offset(fx, fy),
    )
    // Housing
    drawRect(housingBorderColor, Offset(fx - halfW - 1f, fy - halfH - 1f), Size(width + 2f, height + 2f))
    drawRect(housingColor, Offset(fx - halfW, fy - halfH), Size(width, height))
    // Flash panel
    val flashColor = if (isIdle) {
        color.copy(alpha = 0.25f)
    } else {
        Color(
            red = (color.red + 1f) / 2f,
            green = (color.green + 1f) / 2f,
            blue = (color.blue + 1f) / 2f,
        )
    }
    drawRect(flashColor, Offset(fx - halfW + 2f, fy - halfH + 2f), Size(width - 4f, height - 4f))

    // Downward flash wash (wider, sharper) — gradient version
    if (!isIdle) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    color.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                startY = fy + halfH,
                endY = floorY,
            ),
            topLeft = Offset(fx - halfW * 1.5f, fy + halfH),
            size = Size(width * 1.5f, floorY - fy - halfH),
        )
    }
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
    isIdle: Boolean = false,
    housingColor: Color,
    housingBorderColor: Color,
) {
    val housingSize = 16f * scale
    val half = housingSize / 2f
    val lensRadius = 6f * scale

    // Wide soft glow (dimmed for idle)
    val glowAlpha = if (isIdle) 0.06f else 0.25f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(fx, fy),
            radius = 35f * scale,
        ),
        radius = 35f * scale,
        center = Offset(fx, fy),
    )
    // Housing
    drawRect(housingBorderColor, Offset(fx - half - 1f, fy - half - 1f), Size(housingSize + 2f, housingSize + 2f))
    drawRect(housingColor, Offset(fx - half, fy - half), Size(housingSize, housingSize))
    // Round lens
    val lensColor = if (isIdle) color.copy(alpha = 0.3f) else color
    drawCircle(lensColor, radius = lensRadius, center = Offset(fx, fy))

    // Wide downward light wash — gradient beam
    if (!isIdle) {
        val beamTopHalf = 10f * scale
        val beamBottomHalf = 55f * scale
        val beamTop = fy + half + 2f
        val beamBottom = floorY

        // Wide outer beam
        reusablePath.reset()
        reusablePath.moveTo(fx - beamTopHalf, beamTop)
        reusablePath.lineTo(fx - beamBottomHalf, beamBottom)
        reusablePath.lineTo(fx + beamBottomHalf, beamBottom)
        reusablePath.lineTo(fx + beamTopHalf, beamTop)
        reusablePath.close()
        drawPath(
            reusablePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.12f),
                    color.copy(alpha = 0.02f),
                ),
                startY = beamTop,
                endY = beamBottom,
            ),
        )

        // Soft inner core
        val coreTopHalf = 4f * scale
        val coreBottomHalf = 25f * scale
        reusablePath.reset()
        reusablePath.moveTo(fx - coreTopHalf, beamTop)
        reusablePath.lineTo(fx - coreBottomHalf, beamBottom)
        reusablePath.lineTo(fx + coreBottomHalf, beamBottom)
        reusablePath.lineTo(fx + coreTopHalf, beamTop)
        reusablePath.close()
        drawPath(
            reusablePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.08f),
                    color.copy(alpha = 0.01f),
                ),
                startY = beamTop,
                endY = beamBottom,
            ),
        )
    }
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
    isIdle: Boolean = false,
    housingColor: Color,
) {
    val segW = 6f * scale
    val segH = 10f * scale
    val gap = 1.5f * scale
    val totalW = pixelCount * segW + (pixelCount - 1) * gap
    val startX = fx - totalW / 2f

    // Upward glow (LED tubes emit light upward too)
    val upAlpha = if (isIdle) 0.03f else 0.1f
    drawRect(
        color = color.copy(alpha = upAlpha),
        topLeft = Offset(startX - 4f, fy - segH / 2f - 14f * scale),
        size = Size(totalW + 8f, 12f * scale),
    )

    // Bar housing
    drawRect(
        color = housingColor,
        topLeft = Offset(startX - 2f, fy - segH / 2f - 2f),
        size = Size(totalW + 4f, segH + 4f),
    )

    for (i in 0 until pixelCount) {
        val segX = startX + i * (segW + gap)
        val brightness = if (isIdle) {
            0.2f + 0.1f * ((i % 3).toFloat() / 2f)
        } else {
            0.7f + 0.3f * ((i % 3).toFloat() / 2f)
        }
        drawRect(
            color = color.copy(alpha = brightness),
            topLeft = Offset(segX, fy - segH / 2f),
            size = Size(segW, segH),
        )
    }

    // Downward glow
    val downAlpha = if (isIdle) 0.04f else 0.12f
    drawRect(
        color = color.copy(alpha = downAlpha),
        topLeft = Offset(startX - 4f, fy + segH / 2f + 2f),
        size = Size(totalW + 8f, 14f * scale),
    )
}

/**
 * Draw a BEAM_CONE fixture from audience perspective — square housing with visible beam cone down.
 *
 * Enhanced with gradient alpha beams that are bright near the fixture and fade to transparent
 * at the floor, wider spread, and a brighter central core for dramatic effect.
 */
private fun DrawScope.drawAudienceBeamCone(
    fx: Float,
    fy: Float,
    color: Color,
    floorY: Float,
    reusablePath: Path,
    scale: Float = 1f,
    isIdle: Boolean = false,
    housingColor: Color,
    housingBorderColor: Color,
) {
    val housingSize = 14f * scale
    val half = housingSize / 2f
    val beamTop = fy + half + 2f
    val beamBottom = floorY
    val beamTopWidth = 8f * scale
    val beamBottomWidth = 50f * scale

    if (!isIdle) {
        // Atmospheric haze glow around the beam (very wide, very subtle)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(fx, (beamTop + beamBottom) / 2f),
                radius = beamBottomWidth * 1.5f,
            ),
            radius = beamBottomWidth * 1.5f,
            center = Offset(fx, (beamTop + beamBottom) / 2f),
        )

        // Outer beam cone — gradient from bright to transparent
        reusablePath.reset()
        reusablePath.moveTo(fx - beamTopWidth, beamTop)
        reusablePath.lineTo(fx - beamBottomWidth, beamBottom)
        reusablePath.lineTo(fx + beamBottomWidth, beamBottom)
        reusablePath.lineTo(fx + beamTopWidth, beamTop)
        reusablePath.close()
        drawPath(
            reusablePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.2f),
                    color.copy(alpha = 0.08f),
                    color.copy(alpha = 0.02f),
                ),
                startY = beamTop,
                endY = beamBottom,
            ),
        )

        // Inner beam core (tighter, brighter)
        val coreTopWidth = 3f * scale
        val coreBottomWidth = 20f * scale
        reusablePath.reset()
        reusablePath.moveTo(fx - coreTopWidth, beamTop)
        reusablePath.lineTo(fx - coreBottomWidth, beamBottom)
        reusablePath.lineTo(fx + coreBottomWidth, beamBottom)
        reusablePath.lineTo(fx + coreTopWidth, beamTop)
        reusablePath.close()
        drawPath(
            reusablePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.18f),
                    color.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                startY = beamTop,
                endY = beamBottom,
            ),
        )

        // Beam center line (brighter hotspot)
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.25f),
                    color.copy(alpha = 0.04f),
                ),
                startY = beamTop,
                endY = beamBottom,
            ),
            start = Offset(fx, beamTop),
            end = Offset(fx, beamBottom),
            strokeWidth = 4f * scale,
        )
    }

    // Fixture housing glow (bright at lens)
    val housingGlowAlpha = if (isIdle) 0.1f else 0.4f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = housingGlowAlpha), Color.Transparent),
            center = Offset(fx, fy),
            radius = 18f * scale,
        ),
        radius = 18f * scale,
        center = Offset(fx, fy),
    )

    // Square housing
    drawRect(housingBorderColor, Offset(fx - half - 1f, fy - half - 1f), Size(housingSize + 2f, housingSize + 2f))
    drawRect(housingColor, Offset(fx - half, fy - half), Size(housingSize, housingSize))
    // Lens
    val lensColor = if (isIdle) color.copy(alpha = 0.3f) else color
    drawRect(lensColor, Offset(fx - half + 2f, fy - half + 2f), Size(housingSize - 4f, housingSize - 4f))
}

/**
 * Draw a floor reflection for a fixture — radial glow where the beam hits the floor.
 *
 * Enhanced with larger radius, brighter center, and an elliptical spread for realism.
 */
private fun DrawScope.drawFloorReflection(
    fx: Float,
    floorY: Float,
    color: Color,
    canvasWidth: Float,
    isIdle: Boolean = false,
) {
    val reflectionRadius = if (isIdle) 40f else 90f
    val reflectionH = if (isIdle) 20f else 50f
    val centerAlpha = if (isIdle) 0.04f else 0.18f
    val midAlpha = if (isIdle) 0.02f else 0.08f
    val reflectionCenterY = floorY + 15f

    // Primary radial floor glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = centerAlpha),
                color.copy(alpha = midAlpha),
                Color.Transparent,
            ),
            center = Offset(fx, reflectionCenterY),
            radius = reflectionRadius,
        ),
        radius = reflectionRadius,
        center = Offset(fx, reflectionCenterY),
    )

    // Secondary wider but dimmer spread (light scatter on floor)
    if (!isIdle) {
        val spreadRadius = reflectionRadius * 1.6f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                center = Offset(fx, reflectionCenterY + 5f),
                radius = spreadRadius,
            ),
            topLeft = Offset(
                (fx - spreadRadius).coerceAtLeast(0f),
                floorY,
            ),
            size = Size(
                (spreadRadius * 2f).coerceAtMost(canvasWidth),
                reflectionH * 1.5f,
            ),
        )
    }
}

package com.chromadmx.ui.screen.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.util.presetDisplayName

/**
 * Fixture scan screen during onboarding.
 *
 * In simulation mode: shows a rig preset picker (grid of 3 presets)
 * and an animated fixture pop-in visualization. The user selects a rig
 * and watches fixtures "appear" on a simulated camera canvas.
 *
 * For real hardware: shows a placeholder message about camera scan.
 *
 * @param isSimulationMode Whether we are in simulation mode.
 * @param selectedPreset The currently selected rig preset.
 * @param fixturesLoaded Number of fixtures that have "popped in" so far.
 * @param totalFixtures Total number of fixtures in the selected rig.
 * @param onSelectPreset Called when the user taps a rig preset.
 * @param onContinue Called when the user taps Continue (after scan completes).
 */
@Composable
fun FixtureScanScreen(
    isSimulationMode: Boolean,
    selectedPreset: RigPreset,
    fixturesLoaded: Int,
    totalFixtures: Int,
    onSelectPreset: (RigPreset) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSimulationMode) {
        SimulationFixtureScan(
            selectedPreset = selectedPreset,
            fixturesLoaded = fixturesLoaded,
            totalFixtures = totalFixtures,
            onSelectPreset = onSelectPreset,
            onContinue = onContinue,
            modifier = modifier,
        )
    } else {
        RealHardwarePlaceholder(
            onContinue = onContinue,
            modifier = modifier,
        )
    }
}

@Composable
private fun SimulationFixtureScan(
    selectedPreset: RigPreset,
    fixturesLoaded: Int,
    totalFixtures: Int,
    onSelectPreset: (RigPreset) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanComplete = totalFixtures > 0 && fixturesLoaded >= totalFixtures

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "CHOOSE YOUR RIG",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = NeonCyan,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pick a virtual fixture layout",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Rig preset picker (3 items in a row)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(RigPreset.entries.toList()) { preset ->
                RigPresetTile(
                    preset = preset,
                    isSelected = preset == selectedPreset,
                    onClick = { onSelectPreset(preset) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated camera canvas
        Text(
            text = "SIMULATED CAMERA",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = PixelFontFamily),
            color = NeonMagenta,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))

        FixturePopInCanvas(
            discoveredCount = fixturesLoaded,
            totalFixtures = totalFixtures,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(200.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        if (totalFixtures > 0) {
            PixelProgressBar(
                progress = fixturesLoaded.toFloat() / totalFixtures,
                modifier = Modifier.fillMaxWidth(0.8f),
                progressColor = NeonCyan,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$fixturesLoaded / $totalFixtures fixtures found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = scanComplete) {
            PixelButton(
                onClick = onContinue,
                backgroundColor = NeonCyan,
                contentColor = Color.Black,
            ) {
                Text("CONTINUE")
            }
        }
    }
}

/**
 * A tile for selecting a rig preset.
 */
@Composable
private fun RigPresetTile(
    preset: RigPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.3f)
    val bgColor = if (isSelected) NeonCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    val rig = remember(preset) { SimulatedFixtureRig(preset) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .pixelBorder(color = borderColor, pixelSize = 2.dp)
            .background(bgColor)
            .padding(2.dp) // border inset
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = preset.presetDisplayName(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PixelFontFamily),
            fontWeight = FontWeight.Bold,
            color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${rig.fixtureCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Canvas showing fixture dots appearing one by one with a pop-in effect.
 */
@Composable
private fun FixturePopInCanvas(
    discoveredCount: Int,
    totalFixtures: Int,
    modifier: Modifier = Modifier,
) {
    // Animate the overall alpha for a "pulse" feel as new fixtures appear
    val pulseAlpha by animateFloatAsState(
        targetValue = if (discoveredCount < totalFixtures) 0.8f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "fixture-pulse",
    )

    Canvas(
        modifier = modifier.background(Color(0xFF0A0A1A)),
    ) {
        val padding = 16f
        val canvasW = size.width - 2 * padding
        val canvasH = size.height - 2 * padding
        if (canvasW <= 0f || canvasH <= 0f || totalFixtures <= 0) return@Canvas

        // CRT scanlines
        val scanLineSpacing = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += scanLineSpacing
        }

        // Grid layout for fixtures
        val cols = when {
            totalFixtures <= 8 -> totalFixtures
            totalFixtures <= 30 -> 10
            else -> 12
        }
        val rows = (totalFixtures + cols - 1) / cols

        for (i in 0 until discoveredCount.coerceAtMost(totalFixtures)) {
            val col = i % cols
            val row = i / cols

            val cx = padding + (col + 0.5f) * canvasW / cols
            val cy = padding + (row + 0.5f) * canvasH / rows.coerceAtLeast(1)

            // Outer glow
            drawCircle(
                color = Color.White.copy(alpha = 0.15f * pulseAlpha),
                radius = 14f,
                center = Offset(cx, cy),
            )
            // Inner bright point
            drawCircle(
                color = Color.White.copy(alpha = 0.7f * pulseAlpha),
                radius = 5f,
                center = Offset(cx, cy),
            )
        }
    }
}

@Composable
private fun RealHardwarePlaceholder(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Camera Scan",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
            color = NeonCyan,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera-based fixture scanning coming soon. For now, you can map fixtures manually from the settings screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        PixelButton(
            onClick = onContinue,
            backgroundColor = NeonCyan,
            contentColor = Color.Black,
        ) {
            Text("CONTINUE")
        }
    }
}

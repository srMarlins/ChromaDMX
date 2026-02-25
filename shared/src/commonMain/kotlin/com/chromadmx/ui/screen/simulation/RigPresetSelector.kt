package com.chromadmx.ui.screen.simulation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Display-friendly metadata for a [RigPreset].
 */
private data class PresetInfo(
    val preset: RigPreset,
    val displayName: String,
    val fixtureCount: Int,
    val description: String,
)

/**
 * Rig preset selection screen with pixel-art styled cards.
 *
 * Displays presets in a 2-column grid. Each card shows the preset name,
 * fixture count, a mini canvas preview of fixture positions, and a
 * selected-state highlight. A "Start Virtual Stage" button at the
 * bottom confirms the selection.
 *
 * Used during onboarding (when no hardware is found) and from settings.
 *
 * @param selectedPreset The currently selected preset.
 * @param onSelectPreset Called when a preset card is tapped.
 * @param onConfirm Called when the user taps "Start Virtual Stage".
 * @param modifier Layout modifier.
 */
@Composable
fun RigPresetSelector(
    selectedPreset: RigPreset,
    onSelectPreset: (RigPreset) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presetInfoList = remember {
        RigPreset.entries.map { preset ->
            val rig = SimulatedFixtureRig(preset)
            PresetInfo(
                preset = preset,
                displayName = preset.displayName(),
                fixtureCount = rig.fixtureCount,
                description = preset.shortDescription(),
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Header
            Text(
                text = "SELECT YOUR RIG",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = PixelFontFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose a virtual fixture layout",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Preset grid (2 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(presetInfoList) { info ->
                    PresetCard(
                        info = info,
                        isSelected = info.preset == selectedPreset,
                        onClick = { onSelectPreset(info.preset) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm button
            PixelButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeonCyan,
                contentColor = Color.Black,
            ) {
                Text("START VIRTUAL STAGE")
            }
        }
    }
}

/**
 * A single preset card in the grid.
 */
@Composable
private fun PresetCard(
    info: PresetInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.3f)
    val bgColor = if (isSelected) {
        NeonCyan.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .pixelBorder(color = borderColor, pixelSize = 3.dp)
            .background(bgColor)
            .padding(3.dp) // border inset
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mini canvas preview
        RigPreviewCanvas(
            preset = info.preset,
            accentColor = if (isSelected) NeonCyan else NeonMagenta.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Preset name
        Text(
            text = info.displayName,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
            fontWeight = FontWeight.Bold,
            color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        // Fixture count
        Text(
            text = "${info.fixtureCount} fixtures",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Description
        Text(
            text = info.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Mini Canvas drawing that shows a simplified top-down view of fixture
 * positions for a given [RigPreset].
 */
@Composable
private fun RigPreviewCanvas(
    preset: RigPreset,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val fixtures = remember(preset) {
        SimulatedFixtureRig(preset).fixtures
    }

    Canvas(
        modifier = modifier.background(Color(0xFF080818)),
    ) {
        if (fixtures.isEmpty()) return@Canvas

        val padding = 8f
        val canvasW = size.width - 2 * padding
        val canvasH = size.height - 2 * padding
        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        // Compute bounds from x/y positions
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

        // Draw stage floor outline
        drawRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(canvasW, canvasH),
        )

        // Draw fixture dots
        val dotRadius = when (preset) {
            RigPreset.SMALL_DJ -> 4f
            RigPreset.TRUSS_RIG -> 3f
            RigPreset.FESTIVAL_STAGE -> 2f
        }

        for (fixture in fixtures) {
            val normX = (fixture.position.x - minX) / rangeX
            val normY = (fixture.position.y - minY) / rangeY
            val cx = padding + normX * canvasW
            val cy = padding + (1f - normY) * canvasH

            // Glow
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = dotRadius * 2f,
                center = Offset(cx, cy),
            )
            // Dot
            drawCircle(
                color = accentColor,
                radius = dotRadius,
                center = Offset(cx, cy),
            )
        }
    }
}

/**
 * User-friendly display name for a [RigPreset].
 */
private fun RigPreset.displayName(): String = when (this) {
    RigPreset.SMALL_DJ -> "Small DJ"
    RigPreset.TRUSS_RIG -> "Truss Rig"
    RigPreset.FESTIVAL_STAGE -> "Festival Stage"
}

/**
 * Short description for a [RigPreset].
 */
private fun RigPreset.shortDescription(): String = when (this) {
    RigPreset.SMALL_DJ -> "8 RGB PARs, single truss"
    RigPreset.TRUSS_RIG -> "30 pixel bars, dual truss"
    RigPreset.FESTIVAL_STAGE -> "108 mixed fixtures"
}

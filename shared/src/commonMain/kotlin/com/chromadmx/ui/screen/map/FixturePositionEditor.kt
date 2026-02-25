package com.chromadmx.ui.screen.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3

/**
 * Canvas-based 2D top-down view showing fixture positions as dots.
 *
 * Fixtures are rendered at their (x, y) positions, scaled to fit the canvas.
 * The selected fixture is highlighted. A Z-height slider is shown below
 * when a fixture is selected.
 */
@Composable
fun FixturePositionEditor(
    fixtures: List<Fixture3D>,
    selectedIndex: Int?,
    onUpdatePosition: (Int, Vec3) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Canvas area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0A0A1A))
                .padding(8.dp),
        ) {
            if (fixtures.isEmpty()) {
                Text(
                    text = "No fixtures to display",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val padding = 32f
                    val canvasW = size.width - 2 * padding
                    val canvasH = size.height - 2 * padding

                    // Find bounds of fixtures for scaling
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

                    for ((index, fixture) in fixtures.withIndex()) {
                        val normX = (fixture.position.x - minX) / rangeX
                        val normY = (fixture.position.y - minY) / rangeY
                        val cx = padding + normX * canvasW
                        val cy = padding + (1f - normY) * canvasH // flip Y for top-down view

                        val isSelected = index == selectedIndex
                        val dotColor = if (isSelected) {
                            Color(0xFF6C63FF) // primary
                        } else {
                            Color(0xFF00E5FF) // secondary
                        }
                        val radius = if (isSelected) 14f else 10f

                        drawCircle(
                            color = dotColor,
                            radius = radius,
                            center = Offset(cx, cy),
                        )

                        // Selection ring
                        if (isSelected) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.3f),
                                radius = 22f,
                                center = Offset(cx, cy),
                            )
                        }
                    }
                }
            }

            // Camera preview placeholder
            Text(
                text = "Camera preview (platform-specific)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }

        // Z-height slider for selected fixture
        if (selectedIndex != null && selectedIndex in fixtures.indices) {
            val fixture = fixtures[selectedIndex]
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Z Height",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Slider(
                    value = fixture.position.z,
                    onValueChange = { newZ ->
                        onUpdatePosition(
                            selectedIndex,
                            fixture.position.copy(z = newZ),
                        )
                    },
                    valueRange = 0f..10f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Text(
                    text = formatFloat(fixture.position.z),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun formatFloat(value: Float): String {
    val intPart = value.toInt()
    val fracPart = ((value - intPart) * 10).toInt().let { kotlin.math.abs(it) }
    return if (value < 0 && intPart == 0) "-0.$fracPart" else "$intPart.$fracPart"
}

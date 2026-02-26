package com.chromadmx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.agent.scene.Scene
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.core.model.Color as DmxColor

/**
 * Horizontal scrollable strip of preset thumbnails.
 *
 * Each preset is rendered as a small pixel-art styled card with a mini
 * color preview canvas and the preset name below. Supports tap to apply
 * and long-press to preview.
 *
 * @param scenes List of available scenes/presets.
 * @param activeSceneName Name of the currently active scene, or null.
 * @param onSceneTap Called when user taps a preset to apply it.
 * @param onSceneLongPress Called when user long-presses to preview (name), or null to revert.
 */
@Composable
fun PresetStrip(
    scenes: List<Scene>,
    activeSceneName: String?,
    onSceneTap: (String) -> Unit,
    onSceneLongPress: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PixelDesign.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.95f)),
    ) {
        if (scenes.isEmpty()) {
            Text(
                text = "No presets",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                ),
                color = colors.onSurfaceDim,
                modifier = Modifier.align(Alignment.Center).padding(vertical = 12.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(scenes) { _, scene ->
                    PresetThumbnail(
                        scene = scene,
                        isActive = scene.name == activeSceneName,
                        onTap = { onSceneTap(scene.name) },
                        onLongPress = { onSceneLongPress(scene.name) },
                        onRelease = { onSceneLongPress(null) },
                    )
                }
            }
        }
    }
}

/**
 * A single preset thumbnail card with a mini color preview and name label.
 */
@Composable
private fun PresetThumbnail(
    scene: Scene,
    isActive: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
) {
    val colors = PixelDesign.colors
    val borderColor = if (isActive) colors.primary else colors.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .pointerInput(scene.name) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = {
                        onLongPress()
                    },
                    onPress = {
                        tryAwaitRelease()
                        onRelease()
                    },
                )
            },
    ) {
        // Mini color preview canvas
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(36.dp)
                .pixelBorder(width = 2.dp, color = borderColor, pixelSize = 2.dp)
                .background(colors.background)
                .padding(2.dp),
        ) {
            PresetMiniCanvas(
                scene = scene,
                modifier = Modifier.matchParentSize(),
            )
        }

        // Name label
        Text(
            text = scene.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontFamily = PixelFontFamily,
            ),
            color = if (isActive) colors.primary else colors.onSurfaceDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/**
 * Mini canvas inside a preset thumbnail showing approximate colors.
 *
 * Uses the scene's layer info to derive representative colors. If layers
 * have no extractable color, falls back to a dim pattern.
 */
@Composable
private fun PresetMiniCanvas(
    scene: Scene,
    modifier: Modifier = Modifier,
) {
    // Extract representative colors from the scene layers
    val colors = scene.layers.mapNotNull { layer ->
        // Try to extract a color from the layer params
        val r = layer.params["r"] ?: layer.params["red"]
        val g = layer.params["g"] ?: layer.params["green"]
        val b = layer.params["b"] ?: layer.params["blue"]
        if (r != null && g != null && b != null) {
            Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        } else {
            // Use a hue based on effectId hash for visual distinction
            val hue = (layer.effectId.hashCode() and 0x7FFFFFFF) % 360
            Color.hsl(hue.toFloat(), 0.7f, 0.5f)
        }
    }.ifEmpty {
        // Fallback: dim pattern based on scene name hash
        val hue = (scene.name.hashCode() and 0x7FFFFFFF) % 360
        listOf(
            Color.hsl(hue.toFloat(), 0.6f, 0.4f),
            Color.hsl((hue + 120) % 360f, 0.5f, 0.3f),
        )
    }

    Canvas(modifier = modifier) {
        val segmentW = size.width / colors.size.coerceAtLeast(1)

        for ((i, color) in colors.withIndex()) {
            val adjustedOpacity = scene.masterDimmer.coerceIn(0.2f, 1f)

            // Draw colored segment
            drawRect(
                color = color.copy(alpha = adjustedOpacity * 0.8f),
                topLeft = Offset(i * segmentW, 0f),
                size = Size(segmentW, size.height),
            )

            // Small glow dot at center of each segment
            drawCircle(
                color = color.copy(alpha = adjustedOpacity),
                radius = 3f,
                center = Offset(i * segmentW + segmentW / 2f, size.height / 2f),
            )
        }
    }
}

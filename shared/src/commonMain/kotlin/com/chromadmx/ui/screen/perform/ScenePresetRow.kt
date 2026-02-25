package com.chromadmx.ui.screen.perform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.ScenePreset
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.ui.theme.DmxPrimary
import com.chromadmx.ui.theme.DmxSurface

/**
 * Horizontal scrollable row of preset thumbnails.
 *
 * Each thumbnail shows a mini pixel-art stage preview.
 * Supports tap to apply, long-press to preview, and swipe up to expand.
 */
@Composable
fun ScenePresetRow(
    presets: List<ScenePreset>,
    activePreset: String? = null,
    fixtures: List<Fixture3D>,
    effectRegistry: EffectRegistry,
    onPresetTap: (String) -> Unit,
    onPresetLongPress: (String?) -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -30f) {
                        onSwipeUp()
                    }
                }
            },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetThumbnailItem(
                preset = preset,
                isActive = preset.id == activePreset,
                fixtures = fixtures,
                effectRegistry = effectRegistry,
                onTap = { onPresetTap(preset.id) },
                onLongPress = { onPresetLongPress(preset.id) },
                onRelease = { onPresetLongPress(null) }
            )
        }
    }
}

@Composable
fun PresetThumbnailItem(
    preset: ScenePreset,
    isActive: Boolean,
    fixtures: List<Fixture3D>,
    effectRegistry: EffectRegistry,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DmxSurface)
                .then(
                    if (isActive) Modifier.border(2.dp, DmxPrimary, RoundedCornerShape(4.dp))
                    else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() },
                        onPress = {
                            try {
                                awaitRelease()
                            } finally {
                                onRelease()
                            }
                        }
                    )
                }
        ) {
            PresetThumbnail(
                preset = preset,
                fixtures = fixtures,
                effectRegistry = effectRegistry
            )

            // Active pixel glow
            if (isActive) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = DmxPrimary.copy(alpha = 0.2f),
                        topLeft = Offset(0f, 0f),
                        size = size
                    )
                }
            }
        }

        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) DmxPrimary else Color.Gray,
            maxLines = 1,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun PresetThumbnail(
    preset: ScenePreset,
    fixtures: List<Fixture3D>,
    effectRegistry: EffectRegistry,
    modifier: Modifier = Modifier,
) {
    val thumbnailStack = remember(preset) {
        EffectStack().apply {
            replaceLayers(preset.layers.mapNotNull { config ->
                val effect = effectRegistry.get(config.effectId) ?: return@mapNotNull null
                EffectLayer(
                    effect = effect,
                    params = config.params,
                    blendMode = config.blendMode,
                    opacity = config.opacity
                )
            })
            masterDimmer = preset.masterDimmer
        }
    }

    Canvas(modifier = modifier.fillMaxSize().padding(4.dp)) {
        if (fixtures.isEmpty()) {
            // Draw a generic pixel-art pattern if no fixtures
            val hash = preset.name.hashCode()
            for (i in 0..4) {
                for (j in 0..4) {
                    if ((hash shr (i * 5 + j)) and 1 == 1) {
                         drawRect(
                            color = DmxPrimary.copy(alpha = 0.6f),
                            topLeft = Offset(i * size.width / 5, j * size.height / 5),
                            size = Size(size.width / 5, size.height / 5)
                        )
                    }
                }
            }
            return@Canvas
        }

        // Simplified rendering: evaluate at 9 grid points
        val gridRes = 3
        for (i in 0 until gridRes) {
            for (j in 0 until gridRes) {
                val pos = com.chromadmx.core.model.Vec3(
                    x = (i - gridRes / 2f) * 2f,
                    y = (j - gridRes / 2f) * 2f,
                    z = 0f
                )
                val color = thumbnailStack.evaluate(pos, 0f, BeatState.IDLE)
                drawRect(
                    color = Color(color.r, color.g, color.b),
                    topLeft = Offset(i * size.width / gridRes, j * size.height / gridRes),
                    size = Size(size.width / gridRes - 1f, size.height / gridRes - 1f)
                )
            }
        }
    }
}

package com.chromadmx.ui.screen.perform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.ui.theme.DmxPrimary
import com.chromadmx.ui.theme.DmxSurfaceVariant

/**
 * Swipe-to-reveal panel for managing effect layers.
 *
 * Provides controls for:
 * - Layer opacity (mini slider)
 * - Enable/disable toggle
 * - Layer reordering (up/down)
 * - Removing layers
 * - Adding new layers
 */
@Composable
fun EffectLayerPanel(
    layers: List<EffectLayer>,
    onOpacityChange: (Int, Float) -> Unit,
    onToggleEnabled: (Int) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onAddLayer: () -> Unit,
    onRemoveLayer: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(DmxSurfaceVariant.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Text(
            text = "EFFECT LAYERS",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Display top layers first (conventional for stack lists)
            val layerCount = layers.size
            items(layerCount, key = { revIdx ->
                val idx = layerCount - 1 - revIdx
                "${idx}_${layers[idx].effect.id}"
            }) { revIdx ->
                val idx = layerCount - 1 - revIdx
                val layer = layers[idx]
                LayerControlItem(
                    index = idx,
                    layer = layer,
                    isTop = idx == layerCount - 1,
                    isBottom = idx == 0,
                    onOpacityChange = { onOpacityChange(idx, it) },
                    onToggleEnabled = { onToggleEnabled(idx) },
                    onMoveUp = { onReorder(idx, idx + 1) },
                    onMoveDown = { onReorder(idx, idx - 1) },
                    onRemove = { onRemoveLayer(idx) }
                )
            }

            if (layers.isEmpty()) {
                item {
                    Text(
                        text = "No layers active",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
        }

        Button(
            onClick = onAddLayer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("+ ADD LAYER", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun LayerControlItem(
    index: Int,
    layer: EffectLayer,
    isTop: Boolean,
    isBottom: Boolean,
    onOpacityChange: (Float) -> Unit,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "L${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DmxPrimary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = layer.effect.name.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = layer.blendMode.name.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = layer.enabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = Modifier.scale(0.6f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DmxPrimary,
                            checkedTrackColor = DmxPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = layer.opacity,
                    onValueChange = onOpacityChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DmxPrimary,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Row(modifier = Modifier.padding(start = 4.dp)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isTop,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("▲", color = if (isTop) Color.DarkGray else Color.White, fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isBottom,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("▼", color = if (isBottom) Color.DarkGray else Color.White, fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("×", color = Color.Red, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

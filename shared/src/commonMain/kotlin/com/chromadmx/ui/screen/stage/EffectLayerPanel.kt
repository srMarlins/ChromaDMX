package com.chromadmx.ui.screen.stage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.BlendMode
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.ui.components.PixelChip
import com.chromadmx.ui.components.PixelDropdown
import com.chromadmx.ui.components.PixelIconButton
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.state.StageEvent
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.theme.PixelShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

// ============================================================================
// EffectLayerPanel — Collapsible panel showing active effect layers.
//
// Sits between the stage canvas and preset strip. Each layer row shows
// enable/disable toggle, effect name, opacity slider, and blend mode chip.
// A selected layer reveals reorder and delete controls.
// ============================================================================

/**
 * Expandable effect layer control panel.
 *
 * @param layers       Current effect layers from [PerformanceState].
 * @param availableEffects Set of registered effect IDs for the "Add Layer" dialog.
 * @param onEvent      Event callback routed to [StageViewModelV2.onEvent].
 * @param modifier     Optional modifier.
 */
@Composable
fun EffectLayerPanel(
    layers: ImmutableList<EffectLayer>,
    availableEffects: ImmutableSet<String>,
    onEvent: (StageEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLayerIndex by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Reset selection if it's out of bounds (must run in SideEffect to avoid state mutation during composition)
    SideEffect {
        if (selectedLayerIndex != null && selectedLayerIndex!! >= layers.size) {
            selectedLayerIndex = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixelDesign.colors.surface.copy(alpha = 0.95f))
            .pixelBorder(
                width = 1.dp,
                color = PixelDesign.colors.outlineVariant,
                pixelSize = 1.dp,
            ),
    ) {
        // ── Header row ─────────────────────────────────────────────
        LayerPanelHeader(
            layerCount = layers.size,
            expanded = expanded,
            onToggleExpanded = { expanded = !expanded },
            onAddLayer = { showAddDialog = true },
        )

        // ── Expandable layer list ──────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
            ) {
                if (layers.isEmpty()) {
                    // Empty state
                    Text(
                        text = "No layers — tap [+] to add",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PixelFontFamily,
                            fontSize = 9.sp,
                        ),
                        color = PixelDesign.colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (layers.size > 4) {
                                    mod
                                        .height(200.dp)
                                        .verticalScroll(rememberScrollState())
                                } else {
                                    mod
                                }
                            },
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        layers.forEachIndexed { index, layer ->
                            LayerRow(
                                layer = layer,
                                index = index,
                                isSelected = selectedLayerIndex == index,
                                onSelect = {
                                    selectedLayerIndex = if (selectedLayerIndex == index) null else index
                                },
                                onToggleEnabled = { onEvent(StageEvent.ToggleLayerEnabled(index)) },
                                onOpacityChanged = { onEvent(StageEvent.SetLayerOpacity(index, it)) },
                                onBlendModeChanged = { onEvent(StageEvent.SetLayerBlendMode(index, it)) },
                            )
                        }
                    }

                    // ── Selected layer actions ─────────────────────────
                    selectedLayerIndex?.let { selIdx ->
                        if (selIdx in layers.indices) {
                            Spacer(Modifier.height(6.dp))
                            LayerActions(
                                layerIndex = selIdx,
                                layerCount = layers.size,
                                onMoveUp = {
                                    if (selIdx > 0) {
                                        onEvent(StageEvent.ReorderLayer(selIdx, selIdx - 1))
                                        selectedLayerIndex = selIdx - 1
                                    }
                                },
                                onMoveDown = {
                                    if (selIdx < layers.size - 1) {
                                        onEvent(StageEvent.ReorderLayer(selIdx, selIdx + 1))
                                        selectedLayerIndex = selIdx + 1
                                    }
                                },
                                onDelete = {
                                    onEvent(StageEvent.RemoveLayer(selIdx))
                                    selectedLayerIndex = null
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Add layer dialog ───────────────────────────────────────────
    if (showAddDialog) {
        AddLayerDialog(
            availableEffects = availableEffects,
            onEffectSelected = { effectId ->
                onEvent(StageEvent.AddLayer)
                // Set the effect on the newly added layer (it'll be at the end)
                val newIndex = layers.size // After AddLayer, this will be the new index
                onEvent(StageEvent.SetEffect(newIndex, effectId))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

// ── Header ─────────────────────────────────────────────────────────────

@Composable
private fun LayerPanelHeader(
    layerCount: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddLayer: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "layerArrow",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Expand/collapse arrow
            Text(
                text = "\u25B8", // Right-pointing triangle
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 12.sp,
                ),
                color = PixelDesign.colors.primary,
                modifier = Modifier.rotate(arrowRotation),
            )
            Text(
                text = "LAYERS ($layerCount)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = PixelDesign.colors.onSurface,
            )
        }

        // Add button
        PixelIconButton(
            onClick = onAddLayer,
            modifier = Modifier.size(28.dp),
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = PixelDesign.colors.primary,
            )
        }
    }
}

// ── Layer Row ──────────────────────────────────────────────────────────

@Composable
private fun LayerRow(
    layer: EffectLayer,
    index: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleEnabled: () -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onBlendModeChanged: (BlendMode) -> Unit,
) {
    val rowBg = if (isSelected) {
        PixelDesign.colors.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    val borderColor = if (isSelected) {
        PixelDesign.colors.primary.copy(alpha = 0.4f)
    } else {
        PixelDesign.colors.outlineVariant.copy(alpha = 0.3f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelBorder(
                width = 1.dp,
                color = borderColor,
                pixelSize = 1.dp,
            )
            .clip(PixelShape(4.dp))
            .background(rowBg)
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        // Top row: enable toggle, name, blend mode chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Enable/disable checkbox toggle
                EnableToggle(
                    enabled = layer.enabled,
                    onClick = onToggleEnabled,
                )

                // Effect name
                Text(
                    text = layer.effect.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                    color = if (layer.enabled) {
                        PixelDesign.colors.onSurface
                    } else {
                        PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Blend mode chip
            BlendModeChip(
                blendMode = layer.blendMode,
                onChanged = onBlendModeChanged,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Bottom row: opacity slider with percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PixelSlider(
                value = layer.opacity,
                onValueChange = onOpacityChanged,
                enabled = layer.enabled,
                accentColor = if (layer.enabled) {
                    PixelDesign.colors.primary
                } else {
                    PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.3f)
                },
                modifier = Modifier.weight(1f).height(24.dp),
            )
            Text(
                text = "${(layer.opacity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 9.sp,
                ),
                color = if (layer.enabled) {
                    PixelDesign.colors.primary
                } else {
                    PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.width(32.dp),
            )
        }
    }
}

// ── Enable/Disable Toggle ──────────────────────────────────────────────

@Composable
private fun EnableToggle(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val checkChar = if (enabled) "\u2611" else "\u2610" // ☑ / ☐
    val color = if (enabled) PixelDesign.colors.primary else PixelDesign.colors.onSurfaceVariant

    Text(
        text = checkChar,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = PixelFontFamily,
            fontSize = 14.sp,
        ),
        color = color,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp),
    )
}

// ── Blend Mode Chip ────────────────────────────────────────────────────

@Composable
private fun BlendModeChip(
    blendMode: BlendMode,
    onChanged: (BlendMode) -> Unit,
) {
    val modes = BlendMode.entries
    val currentIndex = modes.indexOf(blendMode)

    // Cycle through blend modes on tap
    val displayName = when (blendMode) {
        BlendMode.NORMAL -> "Norm"
        BlendMode.ADDITIVE -> "Add"
        BlendMode.MULTIPLY -> "Mul"
        BlendMode.OVERLAY -> "Ovl"
    }

    PixelChip(
        text = displayName,
        selected = blendMode != BlendMode.NORMAL,
        onClick = {
            val nextIndex = (currentIndex + 1) % modes.size
            onChanged(modes[nextIndex])
        },
    )
}

// ── Layer Actions (Move Up/Down/Delete) ────────────────────────────────

@Composable
private fun LayerActions(
    layerIndex: Int,
    layerCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Move Up
        PixelIconButton(
            onClick = onMoveUp,
            enabled = layerIndex > 0,
            modifier = Modifier.size(32.dp),
        ) {
            Text(
                text = "\u2191", // ↑
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp,
                ),
            )
        }

        // Move Down
        PixelIconButton(
            onClick = onMoveDown,
            enabled = layerIndex < layerCount - 1,
            modifier = Modifier.size(32.dp),
        ) {
            Text(
                text = "\u2193", // ↓
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp,
                ),
            )
        }

        // Delete
        PixelIconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
        ) {
            Text(
                text = "\u2715", // ✕
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp,
                ),
                color = PixelDesign.colors.error,
            )
        }
    }
}

// ── Add Layer Dialog ───────────────────────────────────────────────────

@Composable
private fun AddLayerDialog(
    availableEffects: Set<String>,
    onEffectSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val effects = remember(availableEffects) { availableEffects.sorted() }
    var selectedIndex by remember { mutableStateOf(-1) }

    com.chromadmx.ui.components.PixelDialog(
        onDismissRequest = onDismiss,
        title = "ADD LAYER",
        confirmButton = {
            com.chromadmx.ui.components.PixelButton(
                onClick = {
                    val effectId = effects.getOrNull(selectedIndex)
                    if (effectId != null) {
                        onEffectSelected(effectId)
                    }
                },
                enabled = selectedIndex in effects.indices,
            ) {
                Text(
                    text = "ADD",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                )
            }
        },
        dismissButton = {
            com.chromadmx.ui.components.PixelButton(
                onClick = onDismiss,
                variant = com.chromadmx.ui.components.PixelButtonVariant.Secondary,
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                )
            }
        },
    ) {
        if (effects.isEmpty()) {
            Text(
                text = "No effects registered",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 9.sp,
                ),
                color = PixelDesign.colors.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Select an effect:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 9.sp,
                ),
                color = PixelDesign.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            PixelDropdown(
                items = effects,
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
                placeholder = "Choose effect...",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

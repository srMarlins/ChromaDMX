package com.chromadmx.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelShape

// ============================================================================
// PixelContainers.kt
//
// Additional pixel design system components:
//   1. PixelIconButton  — small square icon button with optional glow
//   2. PixelDropdown    — chamfered dropdown selector
//   3. PixelDialog      — centered modal dialog with scale animation
//   4. PixelBottomSheet — slide-up bottom sheet with drag handle
// ============================================================================

// ── 1. PixelIconButton ──────────────────────────────────────────────────────

/**
 * A small square icon button with the pixel-art chamfered style.
 *
 * 36.dp square, [PixelShape] (6.dp chamfer) clip, spring press animation
 * that scales from 0.9 to 1.0. Supports an optional glowing border.
 *
 * @param onClick    Callback invoked on click.
 * @param modifier   Optional [Modifier].
 * @param glowing    When `true`, uses [pixelBorderGlowing]; otherwise [pixelBorder].
 * @param enabled    Controls clickability and visual appearance.
 * @param content    Icon or composable content rendered inside the button.
 */
@Composable
fun PixelIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowing: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonSpring = ChromaAnimations.buttonPress
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = buttonSpring.dampingRatio,
            stiffness = buttonSpring.stiffness,
        ),
        label = "iconBtnScale",
    )

    val bgColor = if (enabled) PixelDesign.colors.surface else PixelDesign.colors.surfaceVariant
    val contentColor = if (enabled) PixelDesign.colors.onSurface else PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.5f)
    val shape = PixelShape(6.dp)

    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .let { mod ->
                if (glowing && enabled) {
                    mod.pixelBorderGlowing(chamfer = 6.dp)
                } else {
                    mod.pixelBorder(chamfer = 6.dp)
                }
            }
            .clip(shape)
            .background(bgColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

// ── 2. PixelDropdown ────────────────────────────────────────────────────────

/**
 * A chamfered dropdown selector with pixel-art styling.
 *
 * Displays a trigger button showing the selected item (or [placeholder])
 * and a down arrow. Clicking expands a chamfered dropdown card underneath.
 * The dropdown is scrollable when more than 5 items are present.
 *
 * @param items          The list of selectable option labels.
 * @param selectedIndex  Index of the currently selected item, or -1 for none.
 * @param onItemSelected Callback with the index of the newly selected item.
 * @param modifier       Optional [Modifier].
 * @param placeholder    Text shown when no item is selected.
 */
@Composable
fun PixelDropdown(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select...",
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedIndex in items.indices) items[selectedIndex] else placeholder

    Box(modifier = modifier) {
        // Trigger button
        Row(
            modifier = Modifier
                .pixelBorder(chamfer = 6.dp)
                .clip(PixelShape.Small)
                .background(PixelDesign.colors.surface, PixelShape.Small)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = PixelDesign.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Expand",
                tint = PixelDesign.colors.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }

        // Dropdown menu
        MaterialTheme(
            // Pass through current shapes/colors so DropdownMenu inherits correctly
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = PixelDesign.colors.surface,
                onSurface = PixelDesign.colors.onSurface,
            ),
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .pixelBorderGlowing(chamfer = 9.dp)
                    .clip(PixelShape.Large)
                    .background(PixelDesign.colors.surface, PixelShape.Large)
                    .heightIn(max = 220.dp),
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val itemBg = if (isSelected) {
                        PixelDesign.colors.primary.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) {
                                    PixelDesign.colors.primary
                                } else {
                                    PixelDesign.colors.onSurface
                                },
                            )
                        },
                        onClick = {
                            onItemSelected(index)
                            expanded = false
                        },
                        modifier = Modifier.background(itemBg),
                    )
                }
            }
        }
    }
}

// ── 3. PixelDialog ──────────────────────────────────────────────────────────

/**
 * A pixel-art styled modal dialog.
 *
 * Centered chamfered card with a glowing border. Enters and exits with a
 * scale animation (0.8 to 1.0) using [ChromaAnimations.cardExpand].
 * A 60% black scrim is drawn behind the dialog.
 *
 * @param onDismissRequest Called when the user taps outside or presses back.
 * @param title            Optional uppercase title displayed at the top.
 * @param confirmButton    Primary action button composable.
 * @param dismissButton    Optional secondary/cancel button composable.
 * @param content          Body content slot.
 */
@Composable
fun PixelDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // Scale-in animation
    var appeared by remember { mutableStateOf(false) }
    val cardExpandSpring = ChromaAnimations.cardExpand
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = cardExpandSpring.dampingRatio,
            stiffness = cardExpandSpring.stiffness,
        ),
        label = "dialogScale",
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        // Trigger appear animation on first composition
        androidx.compose.runtime.LaunchedEffect(Unit) { appeared = true }

        // Scrim is provided by Dialog composable, but we ensure our card scales
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelDesign.colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Dialog card — catch clicks so they don't dismiss
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .fillMaxWidth(0.85f)
                    .pixelBorderGlowing(chamfer = 9.dp)
                    .clip(PixelShape.Large)
                    .background(PixelDesign.colors.surface, PixelShape.Large)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume click */ },
                    )
                    .padding(20.dp),
            ) {
                // Title
                if (title != null) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = PixelDesign.colors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Content
                content()

                Spacer(modifier = Modifier.height(16.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissButton != null) {
                        Box(modifier = Modifier.weight(1f)) { dismissButton() }
                    }
                    Box(modifier = Modifier.weight(1f)) { confirmButton() }
                }
            }
        }
    }
}

// ── 4. PixelBottomSheet ─────────────────────────────────────────────────────

/**
 * A shape that chamfers only the top-left and top-right corners,
 * leaving the bottom corners square. Used for bottom sheets.
 */
private class TopChamferShape(private val chamferPx: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val c = chamferPx.coerceAtMost(minOf(size.width, size.height) / 2f)
        val path = Path().apply {
            // Start at top-left chamfer
            moveTo(c, 0f)
            // Top edge
            lineTo(size.width - c, 0f)
            // Top-right chamfer
            lineTo(size.width, c)
            // Right edge (straight down to bottom-right — no chamfer)
            lineTo(size.width, size.height)
            // Bottom edge
            lineTo(0f, size.height)
            // Left edge (straight up to top-left chamfer)
            lineTo(0f, c)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A pixel-art styled bottom sheet with slide-up animation.
 *
 * Slides in from the bottom using [ChromaAnimations.panelSlide].
 * Has chamfered top corners, a pixel border on the top edge, and a
 * centered drag handle. A 40% black scrim appears behind the sheet.
 *
 * @param visible   Controls visibility with enter/exit animations.
 * @param onDismiss Called when the scrim is tapped.
 * @param modifier  Optional [Modifier] applied to the sheet content column.
 * @param content   Sheet body content provided in a [ColumnScope].
 */
@Composable
fun PixelBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val panelSpring = ChromaAnimations.panelSlide

    // Scrim
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = spring(
                dampingRatio = panelSpring.dampingRatio,
                stiffness = panelSpring.stiffness,
            ),
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = spring(
                dampingRatio = panelSpring.dampingRatio,
                stiffness = panelSpring.stiffness,
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelDesign.colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }

    // Sheet
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = panelSpring.dampingRatio,
                    stiffness = panelSpring.stiffness,
                ),
                expandFrom = Alignment.Top, // Expand downward from preset bar
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = panelSpring.dampingRatio,
                    stiffness = panelSpring.stiffness,
                ),
                shrinkTowards = Alignment.Top, // Collapse upward toward preset bar
            ),
        ) {
            val chamferPx = with(androidx.compose.ui.platform.LocalDensity.current) { 9.dp.toPx() }
            val topChamferShape = remember(chamferPx) { TopChamferShape(chamferPx = chamferPx) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelBorder(chamfer = 9.dp)
                    .clip(topChamferShape)
                    .background(PixelDesign.colors.surface, topChamferShape)
                    // Consume clicks so they don't go through to scrim
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume */ },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Drag handle
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(PixelShape.Small)
                        .background(
                            PixelDesign.colors.onSurface.copy(alpha = 0.3f),
                            PixelShape.Small,
                        ),
                )
                Spacer(modifier = Modifier.height(12.dp))

                // User content
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    content = content,
                )
            }
        }
    }
}

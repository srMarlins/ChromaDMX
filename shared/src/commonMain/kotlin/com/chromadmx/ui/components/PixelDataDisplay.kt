package com.chromadmx.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.theme.PixelShape
import kotlinx.coroutines.delay

// ── PixelListItem ────────────────────────────────────────────────────

/**
 * A pixel-styled list item row with optional leading icon, title, subtitle,
 * and trailing content. Tappable with press feedback (scale 0.98).
 *
 * @param title       Primary text displayed in `titleMedium` style.
 * @param subtitle    Optional secondary text in `bodySmall` at 70% alpha.
 * @param onClick     Optional click handler — enables press animation when set.
 * @param leadingIcon Optional composable displayed before the title column.
 * @param trailingContent Optional composable displayed at the end of the row.
 * @param showDivider Whether to display a [PixelDivider] below this item.
 * @param modifier    Modifier applied to the root layout.
 */
@Composable
fun PixelListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonSpring = ChromaAnimations.buttonPress
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = buttonSpring.dampingRatio,
            stiffness = buttonSpring.stiffness,
        )
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .let { mod ->
                    if (onClick != null) {
                        mod.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                            role = androidx.compose.ui.semantics.Role.Button,
                        )
                    } else {
                        mod
                    }
                }
                .padding(
                    horizontal = PixelDesign.spacing.medium,
                    vertical = PixelDesign.spacing.small,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(PixelDesign.spacing.small))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = PixelDesign.colors.onSurface,
                        fontFamily = PixelFontFamily,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PixelDesign.colors.onSurface.copy(alpha = 0.7f),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(PixelDesign.spacing.small))
                trailingContent()
            }
        }

        if (showDivider) {
            PixelDivider()
        }
    }
}

// ── PixelChip ────────────────────────────────────────────────────────

/**
 * A chamfered pixel-styled chip with selected/unselected states.
 *
 * Selected state uses primary background + glowing border + onPrimary text.
 * Unselected state uses transparent background + standard border + onSurface text.
 *
 * @param text     Label text displayed in `labelMedium` UPPERCASE.
 * @param selected Whether the chip is in the selected state.
 * @param onClick  Click handler for toggling the chip.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun PixelChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonSpring = ChromaAnimations.buttonPress
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = buttonSpring.dampingRatio,
            stiffness = buttonSpring.stiffness,
        )
    )

    val chipShape = PixelShape(6.dp)
    val backgroundColor = if (selected) PixelDesign.colors.primary else Color.Transparent
    val contentColor = if (selected) PixelDesign.colors.onPrimary else PixelDesign.colors.onSurface

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(chipShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button,
            )
            .let { mod ->
                if (selected) {
                    mod.pixelBorderGlowing(chamfer = 6.dp)
                } else {
                    mod.pixelBorder(chamfer = 6.dp)
                }
            }
            .background(backgroundColor, chipShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = contentColor,
                fontFamily = PixelFontFamily,
            ),
        )
    }
}

// ── PixelTable ───────────────────────────────────────────────────────

/**
 * A chamfered pixel-styled data table with header and alternating row colors.
 *
 * @param headers       Column header labels (displayed in UPPERCASE `labelMedium`).
 * @param rows          Data rows — each inner list must match [headers] in length.
 * @param modifier      Modifier applied to the outer container.
 * @param columnWeights Optional column weights — defaults to equal distribution.
 */
@Composable
fun PixelTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    columnWeights: List<Float>? = null,
) {
    val weights = columnWeights ?: List(headers.size) { 1f }

    Column(
        modifier = modifier
            .clip(PixelShape.Large)
            .pixelBorder(chamfer = 9.dp)
            .background(PixelDesign.colors.surface, PixelShape.Large),
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelDesign.colors.primary.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            headers.forEachIndexed { index, header ->
                Text(
                    text = header.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = PixelDesign.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PixelFontFamily,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weights.getOrElse(index) { 1f }),
                )
            }
        }

        // Data rows
        rows.forEachIndexed { rowIndex, row ->
            val rowBackground = if (rowIndex % 2 == 0) {
                PixelDesign.colors.surface
            } else {
                PixelDesign.colors.surfaceVariant
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEachIndexed { colIndex, cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PixelDesign.colors.onSurface,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(weights.getOrElse(colIndex) { 1f }),
                    )
                }
            }
        }
    }
}

// ── PixelToast / PixelSnackbar ───────────────────────────────────────

/**
 * A pixel-styled toast notification that slides in from the bottom and
 * auto-dismisses after 3 seconds.
 *
 * @param visible     Whether the toast is currently shown.
 * @param onDismiss   Called when the toast should be hidden (after timeout or action).
 * @param message     The message text to display.
 * @param icon        Optional leading icon.
 * @param actionLabel Optional action button label.
 * @param onAction    Called when the action button is tapped.
 * @param modifier    Modifier applied to the outer container.
 */
@Composable
fun PixelToast(
    visible: Boolean,
    onDismiss: () -> Unit,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    // Auto-dismiss after 3 seconds
    if (visible) {
        LaunchedEffect(message) {
            delay(3000L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = ChromaAnimations.panelSlide.dampingRatio,
                stiffness = ChromaAnimations.panelSlide.stiffness,
            ),
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = ChromaAnimations.panelSlide.dampingRatio,
                stiffness = ChromaAnimations.panelSlide.stiffness,
            ),
        ),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelDesign.spacing.medium)
                .clip(PixelShape.Large)
                .pixelBorder(chamfer = 9.dp)
                .background(PixelDesign.colors.surface, PixelShape.Large)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PixelDesign.colors.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(PixelDesign.spacing.small))
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PixelDesign.colors.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.width(PixelDesign.spacing.small))
                    Text(
                        text = actionLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PixelDesign.colors.primary,
                            fontFamily = PixelFontFamily,
                        ),
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    onAction()
                                    onDismiss()
                                },
                                role = androidx.compose.ui.semantics.Role.Button,
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Alias for [PixelToast] — provides a snackbar-style API with the same
 * pixel-art appearance and behavior.
 */
@Composable
fun PixelSnackbar(
    visible: Boolean,
    onDismiss: () -> Unit,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) = PixelToast(
    visible = visible,
    onDismiss = onDismiss,
    message = message,
    modifier = modifier,
    icon = icon,
    actionLabel = actionLabel,
    onAction = onAction,
)

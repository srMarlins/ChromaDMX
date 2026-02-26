package com.chromadmx.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign

/**
 * Unicode decorator constants for the pixel typography system.
 *
 * These provide consistent visual prefixes and markers across
 * all text-based UI elements in the ChromaDMX design system.
 */
object PixelDecorators {
    /** Screen titles: `▸ STAGE VIEW` */
    const val SECTION_PREFIX = "\u25B8 "

    /** Descriptions under titles: `└─ subtitle text` */
    const val SUB_PREFIX = "\u2514\u2500 "

    /** Input field labels: `✦ IP ADDRESS` */
    const val LABEL_PREFIX = "\u2726 "

    /** List items: `▪ item text` */
    const val BULLET = "\u25AA "

    /** Blinking cursor after headers */
    const val CURSOR = "\u2588"
}

/**
 * A pixel-styled section title with optional description and blinking cursor.
 *
 * Layout:
 * ```
 * ▸ TITLE    [optional badge]    █  (blinking cursor)
 * └─ description text at 70% alpha
 * ```
 *
 * @param title The section title text (automatically uppercased).
 * @param description Optional subtitle displayed below the title at 70% alpha.
 * @param badge Optional composable content displayed after the title (e.g., a [PixelBadge]).
 * @param showCursor Whether to display the blinking block cursor after the title.
 */
@Composable
fun PixelSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    badge: @Composable (() -> Unit)? = null,
    showCursor: Boolean = true,
) {
    val colors = PixelDesign.colors
    val reduceMotion = PixelDesign.reduceMotion

    // Blinking cursor alpha animation
    val cursorAlpha = if (showCursor) {
        if (reduceMotion) {
            ChromaAnimations.Reduced.STATIC_CURSOR_ALPHA
        } else {
            val config = ChromaAnimations.cursorBlink
            val transition = rememberInfiniteTransition(label = "cursorBlink")
            val alpha by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = config.durationMillis,
                        easing = config.easing,
                    ),
                    repeatMode = config.repeatMode,
                ),
                label = "cursorAlpha",
            )
            alpha
        }
    } else {
        0f
    }

    Column(modifier = modifier) {
        // Title row: prefix + title + badge + cursor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = "${PixelDecorators.SECTION_PREFIX}${title.uppercase()}",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
            )

            if (badge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                badge()
            }

            if (showCursor) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = PixelDecorators.CURSOR,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface.copy(alpha = cursorAlpha),
                )
            }
        }

        // Description line
        if (description != null) {
            Text(
                text = "${PixelDecorators.SUB_PREFIX}$description",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * A pixel-styled form field label.
 *
 * Renders as: `✦ LABEL TEXT` using the primary color.
 *
 * @param text The label text (automatically uppercased).
 * @param modifier Modifier applied to the [Text] composable.
 */
@Composable
fun PixelLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${PixelDecorators.LABEL_PREFIX}${text.uppercase()}",
        style = MaterialTheme.typography.labelMedium,
        color = PixelDesign.colors.primary,
        modifier = modifier,
    )
}

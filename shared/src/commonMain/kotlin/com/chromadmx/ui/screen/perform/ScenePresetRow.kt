package com.chromadmx.ui.screen.perform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromadmx.agent.scene.ScenePreset

/**
 * Horizontal row of scene preset buttons.
 * Displays preset thumbnails and names.
 */
@Composable
fun ScenePresetRow(
    presets: List<String>,
    activePreset: String? = null,
    onPresetTap: (String) -> Unit,
    getPresetThumbnailColors: (String) -> List<String>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(presets) { name ->
            val isActive = name == activePreset
            val thumbnailColors = getPresetThumbnailColors(name)

            Column(
                modifier = Modifier
                    .width(80.dp)
                    .clickable { onPresetTap(name) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PresetThumbnail(
                    colors = thumbnailColors,
                    isActive = isActive
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PresetThumbnail(
    colors: List<String>,
    isActive: Boolean,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isActive) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                }
            )
    ) {
        if (colors.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                colors.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(parseComposeColor(hex))
                    )
                }
            }
        }
    }
}

private fun parseComposeColor(hex: String): Color {
    return try {
        val s = hex.removePrefix("#")
        if (s.length == 6) {
            Color(
                red = s.substring(0, 2).toInt(16) / 255f,
                green = s.substring(2, 4).toInt(16) / 255f,
                blue = s.substring(4, 6).toInt(16) / 255f,
                alpha = 1f
            )
        } else if (s.length == 3) {
            Color(
                red = s.substring(0, 1).repeat(2).toInt(16) / 255f,
                green = s.substring(1, 2).repeat(2).toInt(16) / 255f,
                blue = s.substring(2, 3).repeat(2).toInt(16) / 255f,
                alpha = 1f
            )
        } else {
            Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

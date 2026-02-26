package com.chromadmx.ui.screen.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.viewmodel.GenreOption

/**
 * Genre / mood selection screen during onboarding.
 *
 * Presents genres in a 2-column grid. Each tile has a pixel-styled border
 * and representative accent color. Tapping selects the genre and advances.
 *
 * A "Skip" button is available at the bottom for users who want to
 * go straight to the stage.
 *
 * @param genres Available genre options.
 * @param selectedGenre Currently selected genre (null if none).
 * @param onSelectGenre Called when a genre tile is tapped.
 * @param onConfirm Called when the user confirms their selection.
 * @param onSkip Called when the user taps "Skip".
 * @param isGenerating Whether scene generation is in progress.
 * @param generationProgress Progress of scene generation (0f..1f).
 * @param generationError Error message from generation, if any.
 */
@Composable
fun VibeCheckScreen(
    genres: List<GenreOption>,
    selectedGenre: GenreOption?,
    onSelectGenre: (GenreOption) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false,
    generationProgress: Float = 0f,
    generationError: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "VIBE CHECK",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = PixelFontFamily),
            color = NeonCyan,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "What's tonight's vibe?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Genre grid (2 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(genres) { genre ->
                GenreTile(
                    genre = genre,
                    isSelected = genre == selectedGenre,
                    onClick = { onSelectGenre(genre) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Generation progress indicator
        if (isGenerating) {
            Text(
                text = "GENERATING SCENES...",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
                color = NeonCyan,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { generationProgress },
                modifier = Modifier.fillMaxWidth(),
                color = NeonCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Error message
        if (generationError != null) {
            Text(
                text = generationError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Confirm button (visible when a genre is selected and not generating)
        if (selectedGenre != null && !isGenerating) {
            PixelButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(selectedGenre.color.toInt()),
                contentColor = Color.Black,
            ) {
                Text("LET'S GO - ${selectedGenre.displayName.uppercase()}")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Skip button
        PixelButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Text("SKIP")
        }
    }
}

/**
 * A single genre tile in the selection grid.
 */
@Composable
private fun GenreTile(
    genre: GenreOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = Color(genre.color.toInt())
    val borderColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.2f)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "genre-scale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick)
            .pixelBorder(color = borderColor, pixelSize = 3.dp)
            .background(bgColor)
            .padding(3.dp) // border inset
            .padding(vertical = 20.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Color accent bar at top
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .background(accentColor),
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = genre.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

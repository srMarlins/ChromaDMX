package com.chromadmx.ui.screen.chat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonMagenta

/**
 * Horizontally scrollable row of quick action buttons at the top of the chat panel.
 *
 * Provides one-tap shortcuts:
 * - **Generate scenes**: opens the inline pre-generation panel.
 * - **Diagnose network**: sends a diagnostic prompt to the agent.
 * - **Suggest effects**: asks the agent for effect recommendations.
 *
 * Styled with small [PixelButton] components in different accent colors.
 */
@Composable
fun QuickActionBar(
    onGenerateScenes: () -> Unit,
    onDiagnoseNetwork: () -> Unit,
    onSuggestEffects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PixelButton(
            onClick = onGenerateScenes,
            backgroundColor = NeonMagenta.copy(alpha = 0.7f),
            contentColor = Color.White,
            borderColor = NeonMagenta,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Generate scenes",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        PixelButton(
            onClick = onDiagnoseNetwork,
            backgroundColor = NeonCyan.copy(alpha = 0.7f),
            contentColor = Color.Black,
            borderColor = NeonCyan,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Diagnose network",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        PixelButton(
            onClick = onSuggestEffects,
            backgroundColor = NeonGreen.copy(alpha = 0.7f),
            contentColor = Color.Black,
            borderColor = NeonGreen,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Suggest effects",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

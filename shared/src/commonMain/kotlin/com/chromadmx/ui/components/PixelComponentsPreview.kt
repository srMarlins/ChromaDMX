package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.pixelGrid

@Composable
fun PixelComponentsPreview() {
    ChromaDmxTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pixelGrid()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Pixel Art Design System",
                style = MaterialTheme.typography.headlineLarge,
                color = NeonCyan
            )

            // Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PixelButton", style = MaterialTheme.typography.titleLarge, color = NeonMagenta)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(onClick = {}) { Text("PRIMARY") }
                    PixelButton(
                        onClick = {},
                        backgroundColor = MaterialTheme.colorScheme.secondary
                    ) { Text("SECONDARY") }
                }
            }

            // Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PixelCard", style = MaterialTheme.typography.titleLarge, color = NeonMagenta)
                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    // glowColor removed
                ) {
                    Column {
                        Text("Card with Glow", style = MaterialTheme.typography.titleMedium)
                        Text("This card has a subtle neon glow effect.")
                    }
                }
            }

            // Sliders
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PixelSlider", style = MaterialTheme.typography.titleLarge, color = NeonMagenta)
                var sliderValue by remember { mutableStateOf(0.5f) }
                PixelSlider(value = sliderValue, onValueChange = { sliderValue = it })
                Text("Value: ${(sliderValue * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }

            // Badges & Icons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PixelBadge & Icon", style = MaterialTheme.typography.titleLarge, color = NeonMagenta)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PixelBadge(text = "LIVE")
                    PixelBadge(text = "SIMULATION", containerColor = Color.Gray)
                    PixelIcon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color.Red)
                    PixelIcon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan)
                }
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PixelProgressBar", style = MaterialTheme.typography.titleLarge, color = NeonMagenta)
                PixelProgressBar(progress = 0.7f)
            }

            // Divider
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PixelDivider", style = MaterialTheme.typography.titleLarge, color = NeonMagenta)
                PixelDivider()
                Spacer(Modifier.height(8.dp))
                PixelDivider(stepped = false)
            }
        }
    }
}

package com.chromadmx.ui.screen.stage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.ui.components.AudienceView
import com.chromadmx.ui.components.PresetStrip
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.NeonYellow
import com.chromadmx.ui.theme.NodeOnline
import com.chromadmx.ui.theme.NodeWarning
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.viewmodel.StageViewModel

/**
 * Main stage preview screen -- the primary screen users interact with.
 *
 * Features a dual-view stage preview (top-down grid and front-facing audience view)
 * with swipe toggle, enhanced top bar with BPM/dimmer/settings, bottom preset strip,
 * and fixture selection overlay.
 */
@Composable
fun StagePreviewScreen(
    viewModel: StageViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixtures by viewModel.fixtures.collectAsState()
    val fixtureColors by viewModel.fixtureColors.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val masterDimmer by viewModel.masterDimmer.collectAsState()
    val selectedFixtureIndex by viewModel.selectedFixtureIndex.collectAsState()
    val scenes by viewModel.allScenes.collectAsState()
    val activeSceneName by viewModel.activeSceneName.collectAsState()
    val isTopDownView by viewModel.isTopDownView.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = if (isTopDownView) 0 else 1,
        pageCount = { 2 },
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF060612))) {
        // --- Main canvas area (dual view with swipe) ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> VenueCanvas(
                    fixtures = fixtures,
                    fixtureColors = fixtureColors,
                    selectedFixtureIndex = selectedFixtureIndex,
                    onFixtureTapped = { index -> viewModel.selectFixture(index) },
                    onBackgroundTapped = { viewModel.selectFixture(null) },
                    modifier = Modifier.fillMaxSize(),
                )
                1 -> AudienceView(
                    fixtures = fixtures,
                    fixtureColors = fixtureColors,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // --- Top bar overlay with gradient fade ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC060612),
                                Color(0x66060612),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: BPM display
                    BpmDisplayCompact(bpm = bpm)

                    // Center: Master dimmer compact slider
                    MasterDimmerCompact(
                        value = masterDimmer,
                        onValueChange = { viewModel.setMasterDimmer(it) },
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )

                    // Right: Network health indicator + settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NetworkHealthIndicator()
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        // --- View mode indicator (two dots) ---
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ViewModeIndicatorDot(isActive = pagerState.currentPage == 0, label = "TOP")
            ViewModeIndicatorDot(isActive = pagerState.currentPage == 1, label = "FRONT")
        }

        // --- Bottom preset strip ---
        PresetStrip(
            scenes = scenes,
            activeSceneName = activeSceneName,
            onSceneTap = { name -> viewModel.applyScene(name) },
            onSceneLongPress = { name -> viewModel.previewScene(name) },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )

        // --- Fixture selection overlay ---
        AnimatedVisibility(
            visible = selectedFixtureIndex != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp), // Above the preset strip
        ) {
            selectedFixtureIndex?.let { index ->
                val fixture = fixtures.getOrNull(index)
                if (fixture != null) {
                    FixtureInfoOverlay(
                        fixture = fixture,
                        fixtureIndex = index,
                        color = fixtureColors.getOrNull(index),
                        onDismiss = { viewModel.selectFixture(null) },
                    )
                }
            }
        }
    }
}

/**
 * Compact BPM display for the top bar.
 */
@Composable
private fun BpmDisplayCompact(bpm: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${bpm.toInt()}",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = PixelFontFamily,
                fontSize = 18.sp,
            ),
            color = NeonCyan,
        )
        Text(
            text = "BPM",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 8.sp,
            ),
            color = NeonCyan.copy(alpha = 0.6f),
        )
    }
}

/**
 * Compact master dimmer slider for the top bar.
 */
@Composable
private fun MasterDimmerCompact(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "DIM",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 7.sp,
            ),
            color = NeonMagenta.copy(alpha = 0.6f),
        )
        PixelSlider(
            value = value,
            onValueChange = onValueChange,
            accentColor = NeonMagenta,
            modifier = Modifier.weight(1f).height(24.dp),
        )
        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 8.sp,
            ),
            color = NeonMagenta.copy(alpha = 0.8f),
        )
    }
}

/**
 * Simple network health indicator — a small colored dot.
 * Green = connected, yellow = degraded, red = offline.
 * For now, defaults to green (connected state).
 */
@Composable
private fun NetworkHealthIndicator() {
    // TODO: Wire to actual network health status from NetworkModule
    val healthColor = NodeOnline

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(healthColor),
    )
}

/**
 * Small dot indicator showing which view mode is active.
 */
@Composable
private fun ViewModeIndicatorDot(isActive: Boolean, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    if (isActive) NeonCyan else Color.White.copy(alpha = 0.2f),
                ),
        )
        if (isActive) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 6.sp,
                ),
                color = NeonCyan.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * Overlay card showing fixture info when a fixture is selected.
 */
@Composable
private fun FixtureInfoOverlay(
    fixture: com.chromadmx.core.model.Fixture3D,
    fixtureIndex: Int,
    color: com.chromadmx.core.model.Color?,
    onDismiss: () -> Unit,
) {
    val profile = BuiltInProfiles.findById(fixture.fixture.profileId)
    val profileName = profile?.name ?: "Unknown"
    val displayColor = color ?: com.chromadmx.core.model.Color.BLACK

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pixelBorder(width = 2.dp, color = NeonCyan.copy(alpha = 0.5f), pixelSize = 2.dp)
            .background(Color(0xDD0A0A1E))
            .padding(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fixture.fixture.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 12.sp,
                    ),
                    color = NeonCyan,
                )
                Text(
                    text = "#${fixtureIndex + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                    color = Color.White.copy(alpha = 0.4f),
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Profile type
                InfoLabel("TYPE", profileName)

                // DMX address
                InfoLabel("ADDR", "U${fixture.fixture.universeId}/${fixture.fixture.channelStart}")

                // Current color as RGB
                InfoLabel(
                    "COLOR",
                    "R${(displayColor.r * 255).toInt()} G${(displayColor.g * 255).toInt()} B${(displayColor.b * 255).toInt()}",
                )
            }
        }
    }
}

/**
 * Small label + value pair for the fixture info overlay.
 */
@Composable
private fun InfoLabel(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 6.sp,
            ),
            color = NeonYellow.copy(alpha = 0.5f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 9.sp,
            ),
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

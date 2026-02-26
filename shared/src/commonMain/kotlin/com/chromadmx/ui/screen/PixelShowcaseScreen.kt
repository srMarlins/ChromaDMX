package com.chromadmx.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.components.BlinkingCursor
import com.chromadmx.ui.components.PixelBadge
import com.chromadmx.ui.components.PixelBadgeVariant
import com.chromadmx.ui.components.PixelBottomSheet
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelChip
import com.chromadmx.ui.components.PixelDialog
import com.chromadmx.ui.components.PixelDivider
import com.chromadmx.ui.components.PixelDropdown
import com.chromadmx.ui.components.PixelEnchantedDivider
import com.chromadmx.ui.components.PixelIconButton
import com.chromadmx.ui.components.PixelListItem
import com.chromadmx.ui.components.PixelLoadingSpinner
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.components.PixelScaffold
import com.chromadmx.ui.components.PixelSectionTitle
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.PixelSparkles
import com.chromadmx.ui.components.PixelStar
import com.chromadmx.ui.components.PixelSwitch
import com.chromadmx.ui.components.PixelTable
import com.chromadmx.ui.components.PixelTextField
import com.chromadmx.ui.components.PixelToast
import com.chromadmx.ui.components.SpinnerSize
import com.chromadmx.ui.theme.ChromaDmxTheme
import com.chromadmx.ui.theme.PixelColorTheme
import com.chromadmx.ui.theme.PixelDesign

/**
 * A comprehensive component showcase screen for the ChromaDMX pixel design system.
 *
 * Renders every pixel design system component in every relevant state.
 * Intended for debug builds and developer reference. Includes a live
 * theme switcher at the top that wraps all content in [ChromaDmxTheme].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelShowcaseScreen() {
    var selectedTheme by remember { mutableStateOf(PixelColorTheme.MatchaDark) }

    ChromaDmxTheme(colorTheme = selectedTheme) {
        // Dialog / bottom-sheet / toast visibility state
        var showDialog by remember { mutableStateOf(false) }
        var showBottomSheet by remember { mutableStateOf(false) }
        var showToast by remember { mutableStateOf(false) }

        // Form input state
        var textFieldEmpty by remember { mutableStateOf("") }
        var textFieldFilled by remember { mutableStateOf("Hello, pixel world!") }
        var switchChecked by remember { mutableStateOf(true) }
        var switchUnchecked by remember { mutableStateOf(false) }
        var sliderValue by remember { mutableStateOf(0.65f) }
        var dropdownIndex by remember { mutableStateOf(1) }

        // Chip state
        var chipSelected by remember { mutableStateOf(true) }
        var chipUnselected by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            PixelScaffold { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // ── 1. Theme Switcher ────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Theme Switcher",
                            description = "Switch between color themes",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PixelChip(
                                text = "DARK",
                                selected = selectedTheme == PixelColorTheme.MatchaDark,
                                onClick = { selectedTheme = PixelColorTheme.MatchaDark },
                            )
                            PixelChip(
                                text = "LIGHT",
                                selected = selectedTheme == PixelColorTheme.MatchaLight,
                                onClick = { selectedTheme = PixelColorTheme.MatchaLight },
                            )
                            PixelChip(
                                text = "HIGH CONTRAST",
                                selected = selectedTheme == PixelColorTheme.HighContrast,
                                onClick = { selectedTheme = PixelColorTheme.HighContrast },
                            )
                        }
                    }

                    // ── 2. Typography Scale ──────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Typography",
                            description = "All typography styles from the design system",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            @Composable
                            fun TypoSample(name: String, style: androidx.compose.ui.text.TextStyle) {
                                Text(
                                    text = name,
                                    style = style,
                                    color = PixelDesign.colors.onSurface,
                                )
                            }
                            TypoSample("headlineLarge", MaterialTheme.typography.headlineLarge)
                            TypoSample("headlineMedium", MaterialTheme.typography.headlineMedium)
                            TypoSample("headlineSmall", MaterialTheme.typography.headlineSmall)
                            TypoSample("titleLarge", MaterialTheme.typography.titleLarge)
                            TypoSample("titleMedium", MaterialTheme.typography.titleMedium)
                            TypoSample("titleSmall", MaterialTheme.typography.titleSmall)
                            TypoSample("bodyLarge", MaterialTheme.typography.bodyLarge)
                            TypoSample("bodyMedium", MaterialTheme.typography.bodyMedium)
                            TypoSample("bodySmall", MaterialTheme.typography.bodySmall)
                            TypoSample("labelLarge", MaterialTheme.typography.labelLarge)
                            TypoSample("labelMedium", MaterialTheme.typography.labelMedium)
                            TypoSample("labelSmall", MaterialTheme.typography.labelSmall)
                        }
                    }

                    // ── 3. Buttons ───────────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Buttons",
                            description = "All button variants and states",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // All variants enabled
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PixelButtonVariant.entries.forEach { variant ->
                                    PixelButton(
                                        onClick = {},
                                        variant = variant,
                                    ) {
                                        Text(variant.name)
                                    }
                                }
                            }

                            // Disabled button
                            PixelButton(
                                onClick = {},
                                enabled = false,
                            ) {
                                Text("Disabled")
                            }

                            // Icon buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PixelIconButton(onClick = {}) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Star",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                PixelIconButton(
                                    onClick = {},
                                    glowing = true,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Favorite",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ── 4. Form Inputs ───────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Form Inputs",
                            description = "Text fields, switches, sliders, dropdowns",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Text field — empty with placeholder
                            PixelTextField(
                                value = textFieldEmpty,
                                onValueChange = { textFieldEmpty = it },
                                label = "Empty Field",
                                placeholder = "Enter some text...",
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // Text field — filled with value
                            PixelTextField(
                                value = textFieldFilled,
                                onValueChange = { textFieldFilled = it },
                                label = "Filled Field",
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // Switches
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Checked:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelDesign.colors.onSurface,
                                )
                                PixelSwitch(
                                    checked = switchChecked,
                                    onCheckedChange = { switchChecked = it },
                                )
                                Text(
                                    text = "Unchecked:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelDesign.colors.onSurface,
                                )
                                PixelSwitch(
                                    checked = switchUnchecked,
                                    onCheckedChange = { switchUnchecked = it },
                                )
                            }

                            // Slider with value label
                            PixelSlider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                showValueLabel = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // Dropdown
                            PixelDropdown(
                                items = listOf("RGB Par", "Moving Head", "Pixel Bar", "Strobe"),
                                selectedIndex = dropdownIndex,
                                onItemSelected = { dropdownIndex = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // ── 5. Cards ─────────────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Cards",
                            description = "Static, glowing, and clickable variants",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Static card
                            PixelCard(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Static Card",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelDesign.colors.onSurface,
                                )
                            }

                            // Glowing card
                            PixelCard(
                                modifier = Modifier.fillMaxWidth(),
                                glowing = true,
                            ) {
                                Text(
                                    text = "Glowing Card",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelDesign.colors.onSurface,
                                )
                            }

                            // Clickable card
                            PixelCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {},
                            ) {
                                Text(
                                    text = "Clickable Card (press me)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelDesign.colors.onSurface,
                                )
                            }
                        }
                    }

                    // ── 6. Badges ────────────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Badges",
                            description = "All badge variants",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PixelBadgeVariant.entries.forEach { variant ->
                                PixelBadge(
                                    text = variant.name,
                                    variant = variant,
                                )
                            }
                        }
                    }

                    // ── 7. Data Display ──────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Data Display",
                            description = "List items, chips, and tables",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // PixelListItem with icon, title, subtitle
                            PixelListItem(
                                title = "Fixture #1",
                                subtitle = "RGB Par, Universe 1, Ch 1-3",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = PixelDesign.colors.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                            )

                            // Chips — selected and unselected
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PixelChip(
                                    text = "Selected",
                                    selected = chipSelected,
                                    onClick = { chipSelected = !chipSelected },
                                )
                                PixelChip(
                                    text = "Unselected",
                                    selected = chipUnselected,
                                    onClick = { chipUnselected = !chipUnselected },
                                )
                            }

                            // PixelTable — 3 columns, 3 rows
                            PixelTable(
                                headers = listOf("Fixture", "Universe", "Channel"),
                                rows = listOf(
                                    listOf("RGB Par", "1", "1-3"),
                                    listOf("Moving Head", "1", "4-20"),
                                    listOf("Pixel Bar", "2", "1-48"),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // ── 8. Feedback ──────────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Feedback",
                            description = "Progress bars and loading spinners",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Determinate progress at 65%
                            Text(
                                text = "Determinate (65%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            PixelProgressBar(
                                progress = 0.65f,
                                showPercentage = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // Indeterminate progress
                            Text(
                                text = "Indeterminate",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            PixelProgressBar(
                                progress = 0f,
                                indeterminate = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // Loading spinners — all 3 sizes
                            Text(
                                text = "Loading Spinners",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SpinnerSize.entries.forEach { size ->
                                    PixelLoadingSpinner(spinnerSize = size)
                                }
                            }
                        }
                    }

                    // ── 9. Decorative ────────────────────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Decorative",
                            description = "Stars, sparkles, dividers, and cursors",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Stars in a row
                            Text(
                                text = "PixelStar",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PixelStar(size = 12.dp)
                                PixelStar(size = 16.dp)
                                PixelStar(size = 24.dp)
                                PixelStar(
                                    size = 20.dp,
                                    color = PixelDesign.colors.secondary,
                                )
                            }

                            // Sparkles in a fixed-size box
                            Text(
                                text = "PixelSparkles",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            PixelSparkles(containerSize = 64.dp)

                            // Dividers
                            Text(
                                text = "PixelDivider (standard/stepped)",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            PixelDivider()

                            Text(
                                text = "PixelDivider (flat)",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            PixelDivider(stepped = false)

                            Text(
                                text = "PixelEnchantedDivider",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            PixelEnchantedDivider()

                            // Blinking cursor
                            Text(
                                text = "BlinkingCursor",
                                style = MaterialTheme.typography.labelMedium,
                                color = PixelDesign.colors.onSurface,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Type here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PixelDesign.colors.onSurface,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                BlinkingCursor()
                            }
                        }
                    }

                    // ── 10. Dialogs (trigger buttons) ────────────────────
                    item {
                        PixelSectionTitle(
                            title = "Dialogs & Overlays",
                            description = "Trigger buttons for modal components",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PixelButton(
                                onClick = { showDialog = true },
                                variant = PixelButtonVariant.Primary,
                            ) {
                                Text("Show Dialog")
                            }
                            PixelButton(
                                onClick = { showBottomSheet = true },
                                variant = PixelButtonVariant.Secondary,
                            ) {
                                Text("Show Bottom Sheet")
                            }
                            PixelButton(
                                onClick = { showToast = true },
                                variant = PixelButtonVariant.Surface,
                            ) {
                                Text("Show Toast")
                            }
                        }

                        // Bottom spacer for scroll clearance
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // ── Overlay components (rendered outside LazyColumn) ─────

            // Dialog
            if (showDialog) {
                PixelDialog(
                    onDismissRequest = { showDialog = false },
                    title = "Sample Dialog",
                    confirmButton = {
                        PixelButton(
                            onClick = { showDialog = false },
                            variant = PixelButtonVariant.Primary,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        PixelButton(
                            onClick = { showDialog = false },
                            variant = PixelButtonVariant.Surface,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("Cancel")
                        }
                    },
                ) {
                    Text(
                        text = "This is a pixel-styled dialog with chamfered corners and a glowing border.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelDesign.colors.onSurface,
                    )
                }
            }

            // Bottom Sheet
            PixelBottomSheet(
                visible = showBottomSheet,
                onDismiss = { showBottomSheet = false },
            ) {
                PixelSectionTitle(
                    title = "Bottom Sheet",
                    description = "Slides up from the bottom",
                    showCursor = false,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This is a pixel-styled bottom sheet with chamfered top corners and a drag handle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelDesign.colors.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PixelButton(
                    onClick = { showBottomSheet = false },
                    variant = PixelButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Dismiss")
                }
            }

            // Toast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                PixelToast(
                    visible = showToast,
                    onDismiss = { showToast = false },
                    message = "This is a pixel-styled toast notification!",
                    icon = Icons.Filled.Info,
                    actionLabel = "Undo",
                    onAction = { /* no-op for showcase */ },
                )
            }
        }
    }
}

package com.chromadmx.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.Channel
import com.chromadmx.core.model.ChannelType
import com.chromadmx.core.model.FixtureProfile
import com.chromadmx.core.model.FixtureType
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.ui.components.PixelBadge
import com.chromadmx.ui.components.PixelBadgeVariant
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelDropdown
import com.chromadmx.ui.components.PixelDialog
import com.chromadmx.ui.components.PixelIconButton
import com.chromadmx.ui.components.PixelScaffold
import com.chromadmx.ui.components.PixelSectionTitle
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.PixelSwitch
import com.chromadmx.ui.components.PixelTextField
import com.chromadmx.ui.state.AgentStatus
import com.chromadmx.ui.state.DataTransferStatus
import com.chromadmx.ui.state.ProtocolType
import com.chromadmx.ui.state.SettingsEvent
import com.chromadmx.ui.state.SettingsUiState
import com.chromadmx.ui.theme.PixelColorTheme
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.toColors
import com.chromadmx.ui.util.presetDisplayName
import com.chromadmx.ui.viewmodel.SettingsViewModelV2

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModelV2,
    onBack: () -> Unit,
    onProvisioning: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    PixelScaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelIconButton(onClick = onBack) {
                    Text(
                        text = "\u25C0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PixelDesign.colors.onSurface,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PixelDesign.colors.onBackground,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.large),
        ) {
            // Appearance section (theme picker)
            item { AppearanceSection(state.themePreference, viewModel::onEvent) }
            // Network section
            item { NetworkSection(state, viewModel::onEvent) }
            // Simulation section
            item { SimulationSection(state, viewModel::onEvent) }
            // Fixture Profiles section
            item { FixtureProfilesSection(state, viewModel::onEvent) }
            // Agent section
            item { AgentSection(state, viewModel::onEvent) }
            // Hardware section
            item { HardwareSection(onProvisioning) }
            // App section (destructive)
            item {
                AppSection(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onShowResetDialog = { showResetDialog = true },
                )
            }
            // Version footer
            item { VersionFooter() }
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        PixelDialog(
            onDismissRequest = { showResetDialog = false },
            title = "Reset Onboarding?",
            confirmButton = {
                PixelButton(
                    onClick = {
                        viewModel.onEvent(SettingsEvent.ResetOnboarding)
                        showResetDialog = false
                    },
                    variant = PixelButtonVariant.Danger,
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                PixelButton(
                    onClick = { showResetDialog = false },
                    variant = PixelButtonVariant.Secondary,
                ) {
                    Text("Cancel")
                }
            },
        ) {
            Text(
                text = "This will restart the setup wizard.",
                color = PixelDesign.colors.onSurface,
            )
        }
    }
}

// ── Appearance Section ─────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    selectedTheme: PixelColorTheme,
    onEvent: (SettingsEvent) -> Unit,
) {
    PixelCard(
        title = { PixelSectionTitle(title = "Appearance") },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
        ) {
            PixelColorTheme.entries.forEach { theme ->
                ThemeChip(
                    theme = theme,
                    isSelected = theme == selectedTheme,
                    onClick = { onEvent(SettingsEvent.SetThemePreference(theme)) },
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    theme: PixelColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = theme.toColors()
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isSelected) PixelDesign.colors.primary else PixelDesign.colors.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Color swatch: background + primary + secondary in horizontal thirds
        androidx.compose.material3.Surface(
            modifier = Modifier.size(72.dp, 48.dp),
            shape = shape,
            border = BorderStroke(borderWidth, borderColor),
            color = colors.background,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colors.background),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colors.primary),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colors.secondary),
                )
            }
        }
        Text(
            text = theme.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) PixelDesign.colors.primary else PixelDesign.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private fun PixelColorTheme.displayName(): String = when (this) {
    PixelColorTheme.MatchaDark -> "Matcha Dark"
    PixelColorTheme.MatchaLight -> "Matcha Light"
    PixelColorTheme.HighContrast -> "High Contrast"
    PixelColorTheme.NeonCyberpunk -> "Cyberpunk"
    PixelColorTheme.OceanDepths -> "Ocean"
    PixelColorTheme.SunsetWarm -> "Sunset"
    PixelColorTheme.MonochromePro -> "Mono Pro"
}

// ── Network Section ────────────────────────────────────────────────────

@Composable
private fun NetworkSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val protocolItems = ProtocolType.entries.map { it.name }
    val selectedProtocolIndex = ProtocolType.entries.indexOf(state.protocol)

    PixelCard(
        title = { PixelSectionTitle(title = "Network") },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
        ) {
            // Protocol selector
            Text(
                text = "Protocol",
                style = MaterialTheme.typography.labelLarge,
                color = PixelDesign.colors.onSurfaceVariant,
            )
            PixelDropdown(
                items = protocolItems,
                selectedIndex = selectedProtocolIndex,
                onItemSelected = { index ->
                    onEvent(SettingsEvent.SetProtocol(ProtocolType.entries[index]))
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Poll interval slider
            Text(
                text = "Poll Interval: ${state.pollInterval}ms",
                style = MaterialTheme.typography.labelLarge,
                color = PixelDesign.colors.onSurfaceVariant,
            )
            PixelSlider(
                value = state.pollInterval.toFloat(),
                onValueChange = { onEvent(SettingsEvent.SetPollInterval(it.toLong())) },
                valueRange = 1000f..10000f,
                modifier = Modifier.fillMaxWidth(),
                showValueLabel = true,
                valueLabelFormatter = { "${it.toLong()}ms" },
            )

            // Manual IP
            PixelTextField(
                value = state.manualIp,
                onValueChange = { onEvent(SettingsEvent.SetManualIp(it)) },
                label = "Node IP Address",
                modifier = Modifier.fillMaxWidth(),
            )

            // Manual universe and start address in a row
            Row(
                horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
            ) {
                PixelTextField(
                    value = state.manualUniverse,
                    onValueChange = { onEvent(SettingsEvent.SetManualUniverse(it)) },
                    label = "Universe",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PixelTextField(
                    value = state.manualStartAddress,
                    onValueChange = { onEvent(SettingsEvent.SetManualStartAddress(it)) },
                    label = "Start Address",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            // Force Rescan button
            PixelButton(
                onClick = { onEvent(SettingsEvent.ForceRescan) },
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Force Rescan")
            }
        }
    }
}

// ── Simulation Section ─────────────────────────────────────────────────

@Composable
private fun SimulationSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val rigPresetItems = RigPreset.entries.map { it.presetDisplayName() }
    val selectedPresetIndex = RigPreset.entries.indexOf(state.selectedRigPreset)

    PixelCard(
        title = { PixelSectionTitle(title = "Simulation") },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
        ) {
            // Simulation toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Simulation Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PixelDesign.colors.onSurface,
                )
                PixelSwitch(
                    checked = state.simulationEnabled,
                    onCheckedChange = { onEvent(SettingsEvent.ToggleSimulation(it)) },
                )
            }

            // Rig preset dropdown (only when simulation enabled)
            if (state.simulationEnabled) {
                Text(
                    text = "Rig Preset",
                    style = MaterialTheme.typography.labelLarge,
                    color = PixelDesign.colors.onSurfaceVariant,
                )
                PixelDropdown(
                    items = rigPresetItems,
                    selectedIndex = selectedPresetIndex,
                    onItemSelected = { index ->
                        onEvent(SettingsEvent.SetRigPreset(RigPreset.entries[index]))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Reset Simulation button
                PixelButton(
                    onClick = { onEvent(SettingsEvent.ResetSimulation) },
                    variant = PixelButtonVariant.Danger,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset Simulation")
                }
            }
        }
    }
}

// ── Fixture Profiles Section ───────────────────────────────────────────

/** Set of built-in profile IDs that cannot be deleted. */
private val builtInProfileIds: Set<String> = BuiltInProfiles.all().map { it.profileId }.toSet()

@Composable
private fun FixtureProfilesSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<FixtureProfile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    PixelCard(
        title = { PixelSectionTitle(title = "Fixture Profiles") },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
        ) {
            if (state.fixtureProfiles.isEmpty()) {
                Text(
                    text = "No profiles loaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelDesign.colors.onSurfaceVariant,
                )
            } else {
                state.fixtureProfiles.forEach { profile ->
                    val isBuiltIn = profile.profileId in builtInProfileIds
                    FixtureProfileRow(
                        profile = profile,
                        isBuiltIn = isBuiltIn,
                        onEdit = { editingProfile = profile },
                        onDelete = { showDeleteConfirm = profile.profileId },
                    )
                }
            }

            Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

            // Add Profile button
            PixelButton(
                onClick = { showAddDialog = true },
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("+ Add Profile")
            }
        }
    }

    // Add profile dialog
    if (showAddDialog) {
        FixtureProfileDialog(
            existingProfile = null,
            onSave = { profile ->
                onEvent(SettingsEvent.AddFixtureProfile(profile))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    // Edit profile dialog
    editingProfile?.let { profile ->
        FixtureProfileDialog(
            existingProfile = profile,
            onSave = { updated ->
                onEvent(SettingsEvent.UpdateFixtureProfile(updated))
                editingProfile = null
            },
            onDismiss = { editingProfile = null },
        )
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { profileId ->
        PixelDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = "Delete Profile?",
            confirmButton = {
                PixelButton(
                    onClick = {
                        onEvent(SettingsEvent.DeleteFixtureProfile(profileId))
                        showDeleteConfirm = null
                    },
                    variant = PixelButtonVariant.Danger,
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                PixelButton(
                    onClick = { showDeleteConfirm = null },
                    variant = PixelButtonVariant.Secondary,
                ) {
                    Text("Cancel")
                }
            },
        ) {
            Text(
                text = "This profile will be removed from your library.",
                color = PixelDesign.colors.onSurface,
            )
        }
    }
}

@Composable
private fun FixtureProfileRow(
    profile: FixtureProfile,
    isBuiltIn: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
    ) {
        // Lock icon for built-in profiles
        if (isBuiltIn) {
            Text(
                text = "\uD83D\uDD12",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Profile name
        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyLarge,
            color = PixelDesign.colors.onSurface,
            modifier = Modifier.weight(1f),
        )

        // Channel count badge
        PixelBadge(
            text = "${profile.channelCount}ch",
            variant = PixelBadgeVariant.Info,
        )

        // Edit button
        PixelIconButton(onClick = onEdit) {
            Text(
                text = "\u270E",
                style = MaterialTheme.typography.bodySmall,
                color = PixelDesign.colors.onSurface,
            )
        }

        // Delete button (only for user-created profiles)
        if (!isBuiltIn) {
            PixelIconButton(onClick = onDelete) {
                Text(
                    text = "\u2716",
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelDesign.colors.error,
                )
            }
        }
    }
}

/**
 * Dialog for adding or editing a fixture profile.
 *
 * When [existingProfile] is non-null the dialog operates in edit mode,
 * pre-filling the fields. Otherwise it creates a new profile.
 */
@Composable
private fun FixtureProfileDialog(
    existingProfile: FixtureProfile?,
    onSave: (FixtureProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEdit = existingProfile != null
    val fixtureTypes = FixtureType.entries
    val fixtureTypeNames = fixtureTypes.map { it.name.replace('_', ' ') }

    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var selectedTypeIndex by remember {
        mutableStateOf(
            existingProfile?.let { fixtureTypes.indexOf(it.type) } ?: 0
        )
    }
    var channelCountText by remember {
        mutableStateOf(existingProfile?.channelCount?.toString() ?: "3")
    }

    PixelDialog(
        onDismissRequest = onDismiss,
        title = if (isEdit) "Edit Profile" else "Add Profile",
        confirmButton = {
            PixelButton(
                onClick = {
                    val chCount = channelCountText.toIntOrNull() ?: 3
                    val selectedType = fixtureTypes[selectedTypeIndex]
                    val channels = buildDefaultChannels(selectedType, chCount)
                    val profileId = existingProfile?.profileId
                        ?: "user-${name.lowercase().replace(' ', '-')}-${chCount}ch"
                    val profile = existingProfile?.copy(
                        name = name,
                        type = selectedType,
                        channels = channels,
                    ) ?: FixtureProfile(
                        profileId = profileId,
                        name = name,
                        type = selectedType,
                        channels = channels,
                    )
                    onSave(profile)
                },
                variant = PixelButtonVariant.Primary,
                enabled = name.isNotBlank() && (channelCountText.toIntOrNull() ?: 0) > 0,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            PixelButton(
                onClick = onDismiss,
                variant = PixelButtonVariant.Secondary,
            ) {
                Text("Cancel")
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
        ) {
            // Name field
            PixelTextField(
                value = name,
                onValueChange = { name = it },
                label = "Profile Name",
                modifier = Modifier.fillMaxWidth(),
            )

            // Type dropdown
            Text(
                text = "Fixture Type",
                style = MaterialTheme.typography.labelLarge,
                color = PixelDesign.colors.onSurfaceVariant,
            )
            PixelDropdown(
                items = fixtureTypeNames,
                selectedIndex = selectedTypeIndex,
                onItemSelected = { selectedTypeIndex = it },
                modifier = Modifier.fillMaxWidth(),
            )

            // Channel count
            PixelTextField(
                value = channelCountText,
                onValueChange = { channelCountText = it },
                label = "Channel Count",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

/**
 * Generates a default channel list for a given fixture type and channel count.
 * Provides sensible defaults so users get a working profile out of the box.
 */
private fun buildDefaultChannels(type: FixtureType, count: Int): List<Channel> {
    val base = when (type) {
        FixtureType.PAR, FixtureType.WASH -> listOf(
            Channel("Red", ChannelType.RED, 0),
            Channel("Green", ChannelType.GREEN, 1),
            Channel("Blue", ChannelType.BLUE, 2),
        )
        FixtureType.MOVING_HEAD -> listOf(
            Channel("Pan", ChannelType.PAN, 0),
            Channel("Tilt", ChannelType.TILT, 1),
            Channel("Dimmer", ChannelType.DIMMER, 2),
            Channel("Red", ChannelType.RED, 3),
            Channel("Green", ChannelType.GREEN, 4),
            Channel("Blue", ChannelType.BLUE, 5),
        )
        FixtureType.PIXEL_BAR -> {
            val pixelCount = (count / 3).coerceAtLeast(1)
            return (0 until pixelCount).flatMap { px ->
                listOf(
                    Channel("Px${px}_R", ChannelType.RED, px * 3),
                    Channel("Px${px}_G", ChannelType.GREEN, px * 3 + 1),
                    Channel("Px${px}_B", ChannelType.BLUE, px * 3 + 2),
                )
            }
        }
        FixtureType.STROBE -> listOf(
            Channel("Dimmer", ChannelType.DIMMER, 0),
            Channel("Strobe", ChannelType.STROBE, 1),
        )
        else -> listOf(
            Channel("Dimmer", ChannelType.DIMMER, 0),
        )
    }
    // Pad with generic channels if the user asked for more than the template provides
    val extra = (base.size until count).map { offset ->
        Channel("Ch${offset + 1}", ChannelType.GENERIC, offset)
    }
    return base.take(count) + extra
}

// ── Agent Section ──────────────────────────────────────────────────────

@Composable
private fun AgentSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val modelItems = SettingsViewModelV2.AVAILABLE_MODELS
    val selectedModelIndex = modelItems.indexOf(state.agentConfig.modelId)

    PixelCard(
        title = { PixelSectionTitle(title = "AI Agent") },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
        ) {
            // API Key (masked display — auto-unmask on edit)
            var showKey by remember { mutableStateOf(false) }
            val displayValue = if (showKey || state.agentConfig.apiKey.isEmpty()) {
                state.agentConfig.apiKey
            } else {
                "\u2022".repeat(state.agentConfig.apiKey.length.coerceAtMost(20))
            }
            PixelTextField(
                value = displayValue,
                onValueChange = { newValue ->
                    if (!showKey) {
                        // First keystroke while masked: unmask and start fresh
                        showKey = true
                        onEvent(SettingsEvent.UpdateAgentConfig(state.agentConfig.copy(apiKey = newValue.filter { it != '\u2022' })))
                    } else {
                        onEvent(SettingsEvent.UpdateAgentConfig(state.agentConfig.copy(apiKey = newValue)))
                    }
                },
                label = "API Key",
                placeholder = "Enter API key...",
                modifier = Modifier.fillMaxWidth(),
            )

            // Model selector
            Text(
                text = "Model",
                style = MaterialTheme.typography.labelLarge,
                color = PixelDesign.colors.onSurfaceVariant,
            )
            PixelDropdown(
                items = modelItems,
                selectedIndex = selectedModelIndex.coerceAtLeast(0),
                onItemSelected = { index ->
                    onEvent(
                        SettingsEvent.UpdateAgentConfig(
                            state.agentConfig.copy(modelId = modelItems[index])
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Max iterations
            PixelTextField(
                value = state.agentConfig.maxIterations.toString(),
                onValueChange = { value ->
                    val iterations = value.toIntOrNull() ?: return@PixelTextField
                    onEvent(
                        SettingsEvent.UpdateAgentConfig(
                            state.agentConfig.copy(maxIterations = iterations)
                        )
                    )
                },
                label = "Max Iterations",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            // Test Connection button + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelButton(
                    onClick = { onEvent(SettingsEvent.TestAgentConnection) },
                    variant = PixelButtonVariant.Primary,
                ) {
                    Text("Test Connection")
                }

                AgentStatusBadge(state.agentStatus)
            }
        }
    }
}

@Composable
private fun AgentStatusBadge(status: AgentStatus) {
    when (status) {
        is AgentStatus.Idle -> { /* No badge shown */ }
        is AgentStatus.Testing -> {
            PixelBadge(text = "Testing...", variant = PixelBadgeVariant.Info)
        }
        is AgentStatus.Success -> {
            PixelBadge(text = "Connected", variant = PixelBadgeVariant.Primary)
        }
        is AgentStatus.Error -> {
            PixelBadge(text = status.message, variant = PixelBadgeVariant.Error)
        }
    }
}

// ── Hardware Section ───────────────────────────────────────────────────

@Composable
private fun HardwareSection(onProvisioning: () -> Unit) {
    PixelCard(
        title = { PixelSectionTitle(title = "Hardware") },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
        ) {
            PixelButton(
                onClick = onProvisioning,
                variant = PixelButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("BLE Provisioning")
            }
        }
    }
}

// ── App Section ────────────────────────────────────────────────────────

@Composable
private fun AppSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onShowResetDialog: () -> Unit,
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    val isTransferring = state.dataTransferStatus is DataTransferStatus.InProgress

    PixelCard(
        title = { PixelSectionTitle(title = "App") },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
        ) {
            // Reset Onboarding (destructive — shows confirmation)
            PixelButton(
                onClick = onShowResetDialog,
                variant = PixelButtonVariant.Danger,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset Onboarding")
            }

            // Export / Import
            Row(
                horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
            ) {
                PixelButton(
                    onClick = { onEvent(SettingsEvent.ExportAppData) },
                    variant = PixelButtonVariant.Secondary,
                    enabled = !isTransferring,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isTransferring) "Exporting..." else "Export Data")
                }
                PixelButton(
                    onClick = { showImportDialog = true },
                    variant = PixelButtonVariant.Secondary,
                    enabled = !isTransferring,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isTransferring) "Importing..." else "Import Data")
                }
            }

            // Status messages
            when (val status = state.dataTransferStatus) {
                is DataTransferStatus.ExportReady -> {
                    Text(
                        text = "Export ready (${status.json.length} chars)",
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelDesign.colors.success,
                    )
                }
                is DataTransferStatus.ImportSuccess -> {
                    Text(
                        text = "Import successful!",
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelDesign.colors.success,
                    )
                }
                is DataTransferStatus.Error -> {
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelDesign.colors.error,
                    )
                }
                else -> {} // Idle or InProgress — no extra text
            }
        }
    }

    // Import dialog
    if (showImportDialog) {
        PixelDialog(
            onDismissRequest = {
                showImportDialog = false
                importText = ""
            },
            title = "Import Data",
            confirmButton = {
                PixelButton(
                    onClick = {
                        if (importText.isNotBlank()) {
                            onEvent(SettingsEvent.ImportAppData(importText))
                        }
                        showImportDialog = false
                        importText = ""
                    },
                    variant = PixelButtonVariant.Primary,
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                PixelButton(
                    onClick = {
                        showImportDialog = false
                        importText = ""
                    },
                    variant = PixelButtonVariant.Secondary,
                ) {
                    Text("Cancel")
                }
            },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PixelDesign.spacing.small),
            ) {
                Text(
                    text = "Paste your exported JSON data below:",
                    style = MaterialTheme.typography.bodySmall,
                )
                PixelTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = "JSON Data",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
        }
    }
}

// ── Version Footer ─────────────────────────────────────────────────────

@Composable
private fun VersionFooter() {
    Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))
    Text(
        text = "Version 1.0.0",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = PixelDesign.colors.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))
}

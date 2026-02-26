package com.chromadmx.ui.screen.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.persistence.FixtureGroup
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelSlider
import com.chromadmx.ui.components.pixelBorder
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily

/** Z-height presets for common fixture mounting positions. */
private data class ZPreset(val label: String, val value: Float)
private val Z_PRESETS = listOf(
    ZPreset("FLOOR", 0f),
    ZPreset("LOW", 1.5f),
    ZPreset("TRUSS", 3f),
    ZPreset("HIGH", 5f),
)

/**
 * Edit overlay shown when a fixture is selected in edit mode.
 *
 * Displays fixture info (read-only), Z-height slider with presets,
 * group assignment dropdown, and test-fire button.
 */
@Composable
fun FixtureEditOverlay(
    fixture: Fixture3D,
    fixtureIndex: Int,
    groups: List<FixtureGroup>,
    onZHeightChanged: (Float) -> Unit,
    onGroupAssigned: (String?) -> Unit,
    onCreateGroup: () -> Unit,
    onTestFire: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    PixelCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        borderColor = PixelDesign.colors.info,
        backgroundColor = PixelDesign.colors.surface
    ) {
        Column {
            // --- Header: Name + DMX address ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = fixture.fixture.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PixelFontFamily,
                            fontSize = 12.sp,
                        ),
                        color = PixelDesign.colors.info,
                    )
                    Text(
                        text = "U${fixture.fixture.universeId}/${fixture.fixture.channelStart} | ${fixture.fixture.profileId}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PixelFontFamily,
                            fontSize = 7.sp,
                        ),
                        color = PixelDesign.colors.onSurfaceVariant,
                    )
                }

                // Close button
                Text(
                    text = "X",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                    color = PixelDesign.colors.error,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // --- Z-Height slider ---
            val zStr = (kotlin.math.round(fixture.position.z * 10f) / 10f).toString()
            Text(
                text = "Z-HEIGHT: ${zStr}m",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PixelFontFamily,
                    fontSize = 7.sp,
                ),
                color = PixelDesign.colors.tertiary.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))

            PixelSlider(
                value = fixture.position.z,
                onValueChange = onZHeightChanged,
                valueRange = 0f..6f,
                accentColor = PixelDesign.colors.tertiary,
                modifier = Modifier.fillMaxWidth().height(28.dp),
            )

            Spacer(Modifier.height(4.dp))

            // Z-height preset buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (preset in Z_PRESETS) {
                    val isActive = kotlin.math.abs(fixture.position.z - preset.value) < 0.1f
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PixelFontFamily,
                            fontSize = 6.sp,
                        ),
                        color = if (isActive) PixelDesign.colors.tertiary else PixelDesign.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { onZHeightChanged(preset.value) }
                            .then(
                                if (isActive) {
                                    Modifier.pixelBorder(
                                        width = 1.dp,
                                        color = PixelDesign.colors.tertiary.copy(alpha = 0.5f),
                                        pixelSize = 1.dp,
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- Group assignment ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "GROUP:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 7.sp,
                    ),
                    color = PixelDesign.colors.info.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(8.dp))

                Box {
                    val currentGroupName = groups.find { it.groupId == fixture.groupId }?.name ?: "None"
                    Text(
                        text = currentGroupName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PixelFontFamily,
                            fontSize = 8.sp,
                        ),
                        color = PixelDesign.colors.onSurface.copy(alpha = 0.9f),
                        modifier = Modifier
                            .clickable { groupDropdownExpanded = true }
                            .pixelBorder(
                                width = 1.dp,
                                color = PixelDesign.colors.info.copy(alpha = 0.3f),
                                pixelSize = 1.dp,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )

                    DropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false },
                        modifier = Modifier.background(PixelDesign.colors.surfaceVariant),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "None",
                                    color = PixelDesign.colors.onSurface.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PixelFontFamily,
                                        fontSize = 8.sp,
                                    ),
                                )
                            },
                            onClick = {
                                onGroupAssigned(null)
                                groupDropdownExpanded = false
                            },
                        )
                        for (group in groups) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(group.color.toInt())),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            group.name,
                                            color = PixelDesign.colors.onSurface.copy(alpha = 0.9f),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = PixelFontFamily,
                                                fontSize = 8.sp,
                                            ),
                                        )
                                    }
                                },
                                onClick = {
                                    onGroupAssigned(group.groupId)
                                    groupDropdownExpanded = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "+ New Group...",
                                    color = PixelDesign.colors.info.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PixelFontFamily,
                                        fontSize = 8.sp,
                                    ),
                                )
                            },
                            onClick = {
                                onCreateGroup()
                                groupDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- Action buttons: Test Fire + Close ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelButton(
                    onClick = onTestFire,
                    backgroundColor = PixelDesign.colors.tertiary,
                    contentColor = PixelDesign.colors.onTertiary,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("TEST FIRE", fontSize = 8.sp)
                }

                PixelButton(
                    onClick = onDismiss,
                    backgroundColor = PixelDesign.colors.surfaceVariant,
                    contentColor = PixelDesign.colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("CLOSE", fontSize = 8.sp)
                }
            }
        }
    }
}

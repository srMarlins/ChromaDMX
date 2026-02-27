package com.chromadmx.ui.components.beat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.BeatState

/**
 * Container composable combining [BpmDisplay], [BeatPhaseIndicator], and [BarPhaseIndicator].
 *
 * Layout: BPM display on the left (tappable for tap-tempo), beat phase bar and
 * bar phase segments stacked on the right.
 *
 * @param beatState   Current [BeatState] snapshot from the tempo module.
 * @param onTapTempo  Called when the user taps the BPM display for tap-tempo input.
 * @param bpmSource   Indicates the source of the BPM (link, tap, idle) for color coding.
 * @param modifier    Compose modifier.
 */
@Composable
fun BeatBar(
    beatState: BeatState,
    onTapTempo: () -> Unit,
    bpmSource: BpmSource = BpmSource.TAP,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left: BPM display (tappable) — clipToBounds prevents border glow overflow
        BpmDisplay(
            bpm = beatState.bpm,
            beatPhase = beatState.beatPhase,
            source = bpmSource,
            onTap = onTapTempo,
            modifier = Modifier.width(120.dp).clipToBounds(),
        )

        // Right: stacked indicators — clipToBounds prevents border glow overflow
        Column(
            modifier = Modifier.weight(1f).clipToBounds(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Beat phase progress bar
            BeatPhaseIndicator(
                beatPhase = beatState.beatPhase,
            )

            // Bar phase: 4-segment beat counter
            BarPhaseIndicator(
                barPhase = beatState.barPhase,
            )
        }
    }
}

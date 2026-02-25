package com.chromadmx.ui.screen.perform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.ui.components.VenueCanvas
import com.chromadmx.ui.theme.StageBackground
import com.chromadmx.core.model.Color as DmxColor

/**
 * Stage preview component that displays the [VenueCanvas] with a beat-reactive top bar.
 */
@Composable
fun StagePreview(
    beatState: BeatState,
    fixtures: List<Fixture3D>,
    fixtureColors: List<DmxColor>,
    onTapTempo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(StageBackground)) {
        // The venue visualization
        VenueCanvas(
            fixtures = fixtures,
            fixtureColors = fixtureColors,
            modifier = Modifier.matchParentSize()
        )

        // The top bar overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BpmDisplay(
                    bpm = beatState.bpm,
                    syncSource = beatState.syncSource,
                    beatPhase = beatState.beatPhase,
                    onClick = onTapTempo
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    BarPhaseIndicator(
                        barPhase = beatState.barPhase,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    BeatPhaseIndicator(
                        beatPhase = beatState.beatPhase,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

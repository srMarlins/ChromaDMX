package com.chromadmx.ui.screen.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.state.SetupEvent
import com.chromadmx.ui.state.UseCase
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Use-case selection step in the onboarding flow.
 *
 * Presents three options that fork the setup experience:
 * - **My Room** — home/desk WLED lighting (defaults to DESK_STRIP rig)
 * - **A Stage** — live performance setup (defaults to SMALL_DJ rig)
 * - **Just Exploring** — simulation mode with ROOM_ACCENT rig
 */
@Composable
internal fun UseCaseSelectionContent(
    onEvent: (SetupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "WHAT ARE YOU LIGHTING?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 2.sp,
            ),
            color = PixelDesign.colors.primary,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        PixelButton(
            onClick = { onEvent(SetupEvent.SelectUseCase(UseCase.MY_ROOM)) },
            variant = PixelButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("MY ROOM")
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        PixelButton(
            onClick = { onEvent(SetupEvent.SelectUseCase(UseCase.A_STAGE)) },
            variant = PixelButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("A STAGE")
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        PixelButton(
            onClick = { onEvent(SetupEvent.SelectUseCase(UseCase.JUST_EXPLORING)) },
            variant = PixelButtonVariant.Surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("JUST EXPLORING")
        }
    }
}

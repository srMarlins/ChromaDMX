package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelShape

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current.copy(color = PixelDesign.colors.onSurface),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = if (enabled) {
        PixelDesign.colors.surfaceVariant
    } else {
        PixelDesign.colors.surfaceVariant.copy(alpha = 0.5f)
    }

    Box(modifier = modifier) {
        // Label with decorative prefix
        if (label != null) {
            Text(
                text = "${PixelDecorators.LABEL_PREFIX}${label.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = PixelDesign.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (label != null) 20.dp else 0.dp)
                .clip(PixelShape.Small)
                .background(backgroundColor, PixelShape.Small)
                .let { mod ->
                    if (isFocused) {
                        mod.pixelBorderActive(chamfer = 6.dp)
                    } else {
                        mod.pixelBorder(chamfer = 6.dp)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(PixelDesign.colors.primary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(
                                color = PixelDesign.colors.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

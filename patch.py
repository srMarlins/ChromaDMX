import re

with open('shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt', 'r') as f:
    content = f.read()

content = content.replace("import androidx.compose.foundation.clickable", "import androidx.compose.foundation.selection.toggleable")

old_modifier = """        .size(width = trackWidth, height = trackHeight)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { onCheckedChange?.invoke(!checked) },
            role = Role.Switch
        )
        .let { mod ->"""

new_modifier = """        .size(width = trackWidth, height = trackHeight)
        .let { baseMod ->
            if (onCheckedChange != null) {
                baseMod.toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
            } else {
                baseMod
            }
        }
        .let { mod ->"""

content = content.replace(old_modifier, new_modifier)

with open('shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt', 'w') as f:
    f.write(content)

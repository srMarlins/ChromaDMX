import re

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt", "r") as f:
    content = f.read()

# add import
if "import androidx.compose.foundation.selection.toggleable" not in content:
    content = content.replace(
        "import androidx.compose.foundation.interaction.MutableInteractionSource",
        "import androidx.compose.foundation.interaction.MutableInteractionSource\nimport androidx.compose.foundation.selection.toggleable"
    )

# replace clickable
old_clickable = """        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { onCheckedChange?.invoke(!checked) },
            role = Role.Switch
        )"""

new_toggleable = """        .let { m ->
            if (onCheckedChange != null) {
                m.toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onValueChange = onCheckedChange,
                    role = Role.Switch
                )
            } else m
        }"""

content = content.replace(old_clickable, new_toggleable)

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt", "w") as f:
    f.write(content)

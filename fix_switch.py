import os

filepath = "shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add import
import_clickable = "import androidx.compose.foundation.clickable"
import_toggleable = "import androidx.compose.foundation.selection.toggleable"

if import_toggleable not in content:
    content = content.replace(import_clickable, import_clickable + "\nimport androidx.compose.foundation.selection.toggleable")

# Replace clickable
old_code = """        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { onCheckedChange?.invoke(!checked) },
            role = Role.Switch
        )"""

new_code = """        .toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onValueChange = { onCheckedChange?.invoke(it) },
            role = Role.Switch
        )"""

content = content.replace(old_code, new_code)

with open(filepath, "w") as f:
    f.write(content)

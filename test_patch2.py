import re

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSlider.kt", "r") as f:
    content = f.read()

# add import
if "import androidx.compose.ui.semantics.semantics" not in content:
    content = content.replace(
        "import androidx.compose.ui.Modifier",
        "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.semantics.semantics\nimport androidx.compose.ui.semantics.contentDescription"
    )

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSlider.kt", "w") as f:
    f.write(content)

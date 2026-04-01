import os
import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Check if the file needs updating (contains PasswordVisualTransformation but no keyboardOptions update)
    if 'PasswordVisualTransformation()' in content and 'keyboardType = KeyboardType.Password' not in content:
        print(f"File {filepath} needs update.")

        if filepath.endswith('ProvisioningScreen.kt'):
            # Add KeyboardType import
            if 'import androidx.compose.ui.text.input.KeyboardType' not in content:
                content = content.replace('import androidx.compose.ui.text.input.PasswordVisualTransformation\n', 'import androidx.compose.ui.text.input.KeyboardType\nimport androidx.compose.ui.text.input.PasswordVisualTransformation\n')

            # Add KeyboardOptions import
            if 'import androidx.compose.foundation.text.KeyboardOptions' not in content:
                content = content.replace('import androidx.compose.foundation.layout.width\n', 'import androidx.compose.foundation.layout.width\nimport androidx.compose.foundation.text.KeyboardOptions\n')

            # Add autoCorrectEnabled import (if needed) - Compose multiplatform doesn't always need it but we pass it as a named arg

            # Update OutlinedTextField for wifiPassword
            pattern = r'''OutlinedTextField\(\s*value = wifiPassword,\s*onValueChange = \{ wifiPassword = it \},\s*label = \{ Text\("Wi-Fi Password"\) \},\s*modifier = Modifier\.fillMaxWidth\(\),\s*visualTransformation = PasswordVisualTransformation\(\),\s*singleLine = true\s*\)'''

            replacement = '''OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        singleLine = true
                    )'''

            content = re.sub(pattern, replacement, content)

        elif filepath.endswith('SettingsScreen.kt'):
            # Update PixelTextField for apiKey

            # Find the visualTransformation part and add keyboardOptions after it
            pattern = r'''(visualTransformation = if \(showKey \|\| state\.agentConfig\.apiKey\.isEmpty\(\)\) \{\s*androidx\.compose\.ui\.text\.input\.VisualTransformation\.None\s*\} else \{\s*androidx\.compose\.ui\.text\.input\.PasswordVisualTransformation\(\)\s*\},\s*)(modifier = Modifier\.fillMaxWidth\(\),)'''

            replacement = r'\1keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),\n                \2'

            content = re.sub(pattern, replacement, content)

        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

update_file('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt')
update_file('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt')

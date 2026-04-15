import sys

filepath = 'shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()

# Add imports
import_search = "import androidx.compose.ui.text.input.PasswordVisualTransformation"
import_replace = """import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType"""

if import_search in content:
    content = content.replace(import_search, import_replace)
    print("Patched imports successfully.")
else:
    print("Could not find import search string.")

# Add keyboardOptions
search = """                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )"""

replace = """                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        singleLine = true
                    )"""

if search in content:
    content = content.replace(search, replace)
    print("Patched OutlinedTextField successfully.")
else:
    print("Could not find OutlinedTextField search string.")

with open(filepath, 'w') as f:
    f.write(content)

## 2024-05-20 - Prevent OS Dictionary Caching for Secrets in Compose
**Vulnerability:** API keys and Wi-Fi passwords in custom text fields were leaking into the OS keyboard dictionary and auto-complete cache because they lacked proper `KeyboardOptions`.
**Learning:** Compose's `PasswordVisualTransformation` only masks the UI output (bullets). It does NOT prevent the underlying OS keyboard from learning the typed characters. Both `visualTransformation` AND `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` are required.
**Prevention:** Always explicitly disable auto-correct and set the keyboard type to password for any text field handling secrets, tokens, or passwords across all platforms.

## 2026-03-01 - [High] Secure Masking of Secrets in Jetpack Compose UI
**Vulnerability:** API keys and sensitive tokens in `SettingsScreen.kt` were manually mutated into strings composed of bullet characters (`\u2022`) to mask them. The underlying logic was vulnerable because it replaced the actual state value and required complicated reconstruction logic on the first keystroke, creating risks of secrets leaking in state updates, logging, or accidentally being saved as dots into storage or the API config.
**Learning:** For masking secrets like API keys in Compose UI state, custom text fields must support Compose's `VisualTransformation` parameter so that the view can mask the output securely without ever changing the underlying raw string state value.
**Prevention:** When building custom TextFields (e.g., `PixelTextField`), always expose the `visualTransformation` parameter and pass it to the internal `BasicTextField`. Always use `PasswordVisualTransformation()` to mask sensitive values instead of manipulating strings.

## 2026-04-03 - [Secure Text Fields]
**Vulnerability:** Sensitive text fields (API Key, Wi-Fi Password) were missing explicit keyboard configuration to disable autocorrect and set the password keyboard type.
**Learning:** Even with `PasswordVisualTransformation()`, if `KeyboardOptions` doesn't explicitly disable autocorrect and set `KeyboardType.Password`, the OS keyboard might cache sensitive inputs in its dictionary or offer them as autocomplete suggestions.
**Prevention:** Always set `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` on text fields handling secrets.

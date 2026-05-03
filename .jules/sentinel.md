## 2026-03-01 - [High] Secure Masking of Secrets in Jetpack Compose UI
**Vulnerability:** API keys and sensitive tokens in `SettingsScreen.kt` were manually mutated into strings composed of bullet characters (`\u2022`) to mask them. The underlying logic was vulnerable because it replaced the actual state value and required complicated reconstruction logic on the first keystroke, creating risks of secrets leaking in state updates, logging, or accidentally being saved as dots into storage or the API config.
**Learning:** For masking secrets like API keys in Compose UI state, custom text fields must support Compose's `VisualTransformation` parameter so that the view can mask the output securely without ever changing the underlying raw string state value.
**Prevention:** When building custom TextFields (e.g., `PixelTextField`), always expose the `visualTransformation` parameter and pass it to the internal `BasicTextField`. Always use `PasswordVisualTransformation()` to mask sensitive values instead of manipulating strings.
## 2026-05-03 - Prevent ADB Backup Extraction
**Vulnerability:** Android apps with `android:allowBackup="true"` (default) allow attackers with physical access or USB debugging to extract sensitive local app data.
**Learning:** This is a common and often overlooked configuration in Android manifests that poses a medium-security risk for apps handling sensitive data.
**Prevention:** Ensure `android:allowBackup="false"` is explicitly set in the `AndroidManifest.xml` to disable backup extraction unless explicitly required for the app's functionality.

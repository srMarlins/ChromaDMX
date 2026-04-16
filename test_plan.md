1. **Fix API Key field vulnerability**
   - Edit `shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt` to add `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to the `PixelTextField` where `API Key` is entered.
   - This prevents the OS dictionary cache and autocomplete from saving or leaking the sensitive API key.

2. **Verify changes**
   - Compile Android Main `:shared:compileAndroidMain`
   - Run linter `:shared:lint`
   - Run UI verification using frontend instructions.

3. **Complete pre-commit steps**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

4. **Submit the PR**
   - Branch: `sentinel-secure-apikey-keyboard`
   - Title: `🛡️ Sentinel: [HIGH] Secure API Key from Keyboard Caching`

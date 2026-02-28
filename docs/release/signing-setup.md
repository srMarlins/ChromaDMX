# ChromaDMX Release Signing Setup

## Android

### Generate a Release Keystore

```bash
keytool -genkeypair \
  -v \
  -storetype JKS \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -alias chromadmx \
  -keystore chromadmx-release.jks \
  -dname "CN=ChromaDMX, O=ChromaDMX, L=Unknown, ST=Unknown, C=US"
```

### Base64 Encode for GitHub Secrets

```bash
base64 -i chromadmx-release.jks -o keystore-base64.txt
# Copy the contents of keystore-base64.txt into the RELEASE_KEYSTORE_BASE64 secret
```

### GitHub Secrets (Android)

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `.jks` keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias (e.g., `chromadmx`) |
| `RELEASE_KEY_PASSWORD` | Key password |
| `GOOGLE_API_KEY` | Google AI API key (optional) |
| `ANTHROPIC_API_KEY` | Anthropic API key (optional) |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Store service account JSON (optional, for auto-upload) |

### Play Store Service Account (Optional)

1. Go to Google Play Console > Setup > API access
2. Create a service account with "Release manager" permissions
3. Download the JSON key file
4. Paste the entire JSON content into `PLAY_SERVICE_ACCOUNT_JSON`

---

## iOS

### App Store Connect API Key

1. Go to [App Store Connect > Users and Access > Integrations > App Store Connect API](https://appstoreconnect.apple.com/access/integrations/api)
2. Generate a new key with "App Manager" role
3. Download the `.p8` private key file (only available once)
4. Note the **Key ID** and **Issuer ID**

```bash
# Base64 encode the .p8 key
base64 -i AuthKey_XXXXXXXXXX.p8
```

### Register App ID

1. Go to [Apple Developer > Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/identifiers/list)
2. Register a new App ID with bundle identifier `com.chromadmx.app`
3. Enable capabilities: Network Extensions (if needed)

### Initialize Fastlane Match

Match stores signing certificates and provisioning profiles in a private git repo.

```bash
# Create a private GitHub repo for certificates (e.g., chromadmx-certs)

cd ios

# Initialize match (first time only, run locally)
bundle exec fastlane match init

# Generate certificates and profiles
bundle exec fastlane match appstore
```

### GitHub Secrets (iOS)

| Secret | Value |
|--------|-------|
| `ASC_KEY_ID` | App Store Connect API key ID |
| `ASC_ISSUER_ID` | App Store Connect issuer ID |
| `ASC_KEY_CONTENT` | Base64-encoded `.p8` private key content |
| `MATCH_GIT_URL` | Private cert storage repo URL (e.g., `https://github.com/srMarlins/chromadmx-certs.git`) |
| `MATCH_GIT_BASIC_AUTH` | Base64-encoded `username:personal_access_token` |
| `MATCH_PASSWORD` | Encryption password for Match cert storage |
| `KEYCHAIN_PASSWORD` | Any random password for the temporary CI keychain |
| `DEVELOPMENT_TEAM` | Apple Developer Team ID (10-character alphanumeric) |

To generate `MATCH_GIT_BASIC_AUTH`:
```bash
echo -n "github_username:ghp_your_personal_access_token" | base64
```

---

## Triggering Releases

### Tag Push (Recommended)

Both Android and iOS release workflows trigger on version tags:

```bash
git tag v0.1.0
git push origin v0.1.0
```

This will:
- Build signed APK + AAB (Android) and IPA (iOS)
- Create a GitHub Release with all artifacts
- Upload AAB to Play Store internal track (if configured)
- Upload IPA to TestFlight (if configured)

### Manual Dispatch

Go to GitHub Actions > select the workflow > "Run workflow":
- **Android Release**: optionally specify a version string
- **iOS Release**: optionally specify a version string

Manual runs create artifacts but do NOT create a GitHub Release.

### Pre-release Tags

Use semver pre-release tags for testing:
```bash
git tag v0.1.0-rc1    # release candidate
git tag v0.1.0-beta1  # beta
```

---

## Local Development

No secrets are needed for local development:
- **Android**: `./gradlew :android:app:assembleDebug` works without signing config
- **iOS**: Local builds use real LinkKit headers (stubs only used in CI with `-Pchromadmx.linkkit.stubs=true`)
- **API keys**: Read from `local.properties` (gitignored)

# ChromaDMX

Mobile-first DMX lighting controller. Kotlin Multiplatform (Android + iOS), Compose Multiplatform UI.

## Build & Test

```bash
./gradlew :shared:<module>:allTests   # modules: core, networking, tempo, engine, simulation, vision, agent
./gradlew :android:app:assembleDebug  # Android APK
```

CI runs: compile → 7 module test suites → APK build.

## Code Patterns

- `expect/actual` for platform abstractions
- Koin Multiplatform for DI — named qualifiers ("real"/"simulated") for transport switching
- `StateFlow` for observable state in ViewModels
- Coroutines for async — never block the main thread
- All dependency versions in `gradle/libs.versions.toml`

## Critical Constraints

- DMX output path must be pure shared Kotlin — no platform imports in `shared/` modules
- Three independent loops (UI/Engine/DMX) must never block each other
- AI agent is optional — app must be fully functional offline
- Effects are 3D spatial functions, not GPU framebuffer samples
- 40Hz DMX output timing must not be blocked

## Workflow

- Feature branches + PRs, never commit directly to main
- Reference GitHub Issues in commits/PRs

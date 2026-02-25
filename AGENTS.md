# ChromaDMX — Agent Instructions

Shared context for all AI agents working on this codebase (Claude, Gemini, Copilot, Jules).

## Project

Mobile-first, agentic DMX lighting controller for live performance.
Kotlin Multiplatform (Android + iOS) with Compose Multiplatform UI.

## Architecture

Full spec: `docs/plans/2026-02-24-chromadmx-architecture-design.md`

### Module Layout

```
shared/
  core/          — DMX primitives, universe, fixtures, color, 3D math
  networking/    — Art-Net 4 (port 6454), sACN (port 5568), discovery
  tempo/         — BPM detection, tap tempo, beat sync
  engine/        — Effect engine, 60fps render loop, triple-buffered output
  simulation/    — Virtual DMX nodes, fixtures, camera for testing
  vision/        — Camera blob detection, fixture mapping
  agent/         — Koog AI agent, 17 tools, ReAct strategy
  ui/            — Compose Multiplatform shared UI
android/app/     — Android shell (Koin DI, Compose Activity)
ios/              — iOS shell (SwiftUI + KMP interop)
build-logic/     — Convention plugins (chromadmx.kmp.library, chromadmx.compose, chromadmx.android.application)
```

### Key Technical Details

- Package root: `com.chromadmx` (sub-packages: `.core`, `.networking`, `.tempo`, `.vision`, `.engine`, `.simulation`, `.agent`)
- Art-Net 4 primary protocol (port 6454), sACN secondary (port 5568)
- Triple buffer with atomicfu for lock-free engine→DMX data flow
- 40Hz DMX output loop, 60fps effect engine loop
- Koog 0.6.3 agent SDK (JetBrains KMP AI framework)

## Code Style

- Kotlin Multiplatform — shared code in `shared/`, platform code in `android/` and `ios/`
- Use `expect/actual` for platform abstractions
- Dependency injection via Koin Multiplatform
- Coroutines for async — never block the main thread
- `StateFlow` for observable state in ViewModels
- No platform dependencies in the DMX output path

## Testing

- Use the simulation layer (`shared/simulation/`) for hardware-free testing
- Unit tests for all shared business logic
- Integration tests using simulated DMX nodes, fixtures, and camera
- Test effects by asserting computed colors at known 3D coordinates
- Build command: `./gradlew :shared:<module>:allTests`
- AGP 9.0 test task: `testAndroidHostTest` (not `testDebugUnitTest`)

## Key Principles

- DMX output path must be pure shared Kotlin — no platform dependencies
- AI agent is a power-up, not a dependency — app must be fully functional offline
- Effects are 3D spatial functions, not GPU framebuffer samples
- Three independent loops (UI/Engine/DMX) must never block each other

## Workflow

- Never commit directly to main — always use feature branches + PRs
- Track work via GitHub Issues — reference issues in commits/PRs
- CI runs on every PR: compile → 7 module test suites → APK build

## Review Focus Areas

When reviewing code, pay attention to:
- Thread safety (coroutine context, shared mutable state)
- Coroutine misuse (blocking main thread, missing cancellation)
- Platform leaks in shared code (no Android/iOS imports in `shared/`)
- Proper Koin DI scoping
- DMX timing constraints (40Hz output must not be blocked)

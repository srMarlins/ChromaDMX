# ChromaDMX

Mobile-first, agentic DMX lighting controller for live performance (KMP: Android + iOS).

## Architecture Reference

Full spec: `docs/plans/2026-02-24-chromadmx-architecture-design.md`

## How To Work

### Workflow
- Use git worktrees for feature work — isolate changes from main
- Use subagents for parallel independent tasks
- Track work via GitHub Issues — create issues before starting features, reference them in commits/PRs
- Never commit directly to main — always use feature branches + PRs

### Code Style
- Kotlin Multiplatform — shared code in `shared/`, platform code in `android/` and `ios/`
- Use `expect/actual` for platform abstractions
- Dependency injection via Koin Multiplatform
- Coroutines for async — never block the main thread
- `StateFlow` for observable state in ViewModels

### Testing
- Use the simulation layer (`shared/simulation/`) for hardware-free testing
- Unit tests for all shared business logic
- Integration tests using simulated DMX nodes, fixtures, and camera
- Test effects by asserting computed colors at known 3D coordinates

### Key Principles
- DMX output path must be pure shared Kotlin — no platform dependencies
- AI agent is a power-up, not a dependency — app must be fully functional offline
- Effects are 3D spatial functions, not GPU framebuffer samples
- Three independent loops (UI/Engine/DMX) must never block each other

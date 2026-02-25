# ChromaDMX GitHub Issue Breakdown

**Date:** 2026-02-24
**Status:** Approved
**Approach:** Module-aligned epics with checklist sub-tasks

---

## Overview

Full project breakdown into 12 epic-level GitHub issues, one per major module/milestone. Each issue contains a checklist of sub-tasks. Issues are ordered by dependency — later issues reference earlier ones as blockers.

## Issue Dependency Graph

```
#1 Scaffolding
 └→ #2 Build Logic
     └→ #3 Core Module
         ├→ #4 Networking
         ├→ #5 Tempo
         ├→ #6 Simulation (also needs #4)
         ├→ #7 Effect Engine (also needs #5)
         ├→ #8 Vision
         ├→ #9 Agent (also needs #4, #5, #7)
         ├→ #10 Android Shell
         └→ #11 iOS Shell
              └→ #12 Compose UI (needs #7, #8, #9, #10)
```

## Issues

### 1. GitHub Repo & Project Scaffolding
- Create private GitHub repo
- Push existing files (CLAUDE.md, .gitignore, docs/)
- Initialize KMP project structure (shared/, android/, ios/, build-logic/)
- Set up Gradle wrapper and version catalog (libs.versions.toml)
- Configure Compose Multiplatform plugin
- Configure Koin Multiplatform dependency
- Verify project builds on Android and iOS (empty shells)

### 2. Gradle Convention Plugins & Build Logic
**Blocked by:** #1
- Create build-logic/ with convention plugins for shared modules
- Android library convention plugin
- iOS framework convention plugin
- Compose Multiplatform convention plugin
- Shared libs.versions.toml entries (coroutines, serialization, ktor, koin, koog)

### 3. Core Module
**Blocked by:** #2
- Color data class (RGB, blending, interpolation, DMX byte conversion)
- Vec3 utility (3D vector math, distance, normalize)
- DMXUniverse model (512 channels, universe ID)
- Fixture, Fixture3D, SpatialCoordinate models
- FixtureProfile and fixture config models
- BlendMode enum and color blending functions
- EffectParams container
- ByteArray extensions for DMX channel packing
- Unit tests for all models and math utilities

### 4. Networking Module
**Blocked by:** #3
- ArtNetCodec — encode/decode ArtDmx, ArtPoll, ArtPollReply packets
- SacnCodec — encode/decode sACN E1.31 data packets
- UdpTransport expect/actual abstraction
- NodeDiscovery — ArtPoll broadcast, reply collection, live device registry
- DmxOutputService — 40Hz broadcast loop, multi-universe frame assembly
- Unit tests for codec (known byte sequences)
- Integration tests with SimulatedDmxNode (after #6)

### 5. Tempo Module
**Blocked by:** #3
- BeatClock interface (bpm, beatPhase, barPhase StateFlows)
- TapTempoClock implementation (tap tempo, phase nudge)
- AbletonLinkClock expect/actual (JNI on Android, cinterop on iOS)
- Spike: Ableton Link SDK version selection and KMP bridging approach
- Unit tests for TapTempoClock BPM calculation
- Integration test: clock drives effect engine at correct phase

### 6. Simulation Module
**Blocked by:** #3, #4
- SimulatedDmxNode — fake Art-Net node on localhost
- SimulatedFixtureRig — preset layouts (small_dj, truss_rig, festival_stage)
- SimulatedCamera — synthetic frames with blobs at fixture positions
- SimulatedNetwork — loopback UDP with configurable packet loss/latency
- Koin module to swap real → simulated implementations
- Tests: simulated node responds to ArtPoll, receives ArtDmx

### 7. Effect Engine Module
**Blocked by:** #3, #5
- SpatialEffect interface (compute at Vec3 + time + beat)
- EffectStack with layer blending and master dimmer
- EffectRegistry — register and look up effects by ID
- Triple-buffered color output (atomicfu buffer swap)
- Engine loop (60fps coroutine on Dispatchers.Default)
- V1 effects: SolidColor, GradientSweep3D, RadialPulse3D, PerlinNoise3D, WaveEffect3D, Chase3D, Strobe, RainbowSweep3D, ParticleBurst3D
- Unit tests: assert computed colors at known coordinates/times
- Performance test: 240 fixtures under 1ms per frame

### 8. Vision Module
**Blocked by:** #3
- CameraSource expect/actual abstraction (grayscale FloatArray output)
- Blob detection algorithm (brightness threshold, connected-component labeling, centroid)
- Ambient baseline subtraction
- Scan orchestrator state machine (test-fire sequence)
- Multi-cell endpoint detection and pixel interpolation
- SpatialMap builder (camera coords → Fixture3D with user Z-height)
- Unit tests with synthetic frames (from SimulatedCamera)

### 9. Agent Module (Koog)
**Blocked by:** #3, #4, #5, #7
- Koog dependency setup and configuration
- System prompt definition
- Scene tools: setEffect, setBlendMode, setMasterDimmer, setColorPalette, setTempoMultiplier, createScene, loadScene
- Network tools: scanNetwork, getNodeStatus, configureNode, diagnoseConnection
- Fixture tools: listFixtures, fireFixture, setFixtureGroup
- State tools: getEngineState, getBeatState, getNetworkState
- Agent session management (conversation history, background coroutine)
- Pre-generation workflow (batch scene creation for offline use)
- Spike: validate Koog tool definition API matches design
- Integration tests with mocked LLM responses

### 10. Android Platform Shell
**Blocked by:** #3
- Android app module with Koin DI setup
- Bottom tab navigation (Perform, Network, Map, Agent)
- CameraX frame capture integration
- PlatformUdpTransport actual (DatagramSocket)
- Ableton Link JNI bridge (actual)
- Network/Wi-Fi permissions handling
- OpenGL ES 3.0 venue visualization scaffold

### 11. iOS Platform Shell
**Blocked by:** #3
- iOS app entry point with Koin DI setup
- Tab navigation with Compose Multiplatform / SwiftUI interop
- AVFoundation frame capture integration
- PlatformUdpTransport actual (NWConnection/POSIX)
- Ableton Link cinterop bridge (actual)
- Network permissions handling
- Metal venue visualization scaffold

### 12. Compose UI
**Blocked by:** #7, #8, #9, #10
- Perform screen: effect stack sliders, beat visualization, master dimmer, scene presets, tempo display
- Network screen: node list, per-node status cards, universe mapping, health indicators
- Map screen: camera preview, scan progress, fixture position editor, Z-height assignment, fixture grouping
- Agent screen: chat interface, tool call visualization, pre-generation mode
- Compose Canvas venue visualization (fixture dots with live colors)
- Shared theme/design tokens

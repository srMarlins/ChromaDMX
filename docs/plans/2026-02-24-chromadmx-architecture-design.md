# ChromaDMX Architecture Design

**Date:** 2026-02-24
**Status:** Approved
**Author:** Principal Architect (AI-assisted)

---

## 1. Executive Summary

ChromaDMX is a Kotlin Multiplatform (KMP) mobile app (Android + iOS) that serves as an autonomous, agentic lighting director and VJ co-pilot for live electronic music events. It eliminates manual DMX networking, automates fixture spatial mapping via computer vision, runs a 3D generative effect engine synced to live tempo, and provides an AI-powered co-pilot for natural language lighting control.

### Key Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Platform | KMP (Android + iOS) | Single shared codebase for business logic, native platform shells |
| UI Framework | Compose Multiplatform + native views for camera/GPU | Max code sharing, native performance where needed |
| Rendering Strategy | 3D spatial functions on CPU (not GPU framebuffer sampling) | Simpler, faster, no GPU readback bottleneck, works with screen off |
| DMX Protocol | Art-Net 4 primary, sACN (E1.31) secondary | Art-Net is most widely supported by off-the-shelf hardware |
| AI Framework | JetBrains Koog | Native KMP support, tool-calling architecture, multi-provider LLM |
| Tempo Sync | Ableton Link primary, tap tempo fallback | Industry standard, gives BPM + beat phase + bar phase |
| Vision Approach | Classical CV (threshold + blob detection), no ML | Sufficient for brightness detection on ambient-subtracted frames |
| 3D Mapping | Camera auto-detect X/Y + manual Z-height per group | Pragmatic v1 approach, avoids AR framework dependency |

---

## 2. Product Context

**Target audience:** Mobile DJs, indie electronic producers, pop-up event organizers.

**Core problem:** Live lighting requires tedious manual networking (IP configuration, subnet masking), rigid pre-programmed sequences, expensive laptops running complex software, and manual DMX channel patching.

**Product goal:** A mobile-first, agentic lighting controller that auto-discovers hardware, maps physical spaces via camera, generates live 3D effects synced to music tempo, and provides an AI co-pilot for natural language creative control.

### Target Hardware (v1)

- 30 multi-cell RGB LED pixel bars (8 pixels per bar = 240 individually controllable points)
- 720 DMX channels across 2 Art-Net universes
- Off-the-shelf Art-Net/sACN nodes (DMXking, ENTTEC ODE, etc.)
- Future: custom ESP32-based Wi-Fi DMX node

---

## 3. Module Architecture

```
chromadmx/
├── shared/
│   ├── core/                    # Foundation layer
│   │   ├── model/               # DMXUniverse, Fixture, Fixture3D, SpatialCoordinate, Color
│   │   ├── config/              # App config, universe mappings, fixture profiles
│   │   └── util/                # ByteArray extensions, color math, Vec3, interpolation
│   │
│   ├── networking/              # DMX protocol & transport
│   │   ├── protocol/            # ArtNet packet encoding/decoding, sACN framing
│   │   ├── discovery/           # ArtPoll, mDNS/Bonjour node discovery
│   │   ├── transport/           # UDP socket abstraction (expect/actual)
│   │   └── output/              # DMX frame assembler, multi-universe broadcaster
│   │
│   ├── agent/                   # Koog AI agent layer
│   │   ├── tools/               # Tool definitions (network, fixture, scene, diagnostics)
│   │   ├── prompts/             # System prompts, scene generation templates
│   │   └── state/               # Conversation history, agent session management
│   │
│   ├── tempo/                   # Beat clock & synchronization
│   │   ├── clock/               # BeatClock interface, phase tracking, bar counting
│   │   ├── link/                # Ableton Link wrapper (expect/actual for native bridge)
│   │   └── tap/                 # Manual tap tempo, BPM calculator
│   │
│   ├── vision/                  # Spatial mapping logic (pure algorithms)
│   │   ├── detection/           # Blob detection, brightness threshold, contour analysis
│   │   ├── mapping/             # Coordinate mapper, fixture-to-canvas projection
│   │   └── calibration/         # Test-fire sequencer, scan orchestration
│   │
│   ├── engine/                  # 3D effect engine & pixel computation
│   │   ├── scene/               # Scene graph, effect stack, parameter state
│   │   ├── effects/             # 3D spatial effect implementations
│   │   ├── sampler/             # Fixture coordinate → color computation
│   │   └── pipeline/            # Engine loop, frame scheduling
│   │
│   └── simulation/              # Hardware-free development & testing
│       ├── network/             # Simulated Art-Net nodes, packet loss injection
│       ├── fixtures/            # Virtual fixture rigs with 3D coordinates
│       ├── camera/              # Synthetic camera frame generation
│       └── rigs/                # Preset rig configurations
│
├── android/
│   ├── app/                     # Android app module, DI (Koin), navigation
│   ├── ui/                      # Compose UI screens
│   ├── rendering/               # OpenGL ES 3.0 venue visualization
│   ├── camera/                  # CameraX frame capture, preview
│   └── platform/                # UDP socket impl, Link JNI, permissions
│
├── ios/
│   ├── app/                     # iOS app entry, DI, navigation
│   ├── ui/                      # Compose Multiplatform + SwiftUI interop
│   ├── rendering/               # Metal venue visualization
│   ├── camera/                  # AVFoundation frame capture, preview
│   └── platform/                # UDP socket impl, Link cinterop, permissions
│
└── build-logic/                 # Gradle convention plugins, version catalogs
```

### Module Dependency Graph

```
core ← networking
core ← tempo
core ← vision
core ← engine
core ← simulation

networking ← engine       (engine feeds colors to DMX output)
networking ← agent         (agent can scan/configure network)
engine ← agent             (agent sets scene params via tools)
tempo ← engine             (engine reads beat clock)
vision ← simulation        (simulated camera feeds vision pipeline)
networking ← simulation    (simulated nodes for protocol testing)

No circular dependencies.
```

---

## 4. Threading & Performance Architecture

### Three Independent Loops

```
┌─────────────────────────────────────────────────────────────┐
│  UI THREAD (Main/Compose)                                   │
│  - Touch input, UI rendering, Compose recomposition         │
│  - Rate: 60fps (16.6ms frame budget)                        │
│  - NEVER does network I/O, disk I/O, or heavy computation   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  ENGINE THREAD (Background coroutine on Dispatchers.Default)│
│  - Evaluates 3D spatial effect functions at fixture coords  │
│  - Produces Array<Color> (one per fixture pixel, ~240)      │
│  - Rate: 60fps (synchronized to beat clock)                 │
│  - Pure math: ~12 microseconds per frame for 240 points     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  DMX OUTPUT THREAD (Background coroutine)                   │
│  - Packs DMX frames from latest color buffer                │
│  - Broadcasts UDP packets (Art-Net / sACN)                  │
│  - Rate: 40-44Hz (DMX protocol timing)                      │
│  - Independent clock — grabs latest color buffer atomically │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

```
Beat Clock ──┐
              ├──→ EffectStack.evaluate(fixtures) ──→ Color Buffer ──→ DMX Output
UI Input ────┘           ↑                              │
                         │                        (triple-buffered,
Agent Output ────────────┘                         atomic swap)
                                                        │
                                                        ▼
                                                  UDP Broadcast
                                                  (Art-Net/sACN)
```

### Triple-Buffered Color Data

The engine thread writes sampled colors to buffer A while the DMX output thread reads from buffer B. When the engine completes a frame, buffers swap atomically via `kotlinx.atomicfu`. Buffer C absorbs timing differences.

- Engine thread never blocks waiting for DMX output
- DMX output thread never blocks waiting for engine
- Zero mutex contention on the hot path
- If one runs slow, the other re-reads the latest complete buffer

### State Management

```kotlin
class PerformViewModel(
    private val engine: EffectEngine,
    private val beatClock: BeatClock,
    private val dmxOutput: DmxOutputService,
) {
    val sceneState: StateFlow<SceneState>
    val beatState: StateFlow<BeatState>
    val fixtureColors: StateFlow<Array<Color>>

    fun setEffectParam(layer: Int, param: String, value: Float)
    fun loadScene(sceneId: String)
    fun setMasterDimmer(value: Float)
}
```

Unidirectional data flow: UI observes `StateFlow`, user actions call ViewModel methods, ViewModel updates engine state, engine emits new state.

---

## 5. Networking Layer

### Protocol Support

**Art-Net 4** as primary (most hardware compatibility), **sACN (E1.31)** as secondary.

### Components

| Component | Responsibility |
|---|---|
| `ArtNetCodec` | Encode/decode Art-Net packets (ArtDmx, ArtPoll, ArtPollReply) |
| `SacnCodec` | Encode/decode sACN data packets and discovery |
| `DmxOutputService` | Frame assembly, 40Hz broadcast loop, multi-universe management |
| `UdpTransport` | `expect/actual` UDP socket (DatagramSocket on Android, NWConnection/POSIX on iOS) |
| `NodeDiscovery` | ArtPoll broadcast every 3s, collect replies, build live device registry |

### Multi-Universe Support

30 pixel bars × 8 pixels × 3 channels (RGB) = 720 channels → 2 Art-Net universes.

`DmxOutputService` manages `Map<Int, ByteArray>` (universe → channel data) and sends one ArtDmx packet per universe per frame.

### Zero-Config Discovery

1. App broadcasts ArtPoll on the local subnet
2. Art-Net nodes reply with ArtPollReply (IP, name, universe config)
3. App builds a live node registry, auto-maps universes
4. No manual IP entry required for compliant hardware

### Phase 2: Custom ESP32 Node Provisioning

- BLE-based Wi-Fi credential transfer (phone → ESP32 over BLE)
- ESP32 joins network, starts responding to ArtPoll
- App discovers automatically via standard discovery loop
- Clean addition — no changes to protocol layer

### Platform Constraints (Honest Assessment)

- iOS: Cannot programmatically join Wi-Fi networks without user consent (NEHotspotConfiguration requires explicit approval)
- Android: Wi-Fi suggestion API is restricted post-Android 10
- "Silently provisions" is aspirational — the best achievable UX is "minimal friction with clear user consent"

---

## 6. Vision / Spatial Mapping

### Scan Pipeline

```
User taps "Map Fixtures"
        │
        ▼
1. Start camera preview (CameraX / AVFoundation)
        │
        ▼
2. Capture ambient baseline frame (brightness reference)
        │
        ▼
3. For each bar (1..30):
   a. Fire bar at full white via DMX
   b. Wait ~80ms for full brightness
   c. Capture camera frame
   d. Subtract ambient baseline (shared/vision: pure math)
   e. Detect bright blob via threshold + contour
   f. Record centroid coordinates
   g. Turn off bar, wait ~50ms for decay
        │
        ▼
4. For multi-cell bars (endpoint detection):
   a. Fire end-pixels only of each bar
   b. Detect two endpoint blobs
   c. Interpolate pixel positions along the line segment
        │
        ▼
5. Build SpatialMap: Map<FixtureId, List<Coord2D>>
        │
        ▼
6. User assigns Z-height per fixture group
   ("top truss = 3m, stands = 1.5m, floor = 0m")
        │
        ▼
7. Generate Fixture3D coordinates (X from camera, Y from camera, Z from user)
        │
        ▼
8. Show preview overlay for user confirmation/adjustment
```

**Total scan time:** ~6 seconds for 30 bars (including multi-cell endpoint detection).

### Shared/Platform Split

| Layer | Location | Responsibility |
|---|---|---|
| Frame capture | Platform (CameraX/AVFoundation) | Grab camera frame, convert to grayscale FloatArray |
| Blob detection | Shared (pure Kotlin math) | Brightness threshold, connected-component labeling, centroid calc |
| Coordinate mapping | Shared | Camera-to-canvas projection, line interpolation, 3D coordinate assembly |
| Scan orchestration | Shared | State machine driving the test-fire sequence |

### Why Classical CV (No ML)

For detecting "which pixels got brighter against a dark baseline," classical image processing (subtraction + threshold + contour detection) is:
- Faster than ML inference
- More predictable (deterministic)
- Zero model bundling overhead
- Trivially testable with simulated camera frames

ML-based detection can be added later for edge cases (strong ambient light, reflective surfaces).

---

## 7. 3D Effect Engine

### Core Architecture

Effects are **3D spatial functions** computed on the CPU at each fixture's 3D coordinates. No GPU framebuffer rendering or pixel readback in the DMX output path.

```kotlin
interface SpatialEffect {
    fun compute(
        pos: Vec3,          // fixture position in 3D space
        time: Float,        // seconds since engine start
        beat: BeatState,    // bpm, beatPhase, barPhase
        params: EffectParams // per-effect parameters
    ): Color
}
```

### Why 3D Functions (Not 2D Texture Sampling)

Fixtures exist in 3D space (different heights, depths, angles). A 2D framebuffer approach would flatten this into a projection, losing spatial accuracy. Computing colors directly at 3D coordinates:

1. **No GPU readback problem.** Pure math, no `glReadPixels` or Metal texture reads.
2. **Effects naturally work in 3D.** Waves sweep through volume, gradients map across venue space.
3. **Trivially fast.** 240 evaluations × ~50ns = ~12μs per frame. Well under 1ms.
4. **Fully testable.** Unit test: given position + time + params, assert output color.
5. **Works with screen off.** DMX output doesn't depend on any rendering surface.

### Effect Stack & Blending

```kotlin
data class EffectLayer(
    val effect: SpatialEffect,
    val params: EffectParams,
    val blendMode: BlendMode,    // NORMAL, ADDITIVE, MULTIPLY, OVERLAY
    val opacity: Float,          // 0.0 to 1.0
)

class EffectStack {
    val layers: List<EffectLayer>
    val masterDimmer: Float

    fun evaluate(fixtures: List<Fixture3D>, time: Float, beat: BeatState): Array<Color> {
        // For each fixture: evaluate all layers, blend, apply master dimmer
    }
}
```

### V1 Effect Library

| Effect | Description | Key Parameters |
|---|---|---|
| SolidColor | Static single color | `color` |
| GradientSweep3D | Linear gradient sweep through 3D space along arbitrary axis | `axis`, `colorA`, `colorB`, `speed`, `offset` |
| RadialPulse3D | Expanding sphere pulse from center point | `center`, `radius`, `speed`, `colorA`, `colorB`, `beatSync` |
| PerlinNoise3D | 3D animated noise field | `scale`, `speed`, `octaves`, `colorA`, `colorB` |
| WaveEffect3D | Sine wave along configurable axis | `axis`, `frequency`, `speed`, `colorA`, `colorB` |
| Chase3D | Sequential color chase along fixture order | `speed`, `width`, `color`, `direction` |
| Strobe | Beat-synced flash | `rate`, `dutyCycle`, `color`, `beatDivision` |
| RainbowSweep3D | Hue rotation through 3D space | `axis`, `speed`, `saturation`, `brightness` |
| ParticleBurst3D | Point-source color burst on beat | `origin`, `speed`, `color`, `decay`, `beatTrigger` |

### Shader Uniform Contract (for on-screen visualization)

For the optional GPU-rendered venue visualization on screen, every shader receives:

```glsl
uniform float u_time;
uniform float u_beatPhase;    // 0.0 → 1.0 per beat
uniform float u_barPhase;     // 0.0 → 1.0 per 4 beats
uniform float u_bpm;
uniform vec2  u_resolution;
uniform float u_intensity;    // master dimmer
```

The on-screen visualization is for user experience only — it is NOT the source of truth for DMX output.

---

## 8. Agentic AI Layer (Koog)

### Agent Architecture

The ChromaDMX agent is a Koog-based AI agent with a rich tool registry. It uses cloud LLM inference (via Koog's built-in model support — currently OpenAI and Anthropic) to translate natural language into structured tool calls.

### System Prompt

```
You are a lighting director co-pilot for live electronic music events.
You control DMX lighting fixtures through tools. You understand lighting design,
color theory, music genres, and DMX networking.

When asked to create a mood or scene, translate the creative intent into specific
effect parameters: color palettes, movement speeds, spatial patterns, and beat
synchronization settings.

When troubleshooting, use diagnostic tools to identify issues before suggesting
fixes. Always explain what you're doing and why.
```

### Tool Registry

#### Scene Tools
| Tool | Description |
|---|---|
| `setEffect(layer, effectId, params)` | Apply an effect to a layer with parameters |
| `setBlendMode(layer, mode)` | Set blend mode for a layer |
| `setMasterDimmer(value)` | Set master dimmer (0.0 to 1.0) |
| `setColorPalette(colors[])` | Set active color palette |
| `setTempoMultiplier(multiplier)` | Set tempo multiplier (0.5 = half time) |
| `createScene(name, effects[])` | Save current state as named preset |
| `loadScene(name)` | Load a saved scene preset |

#### Network Tools
| Tool | Description |
|---|---|
| `scanNetwork()` | Trigger ArtPoll discovery, return found nodes |
| `getNodeStatus(nodeId)` | Ping node, return connectivity and status |
| `configureNode(nodeId, universe, startAddr)` | Configure a node's universe and start address |
| `diagnoseConnection(nodeId)` | Run full diagnostic on a node connection |

#### Fixture Tools
| Tool | Description |
|---|---|
| `listFixtures()` | Return all mapped fixtures with 3D coordinates |
| `fireFixture(fixtureId, color)` | Test-fire a specific fixture |
| `setFixtureGroup(name, ids[])` | Create a named fixture group |

#### State Tools
| Tool | Description |
|---|---|
| `getEngineState()` | Return current effects, params, scene state |
| `getBeatState()` | Return current BPM, phase, bar count |
| `getNetworkState()` | Return all connected nodes and their status |

### Design Principles

1. **Tools produce deterministic state changes.** The agent calls structured tools — it never generates shader code or raw DMX values. Invalid parameter values are caught by validation, not sent to hardware.

2. **Agent runs on a background coroutine.** Cloud API calls (500ms-2s) never block UI or DMX output. Tool call results are enqueued and applied on the next engine frame.

3. **Pre-generation for offline use.** Backstage with Wi-Fi: "Generate 10 scenes for tonight's techno set." Agent produces scene presets stored locally. During the live set, the operator loads presets without needing cloud.

4. **Graceful degradation.** No internet = no agent. All manual controls, presets, and the engine still work fully. The agent is a power-up, not a dependency.

---

## 9. Tempo Synchronization

### Ableton Link (Primary)

- Industry standard tempo sync protocol
- Supported by Traktor, rekordbox, Ableton, Serato DJ
- Provides BPM + beat phase + bar phase over local network
- C++ library (LinkKit) wrapped via JNI (Android) and cinterop (iOS)
- Works on the same network as DMX nodes

### Tap Tempo (Fallback)

- Manual BPM entry via tap button
- Phase nudge controls for manual beat alignment
- Always available, no external dependencies

### BeatClock Interface

```kotlin
interface BeatClock {
    val bpm: StateFlow<Float>
    val beatPhase: StateFlow<Float>  // 0.0 → 1.0 per beat
    val barPhase: StateFlow<Float>   // 0.0 → 1.0 per 4 beats
    val elapsed: StateFlow<Long>     // ms since start
}
```

Implemented by `AbletonLinkClock` and `TapTempoClock`, switchable at runtime.

---

## 10. Simulation Layer

### Purpose

Enable full development, testing, and demonstration without any physical DMX hardware.

### Components

| Component | Description |
|---|---|
| `SimulatedDmxNode` | Fake Art-Net node on localhost. Responds to ArtPoll, receives ArtDmx, exposes channel values. |
| `SimulatedFixtureRig` | Virtual fixture layouts with 3D coordinates. Presets: "small_dj" (8 pars), "truss_rig" (30 pixel bars), "festival_stage" (100+). |
| `SimulatedCamera` | Generates synthetic camera frames. Draws bright blobs at fixture positions when fired. Configurable noise and ambient. |
| `SimulatedNetwork` | Loopback UDP, configurable packet loss/latency/jitter, node failure injection. |

### Integration

All platform interfaces use `expect/actual` or dependency injection. Simulation mode swaps implementations:

```kotlin
// Production
val transport: UdpTransport = PlatformUdpTransport()
val camera: CameraSource = PlatformCameraSource()

// Simulation
val transport: UdpTransport = SimulatedUdpTransport(packetLoss = 0.02, latencyMs = 5)
val camera: CameraSource = SimulatedCameraSource(rig = SimulatedFixtureRig.TRUSS_RIG_30)
```

### Development Workflows

1. **Automated tests (CI):** Create simulated rig → run effects → assert DMX output colors.
2. **On-device demo mode:** Toggle in app swaps to simulated nodes. Full demo with zero hardware.
3. **Rapid iteration:** Write effect → run simulation → check output mathematically → iterate.
4. **Failure testing:** Inject network failures → verify agent troubleshooting tools work.

---

## 11. UI Architecture

### Navigation

Four bottom tabs: **Perform**, **Network**, **Map**, **Agent**.

### Screen Breakdown

| Screen | Tab | Description |
|---|---|---|
| Perform | Perform | Effect stack with touch sliders, beat visualization, master dimmer, scene presets, venue visualization, tempo display |
| Network | Network | Auto-discovered node list, per-node status, universe mapping, connection health |
| Map | Map | Camera spatial scan, fixture position editor, Z-height assignment, fixture grouping, test mode |
| Agent | Agent | Chat interface, tool call visualization, pre-generation mode, conversation history |

### Framework Split

| Component | Framework | Reason |
|---|---|---|
| Navigation, lists, sliders, cards, chat | Compose Multiplatform | Standard UI, max code sharing |
| Camera preview | Native (CameraX / AVFoundation) | Platform API requirement |
| Venue visualization | Compose Canvas (upgrade to native later if needed) | Start simple |

---

## 12. Technology Stack Summary

| Layer | Technology |
|---|---|
| Language | Kotlin (shared), Swift (iOS platform), Kotlin (Android platform) |
| Framework | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform |
| DI | Koin Multiplatform |
| Async | kotlinx.coroutines |
| Serialization | kotlinx.serialization |
| Networking | Ktor (HTTP for AI API), raw UDP sockets (DMX) |
| AI Agent | JetBrains Koog |
| Tempo Sync | Ableton Link (C++ via JNI/cinterop) |
| Camera (Android) | CameraX |
| Camera (iOS) | AVFoundation |
| GPU Viz (Android) | OpenGL ES 3.0 |
| GPU Viz (iOS) | Metal |
| Build | Gradle with convention plugins, version catalogs |
| Testing | kotlin.test, kotlinx.coroutines.test, simulation layer |

---

## 13. Risk Register

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| GPU venue visualization performance on low-end devices | Medium | Medium | Visualization is optional; DMX output is CPU-only. Degrade gracefully. |
| Ableton Link C++/Kotlin bridge complexity | High | Medium | Well-documented FFI patterns exist. Fall back to tap tempo if blocked. |
| Koog API instability (pre-1.0) | Medium | High | Isolate behind interface. Agent is a power-up, not dependency. |
| Camera blob detection in high ambient light | Medium | Low | Ambient subtraction handles most cases. Add ML detection later if needed. |
| iOS Compose Multiplatform maturity | Medium | Medium | Native fallback for any Compose components that don't perform well. |
| Cloud LLM latency during live performance | Low | High | Pre-generate scenes. Agent is offline-graceful. Manual controls always work. |
| Multi-universe DMX timing sync | Low | Low | Art-Net spec handles this. Both universes sent in same output loop iteration. |

---

## 14. Open Questions (To Resolve During Implementation)

1. **Exact Ableton Link SDK version and KMP bridging approach** — needs spike/prototype
2. **Koog tool definition API** — verify current API surface matches our tool registry design
3. **Compose Multiplatform Canvas performance** for venue visualization at 60fps — may need native fallback
4. **DMX fixture profile database** — do we build our own or import from an existing database (e.g., Open Fixture Library)?
5. **Scene preset file format** — JSON? Protobuf? Need to define schema
6. **App distribution** — TestFlight + Play Store internal track for early testing

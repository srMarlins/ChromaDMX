# WLED + Art-Net Home Lighting Expansion

**Date:** 2026-03-02
**Status:** Approved
**Goal:** Expand ChromaDMX from a pro DMX controller into the consumer/home lighting market by adding first-class WLED support, a room-centric UX, and use-case guided onboarding.

## Motivation

ChromaDMX's core audience is semi-pro/enthusiast DMX users who want easy lighting control. The consumer addressable LED market (WLED, WS2812B strips, ESP32 builds) is orders of magnitude larger and has no good mobile app. WLED has 3M+ GitHub downloads but its built-in UI is basic. ChromaDMX's effect engine, AI agent, and simulation layer are direct differentiators against every existing option.

## 1. WLED Integration Architecture

### New Module: `shared/wled/`

Sits alongside `shared/networking/` as a peer transport layer.

### Discovery

WLED devices advertise via mDNS (`_wled._tcp`). The app discovers them automatically on the local network — no manual IP entry, no universe configuration.

### Dual Control Path

- **Art-Net passthrough** — the engine already outputs Art-Net; WLED already receives it. Works today for realtime per-pixel streaming at 40Hz.
- **Native WLED JSON API** — for capabilities Art-Net can't express: on/off, brightness, segment definitions, device naming, firmware info, live status. HTTP/WebSocket JSON over port 80.

### Device Model

Each discovered WLED device maps to a `WledFixture` that extends the existing fixture system. It stores LED count, segment layout, IP address, and firmware version. The engine treats it like any other fixture — spatial position, effects, and layers all work identically.

### Output Path Selection

| Method | When Used | How |
|--------|-----------|-----|
| Art-Net realtime | Effect engine is running (live mode) | Per-pixel RGB at 40Hz via Art-Net UDP |
| WLED JSON API | Static scenes, on/off, brightness | Single HTTP call, no streaming |

The app automatically uses Art-Net when an effect is playing and falls back to JSON API for static states to save bandwidth.

## 2. Use-Case Guided Onboarding

### New First Step After Splash

"What are you lighting?" — a single question that branches the entire onboarding path.

| Choice | Icon | Onboarding Flow | Default Sim Rig |
|--------|------|-----------------|-----------------|
| "My Room" | LED strip | WLED discovery → auto-map segments → pick a vibe | `DESK_STRIP` |
| "A Stage" | truss/par | Network scan → fixture patch → stage preview | `SMALL_DJ` |
| "Just Exploring" | play button | Skip straight to simulation → rig picker | `ROOM_ACCENT` |

### What Each Path Changes

- **Terminology** — "My Room" never says "universe", "DMX address", or "Art-Net". It says "device", "zone", "strip". "A Stage" uses pro terms.
- **Discovery method** — "My Room" leads with mDNS for WLED + BLE for blank ESP32s. "A Stage" leads with Art-Net broadcast scan.
- **Default effects** — "My Room" starts with ambient presets (warm glow, color cycle, breathing). "A Stage" starts with performance presets (chase, strobe, beat-sync).
- **Complexity gating** — "My Room" hides universe/channel config behind an "Advanced" toggle. Still accessible, not the default.

### Persistence

Stored as a user preference, shapes the app experience going forward, changeable in settings anytime. Not a permanent mode lock.

The existing onboarding state machine (PR #74) accommodates this as a new initial state that determines which subsequent states to queue.

## 3. Room Box View

### Concept

The room is a 3D box viewed from slightly above and to the side — like a diorama or dollhouse with one wall cut away. Users rotate it with swipe gestures to see different walls.

### Six Surfaces

Fixtures attach to: front wall, back wall, left wall, right wall, ceiling, floor. Each surface is a flat plane the user taps to add a strip or panel.

### Rendering

- Fixtures render on box faces — an LED strip on the back wall appears as a glowing line on that face. Colors come from live effect engine output.
- Box interior glows — light from fixtures casts ambient color tint onto adjacent faces, simulating how the room actually feels. Simple color bleed, not raytracing.

### Interaction

- **Drag to rotate, pinch to zoom** — same gesture language as the existing stage view.
- **Tap a face → place mode** — tap a wall, then drag to define where a strip goes along that surface. Length and position, no coordinates exposed.

### Stage View (existing, unchanged)

Isometric/top-down grid, meter coordinates, full fixture editor. Remains the default for "A Stage" users.

### Shared Engine

Both views are different renderers over the same fixture list and normalized [0,1] coordinates. The Room Box maps "back wall, 30% from left, 60% up" to the same coordinate space the engine understands. Users can switch views without losing data.

## 4. WLED Discovery & Pairing UX

### Auto-Discovery (Happy Path)

1. mDNS listener finds `_wled._tcp` devices on the local network automatically.
2. Devices appear in a live list: device name, LED count, signal strength.
3. Pixel mascot announces discoveries ("Found 2 lights on your network!").

### Tap to Adopt

Tapping a device pulls full config via WLED JSON API (`/json/info`): LED count, segment layout, firmware version, IP. The app creates a fixture automatically — no channel mapping, no universe assignment. User sees "Back Wall Strip — 60 LEDs".

### Place in the Box

Immediately after adopting, the app highlights the room box and prompts "Where is this?". User taps a face and drags to position. Done. Fixture is live.

### Blank ESP32 Path

If BLE finds an unprovisioned ESP32 (no WLED yet), offer to flash WLED firmware via the existing BLE provisioning system. User picks WiFi network, app provisions the board, board reboots into WLED, app discovers it via mDNS moments later.

### Fallback

Manual IP entry if mDNS fails (corporate networks sometimes block it). Behind an "Add manually" button at the bottom of the discovery list.

### Persistence

Adopted WLED devices persist in SQLDelight alongside DMX fixtures. A `WledDeviceEntity` stores IP, name, LED count, segment config, and firmware version. On app launch, the app pings known devices to check online status.

## 5. Effect Engine → WLED Segment Mapping

### Segments

One WLED device can have multiple segments (e.g., a 120-LED strip split into left half and right half). The app reads segment definitions from the WLED API and maps each segment to a fixture in the engine.

### Spatial Position Mapping

When the user places a strip on a room box wall, the app knows the strip has N LEDs along that face. Each LED gets a normalized position along the wall surface. The effect engine computes color per-pixel using those spatial positions — a Rainbow Sweep physically sweeps across the strip left to right.

### Transparent to Users

"My Room" users never see Art-Net universes, start addresses, or segment mappings. Everything is auto-assigned behind the scenes.

## 6. Subscription Tiers (Revised)

| | Free | Pro | Ultimate |
|---|---|---|---|
| Real hardware | 2 WLED devices, 1 Art-Net node | 10 WLED devices, 8 Art-Net nodes | Unlimited |
| Effects | 4 basics (Solid, Gradient, Rainbow, Wave) | All 9+ effects | All + future packs |
| Features | Room Box view, auto-discovery, basic presets | Stage View, beat sync, BLE provisioning, camera mapping, all presets | AI agent, data export, genre packs |
| Simulation | 3 rigs (Desk Strip, Room Accent, Small DJ) | All rigs | All rigs |
| Segments/device | 1 | 4 | Unlimited |

### Key Changes from Current Model

- **Free tier controls real hardware.** Two WLED devices covers the typical starter setup (monitor strip + desk accent). Enough to get hooked.
- **Segment limit on free** — strip works as a whole, but no multi-zone control until Pro.
- **Room Box view is free, Stage View is Pro** — home users get what they need. Pro unlocks the full editor.
- **AI agent stays Ultimate** — premium differentiator, most expensive to run (LLM API calls).

### Conversion Funnel

Free: discover 2 devices, play with effects → Pro: more devices, beat sync, segments → Ultimate: AI agent, export, genre packs.

## 7. New Simulation Rig Presets

Three new `RigPreset` entries for home/WLED users:

| Preset | Description | Fixtures |
|--------|-------------|----------|
| `DESK_STRIP` | Streamer desk setup. 1-2 LED strips (60 LEDs each) behind a monitor + accent strip under desk. | 2-3 strips, ~180 LEDs |
| `ROOM_ACCENT` | LED strips along ceiling edges / behind furniture. L-shaped or rectangular perimeter. | 3-4 strips, ~300 LEDs |
| `WALL_PANELS` | Grid of discrete light panels (Nanoleaf-style). 6-12 panels in a wall cluster. | 6-12 single-RGB fixtures |

These are critical because simulation is the free tier's primary preview experience. WLED users who see only "Small DJ" and "Festival Stage" will bounce.

## 8. Agent Tools for WLED

### New Tools

| Tool | Description | Example Prompt |
|------|-------------|---------------|
| `listWledDevices` | Returns discovered + adopted devices with status, LED count, IP | "What lights do I have?" |
| `setDeviceBrightness` | Sets brightness on a specific device or "all" | "Dim the desk strip to 30%" |
| `setDeviceColor` | Sets static color via JSON API | "Make the ceiling warm white" |
| `setDevicePower` | Turns devices on/off | "Turn off everything" |
| `createZone` | Groups devices/segments into a named zone | "Group desk and monitor as 'workstation'" |
| `setZoneEffect` | Applies engine effect to a zone | "Sunset sweep on the workstation" |

### Existing Tools (No Changes Needed)

`setEffect`, `setColorPalette`, `setMasterDimmer`, `listFixtures`, `createScene` — these already work because WLED devices map to fixtures.

### Personality Shift

When onboarding use-case is "My Room", the agent system prompt changes from "professional lighting technician" to "creative ambient lighting assistant." It suggests vibes, times of day, and activities rather than DMX parameters and BPM values.

## Summary

This design expands ChromaDMX from a niche DMX tool to a consumer-ready lighting app by:

1. Adding WLED as a first-class device type (zero-config mDNS discovery)
2. Forking the UX at onboarding so home users never see pro complexity
3. Introducing a Room Box spatial view alongside the existing Stage View
4. Lowering the free tier barrier so real hardware works out of the box
5. Extending the AI agent with home-friendly tools and personality
6. Adding simulation rigs that match consumer setups

The existing engine, effect system, and fixture model require minimal changes — this is primarily a new transport module (`shared/wled/`), new UI components (Room Box, onboarding fork), and revised subscription gates.

# Fixture Profile System (#16) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Overhaul the fixture model from RGB-only to a full channel abstraction system with built-in profiles, engine-to-DMX bridge, and integration across all modules.

**Architecture:** Extend the existing `FixtureProfile` in `shared/core` with typed channels, capabilities, physical properties, and rendering hints. Build a `DmxBridge` that converts the engine's `Array<Color>` output into per-universe `ByteArray` DMX frames using profile channel mappings. Wire through DI so the 40Hz DMX output loop receives real data.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Koin DI, kotlinx-coroutines, atomicfu

---

## Context Files

Before starting, read these files to understand the current state:

- `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/FixtureProfile.kt` — current minimal profile
- `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/Fixture.kt` — current fixture model
- `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/Color.kt` — RGB color model
- `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/DMXUniverse.kt` — universe byte array
- `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/pipeline/EffectEngine.kt` — engine loop
- `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/effect/EffectStack.kt` — compositing
- `shared/networking/src/commonMain/kotlin/com/chromadmx/networking/output/DmxOutputService.kt` — DMX output
- `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/fixtures/SimulatedFixtureRig.kt` — simulated rigs
- `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt` — DI wiring

---

### Task 1: Enhance ChannelType Enum

**Files:**
- Modify: `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/FixtureProfile.kt`
- Test: `shared/core/src/commonTest/kotlin/com/chromadmx/core/model/ChannelTypeTest.kt`

**Step 1: Write the failing test**

Create `shared/core/src/commonTest/kotlin/com/chromadmx/core/model/ChannelTypeTest.kt`:

```kotlin
package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChannelTypeTest {
    @Test
    fun allExpectedChannelTypesExist() {
        val expected = setOf(
            "DIMMER", "RED", "GREEN", "BLUE", "WHITE", "AMBER", "UV",
            "PAN", "TILT", "PAN_FINE", "TILT_FINE",
            "GOBO", "COLOR_WHEEL", "FOCUS", "ZOOM", "PRISM",
            "STROBE", "SHUTTER",
            "GENERIC"
        )
        val actual = ChannelType.entries.map { it.name }.toSet()
        for (name in expected) {
            assertTrue(name in actual, "Missing ChannelType: $name")
        }
    }

    @Test
    fun channelTypeIsColor() {
        assertTrue(ChannelType.RED.isColor)
        assertTrue(ChannelType.GREEN.isColor)
        assertTrue(ChannelType.BLUE.isColor)
        assertTrue(ChannelType.WHITE.isColor)
        assertTrue(ChannelType.AMBER.isColor)
        assertTrue(ChannelType.UV.isColor)
        assertTrue(!ChannelType.PAN.isColor)
        assertTrue(!ChannelType.DIMMER.isColor)
    }

    @Test
    fun channelTypeIsMovement() {
        assertTrue(ChannelType.PAN.isMovement)
        assertTrue(ChannelType.TILT.isMovement)
        assertTrue(ChannelType.PAN_FINE.isMovement)
        assertTrue(ChannelType.TILT_FINE.isMovement)
        assertTrue(!ChannelType.RED.isMovement)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.ChannelTypeTest" -x :shared:agent:testAndroidHostTest`
Expected: FAIL — `ChannelType` doesn't have the new entries or properties yet.

**Step 3: Implement ChannelType**

Replace the `FixtureType` and add `ChannelType` in `FixtureProfile.kt`:

```kotlin
@Serializable
enum class ChannelType(val isColor: Boolean = false, val isMovement: Boolean = false) {
    DIMMER,
    RED(isColor = true), GREEN(isColor = true), BLUE(isColor = true),
    WHITE(isColor = true), AMBER(isColor = true), UV(isColor = true),
    PAN(isMovement = true), TILT(isMovement = true),
    PAN_FINE(isMovement = true), TILT_FINE(isMovement = true),
    GOBO, COLOR_WHEEL, FOCUS, ZOOM, PRISM,
    STROBE, SHUTTER,
    GENERIC
}
```

Keep `FixtureType` enum as-is (PAR, PIXEL_BAR, MOVING_HEAD, STROBE, LASER, OTHER) — add WASH, SPOT:

```kotlin
@Serializable
enum class FixtureType {
    PAR, PIXEL_BAR, MOVING_HEAD, STROBE, WASH, SPOT, LASER, OTHER
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.ChannelTypeTest" -x :shared:agent:testAndroidHostTest`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/core/src/commonMain/kotlin/com/chromadmx/core/model/FixtureProfile.kt
git add shared/core/src/commonTest/kotlin/com/chromadmx/core/model/ChannelTypeTest.kt
git commit -m "feat(core): add ChannelType enum with color/movement classification (#16)"
```

---

### Task 2: Create Channel Data Class and Overhaul FixtureProfile

**Files:**
- Modify: `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/FixtureProfile.kt`
- Test: `shared/core/src/commonTest/kotlin/com/chromadmx/core/model/FixtureProfileTest.kt`

**Step 1: Write the failing test**

Create `shared/core/src/commonTest/kotlin/com/chromadmx/core/model/FixtureProfileTest.kt`:

```kotlin
package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FixtureProfileTest {
    @Test
    fun channelDataClass() {
        val ch = Channel(name = "Red", type = ChannelType.RED, offset = 0)
        assertEquals("Red", ch.name)
        assertEquals(ChannelType.RED, ch.type)
        assertEquals(0, ch.offset)
        assertEquals(0, ch.defaultValue)
    }

    @Test
    fun fixtureProfileWithChannels() {
        val profile = FixtureProfile(
            profileId = "generic-rgb-par",
            name = "Generic RGB Par",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("Red", ChannelType.RED, 0),
                Channel("Green", ChannelType.GREEN, 1),
                Channel("Blue", ChannelType.BLUE, 2),
            )
        )
        assertEquals(3, profile.channelCount)
        assertEquals("generic-rgb-par", profile.profileId)
    }

    @Test
    fun findChannelByType() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("Dimmer", ChannelType.DIMMER, 0),
                Channel("Red", ChannelType.RED, 1),
                Channel("Green", ChannelType.GREEN, 2),
                Channel("Blue", ChannelType.BLUE, 3),
            )
        )
        val red = profile.channelByType(ChannelType.RED)
        assertNotNull(red)
        assertEquals(1, red.offset)
        assertNull(profile.channelByType(ChannelType.PAN))
    }

    @Test
    fun colorMixingCapability() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("R", ChannelType.RED, 0),
                Channel("G", ChannelType.GREEN, 1),
                Channel("B", ChannelType.BLUE, 2),
            ),
            capabilities = Capabilities(colorMixing = ColorMixing.RGB)
        )
        assertEquals(ColorMixing.RGB, profile.capabilities.colorMixing)
    }

    @Test
    fun physicalProperties() {
        val profile = FixtureProfile(
            profileId = "test-mh",
            name = "Test Moving Head",
            type = FixtureType.MOVING_HEAD,
            channels = emptyList(),
            physical = PhysicalProperties(
                beamAngle = 15f,
                panRange = 540f,
                tiltRange = 270f
            )
        )
        assertEquals(540f, profile.physical.panRange)
    }

    @Test
    fun renderingHint() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PIXEL_BAR,
            channels = emptyList(),
            renderHint = RenderHint.BAR
        )
        assertEquals(RenderHint.BAR, profile.renderHint)
    }

    @Test
    fun channelCountDerivedFromChannelsList() {
        val profile = FixtureProfile(
            profileId = "test",
            name = "Test",
            type = FixtureType.PAR,
            channels = listOf(
                Channel("R", ChannelType.RED, 0),
                Channel("G", ChannelType.GREEN, 1),
                Channel("B", ChannelType.BLUE, 2),
            )
        )
        assertEquals(3, profile.channelCount)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.FixtureProfileTest" -x :shared:agent:testAndroidHostTest`
Expected: FAIL — new types don't exist yet.

**Step 3: Rewrite FixtureProfile.kt**

Replace the entire `FixtureProfile.kt` with:

```kotlin
package com.chromadmx.core.model

import kotlinx.serialization.Serializable

/** A single DMX channel within a fixture profile. */
@Serializable
data class Channel(
    val name: String,
    val type: ChannelType,
    val offset: Int,
    val defaultValue: Int = 0
)

/** How the fixture mixes colors. */
@Serializable
enum class ColorMixing { RGB, CMY, COLOR_WHEEL, SINGLE }

/** Fixture capabilities. */
@Serializable
data class Capabilities(
    val colorMixing: ColorMixing = ColorMixing.RGB,
    val hasMovement: Boolean = false,
    val goboSlots: Int = 0
)

/** Physical properties of the fixture. */
@Serializable
data class PhysicalProperties(
    val beamAngle: Float = 25f,
    val panRange: Float = 0f,
    val tiltRange: Float = 0f,
    val pixelCount: Int = 1
)

/** How to render this fixture in the stage preview. */
@Serializable
enum class RenderHint { POINT, BAR, BEAM_CONE }

/**
 * Describes the type and channel layout of a fixture model.
 *
 * The [channels] list defines every DMX channel the fixture uses,
 * with typed offsets relative to the fixture's start address.
 */
@Serializable
data class FixtureProfile(
    val profileId: String,
    val name: String,
    val type: FixtureType,
    val channels: List<Channel>,
    val capabilities: Capabilities = Capabilities(),
    val physical: PhysicalProperties = PhysicalProperties(),
    val renderHint: RenderHint = RenderHint.POINT
) {
    /** Total number of DMX channels this fixture occupies. */
    val channelCount: Int get() = channels.size

    /** Find the first channel of a given type, or null. */
    fun channelByType(type: ChannelType): Channel? = channels.firstOrNull { it.type == type }

    /** Find all channels of a given type. */
    fun channelsByType(type: ChannelType): List<Channel> = channels.filter { it.type == type }

    /** Whether this profile has RGB color channels. */
    val hasRgb: Boolean get() =
        channelByType(ChannelType.RED) != null &&
        channelByType(ChannelType.GREEN) != null &&
        channelByType(ChannelType.BLUE) != null
}

/** Well-known fixture types. */
@Serializable
enum class FixtureType {
    PAR, PIXEL_BAR, MOVING_HEAD, STROBE, WASH, SPOT, LASER, OTHER
}

@Serializable
enum class ChannelType(val isColor: Boolean = false, val isMovement: Boolean = false) {
    DIMMER,
    RED(isColor = true), GREEN(isColor = true), BLUE(isColor = true),
    WHITE(isColor = true), AMBER(isColor = true), UV(isColor = true),
    PAN(isMovement = true), TILT(isMovement = true),
    PAN_FINE(isMovement = true), TILT_FINE(isMovement = true),
    GOBO, COLOR_WHEEL, FOCUS, ZOOM, PRISM,
    STROBE, SHUTTER,
    GENERIC
}
```

**Step 4: Fix compilation across modules**

After changing `FixtureProfile`, any module referencing the old `channelLayout: Map<String, Int>` constructor will break. Search for usages:
- `shared/simulation/` — `SimulatedFixtureRig` and rig presets may reference profiles
- `shared/agent/` — tools may reference profiles

Fix each call site to use the new `channels: List<Channel>` constructor. At minimum, compile check:

Run: `./gradlew :shared:core:compileCommonMainKotlinMetadata :shared:engine:compileCommonMainKotlinMetadata :shared:simulation:compileCommonMainKotlinMetadata :shared:agent:compileCommonMainKotlinMetadata`

Fix any compilation errors before proceeding.

**Step 5: Run tests**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.FixtureProfileTest" -x :shared:agent:testAndroidHostTest`
Expected: PASS

**Step 6: Commit**

```bash
git add shared/core/src/commonMain/kotlin/com/chromadmx/core/model/FixtureProfile.kt
git add shared/core/src/commonTest/kotlin/com/chromadmx/core/model/FixtureProfileTest.kt
git commit -m "feat(core): overhaul FixtureProfile with typed channels, capabilities, physics, rendering (#16)"
```

---

### Task 3: Built-in Fixture Profiles

**Files:**
- Create: `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/BuiltInProfiles.kt`
- Test: `shared/core/src/commonTest/kotlin/com/chromadmx/core/model/BuiltInProfilesTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuiltInProfilesTest {
    @Test
    fun allSixProfilesExist() {
        assertEquals(6, BuiltInProfiles.all().size)
    }

    @Test
    fun genericRgbParIs3Channel() {
        val par = BuiltInProfiles.GENERIC_RGB_PAR
        assertEquals(FixtureType.PAR, par.type)
        assertEquals(3, par.channelCount)
        assertTrue(par.hasRgb)
        assertEquals(RenderHint.POINT, par.renderHint)
    }

    @Test
    fun movingHeadHasMovementChannels() {
        val mh = BuiltInProfiles.GENERIC_MOVING_HEAD
        assertEquals(FixtureType.MOVING_HEAD, mh.type)
        assertTrue(mh.capabilities.hasMovement)
        assertTrue(mh.channelByType(ChannelType.PAN) != null)
        assertTrue(mh.channelByType(ChannelType.TILT) != null)
        assertEquals(RenderHint.BEAM_CONE, mh.renderHint)
        assertEquals(540f, mh.physical.panRange)
        assertEquals(270f, mh.physical.tiltRange)
    }

    @Test
    fun pixelBar8Has24Channels() {
        val bar = BuiltInProfiles.PIXEL_BAR_8
        assertEquals(FixtureType.PIXEL_BAR, bar.type)
        assertEquals(24, bar.channelCount) // 8 pixels * 3 channels
        assertEquals(8, bar.physical.pixelCount)
        assertEquals(RenderHint.BAR, bar.renderHint)
    }

    @Test
    fun strobeProfile() {
        val strobe = BuiltInProfiles.GENERIC_STROBE
        assertEquals(FixtureType.STROBE, strobe.type)
        assertTrue(strobe.channelByType(ChannelType.STROBE) != null ||
                   strobe.channelByType(ChannelType.DIMMER) != null)
    }

    @Test
    fun lookupByIdWorks() {
        val found = BuiltInProfiles.findById("generic-rgb-par")
        assertEquals(BuiltInProfiles.GENERIC_RGB_PAR, found)
    }

    @Test
    fun lookupByIdReturnsNullForUnknown() {
        val found = BuiltInProfiles.findById("nonexistent")
        assertEquals(null, found)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.BuiltInProfilesTest" -x :shared:agent:testAndroidHostTest`

**Step 3: Implement BuiltInProfiles**

Create `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/BuiltInProfiles.kt`:

```kotlin
package com.chromadmx.core.model

object BuiltInProfiles {
    val GENERIC_RGB_PAR = FixtureProfile(
        profileId = "generic-rgb-par",
        name = "Generic RGB Par",
        type = FixtureType.PAR,
        channels = listOf(
            Channel("Red", ChannelType.RED, 0),
            Channel("Green", ChannelType.GREEN, 1),
            Channel("Blue", ChannelType.BLUE, 2),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        renderHint = RenderHint.POINT
    )

    val GENERIC_MOVING_HEAD = FixtureProfile(
        profileId = "generic-moving-head",
        name = "Generic Moving Head",
        type = FixtureType.MOVING_HEAD,
        channels = listOf(
            Channel("Pan", ChannelType.PAN, 0),
            Channel("Pan Fine", ChannelType.PAN_FINE, 1),
            Channel("Tilt", ChannelType.TILT, 2),
            Channel("Tilt Fine", ChannelType.TILT_FINE, 3),
            Channel("Dimmer", ChannelType.DIMMER, 4),
            Channel("Red", ChannelType.RED, 5),
            Channel("Green", ChannelType.GREEN, 6),
            Channel("Blue", ChannelType.BLUE, 7),
            Channel("Gobo", ChannelType.GOBO, 8),
            Channel("Strobe", ChannelType.STROBE, 9),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.RGB, hasMovement = true, goboSlots = 8),
        physical = PhysicalProperties(beamAngle = 15f, panRange = 540f, tiltRange = 270f),
        renderHint = RenderHint.BEAM_CONE
    )

    val PIXEL_BAR_8 = FixtureProfile(
        profileId = "pixel-bar-8",
        name = "Pixel Bar 8-Segment",
        type = FixtureType.PIXEL_BAR,
        channels = (0 until 8).flatMap { pixel ->
            listOf(
                Channel("Pixel${pixel}_R", ChannelType.RED, pixel * 3),
                Channel("Pixel${pixel}_G", ChannelType.GREEN, pixel * 3 + 1),
                Channel("Pixel${pixel}_B", ChannelType.BLUE, pixel * 3 + 2),
            )
        },
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        physical = PhysicalProperties(pixelCount = 8),
        renderHint = RenderHint.BAR
    )

    val PIXEL_BAR_16 = FixtureProfile(
        profileId = "pixel-bar-16",
        name = "Pixel Bar 16-Segment",
        type = FixtureType.PIXEL_BAR,
        channels = (0 until 16).flatMap { pixel ->
            listOf(
                Channel("Pixel${pixel}_R", ChannelType.RED, pixel * 3),
                Channel("Pixel${pixel}_G", ChannelType.GREEN, pixel * 3 + 1),
                Channel("Pixel${pixel}_B", ChannelType.BLUE, pixel * 3 + 2),
            )
        },
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        physical = PhysicalProperties(pixelCount = 16),
        renderHint = RenderHint.BAR
    )

    val GENERIC_STROBE = FixtureProfile(
        profileId = "generic-strobe",
        name = "Generic Strobe",
        type = FixtureType.STROBE,
        channels = listOf(
            Channel("Dimmer", ChannelType.DIMMER, 0),
            Channel("Strobe", ChannelType.STROBE, 1),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.SINGLE),
        renderHint = RenderHint.POINT
    )

    val GENERIC_WASH = FixtureProfile(
        profileId = "generic-wash",
        name = "Generic Wash",
        type = FixtureType.WASH,
        channels = listOf(
            Channel("Dimmer", ChannelType.DIMMER, 0),
            Channel("Red", ChannelType.RED, 1),
            Channel("Green", ChannelType.GREEN, 2),
            Channel("Blue", ChannelType.BLUE, 3),
            Channel("Zoom", ChannelType.ZOOM, 4),
        ),
        capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        physical = PhysicalProperties(beamAngle = 60f),
        renderHint = RenderHint.POINT
    )

    private val allProfiles = listOf(
        GENERIC_RGB_PAR, GENERIC_MOVING_HEAD,
        PIXEL_BAR_8, PIXEL_BAR_16,
        GENERIC_STROBE, GENERIC_WASH
    )

    fun all(): List<FixtureProfile> = allProfiles

    fun findById(profileId: String): FixtureProfile? = allProfiles.find { it.profileId == profileId }
}
```

**Step 4: Run tests**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.BuiltInProfilesTest" -x :shared:agent:testAndroidHostTest`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/core/src/commonMain/kotlin/com/chromadmx/core/model/BuiltInProfiles.kt
git add shared/core/src/commonTest/kotlin/com/chromadmx/core/model/BuiltInProfilesTest.kt
git commit -m "feat(core): add 6 built-in fixture profiles (#16)"
```

---

### Task 4: Link Fixture to Profile

**Files:**
- Modify: `shared/core/src/commonMain/kotlin/com/chromadmx/core/model/Fixture.kt`
- Test: `shared/core/src/commonTest/kotlin/com/chromadmx/core/model/FixtureTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FixtureTest {
    @Test
    fun fixtureHasProfileId() {
        val fixture = Fixture(
            fixtureId = "par-1",
            name = "Par 1",
            channelStart = 0,
            channelCount = 3,
            universeId = 0,
            profileId = "generic-rgb-par"
        )
        assertEquals("generic-rgb-par", fixture.profileId)
    }

    @Test
    fun fixtureDefaultsToRgbParProfile() {
        val fixture = Fixture(
            fixtureId = "par-1",
            name = "Par 1",
            channelStart = 0,
            channelCount = 3,
            universeId = 0
        )
        assertEquals("generic-rgb-par", fixture.profileId)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.FixtureTest" -x :shared:agent:testAndroidHostTest`

**Step 3: Add profileId to Fixture**

In `Fixture.kt`, add `profileId` with a default so existing code doesn't break:

```kotlin
@Serializable
data class Fixture(
    val fixtureId: String,
    val name: String,
    val channelStart: Int,
    val channelCount: Int,
    val universeId: Int,
    val profileId: String = "generic-rgb-par"
)
```

**Step 4: Run tests — both new and existing**

Run: `./gradlew :shared:core:testAndroidHostTest -x :shared:agent:testAndroidHostTest`
Expected: ALL PASS (existing tests use positional/named args without profileId, so the default kicks in)

**Step 5: Commit**

```bash
git add shared/core/src/commonMain/kotlin/com/chromadmx/core/model/Fixture.kt
git add shared/core/src/commonTest/kotlin/com/chromadmx/core/model/FixtureTest.kt
git commit -m "feat(core): add profileId to Fixture with backward-compatible default (#16)"
```

---

### Task 5: DmxBridge — Convert Engine Colors to DMX Bytes

**Files:**
- Create: `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/bridge/DmxBridge.kt`
- Test: `shared/engine/src/commonTest/kotlin/com/chromadmx/engine/bridge/DmxBridgeTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.engine.bridge

import com.chromadmx.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DmxBridgeTest {
    private val profiles = mapOf(
        "generic-rgb-par" to BuiltInProfiles.GENERIC_RGB_PAR
    )

    @Test
    fun convertsColorsToUniverseBytes() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("f1", "Par 1", channelStart = 0, channelCount = 3, universeId = 0),
                position = Vec3.ZERO
            ),
            Fixture3D(
                fixture = Fixture("f2", "Par 2", channelStart = 3, channelCount = 3, universeId = 0),
                position = Vec3.ZERO
            )
        )
        val colors = arrayOf(Color.RED, Color.GREEN)
        val bridge = DmxBridge(fixtures, profiles)

        val result = bridge.convert(colors)

        assertTrue(result.containsKey(0))
        val data = result[0]!!
        // Par 1 at channels 0-2: RED = (255, 0, 0)
        assertEquals(255, data[0].toInt() and 0xFF)
        assertEquals(0, data[1].toInt() and 0xFF)
        assertEquals(0, data[2].toInt() and 0xFF)
        // Par 2 at channels 3-5: GREEN = (0, 255, 0)
        assertEquals(0, data[3].toInt() and 0xFF)
        assertEquals(255, data[4].toInt() and 0xFF)
        assertEquals(0, data[5].toInt() and 0xFF)
    }

    @Test
    fun multipleUniverses() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("f1", "Par 1", channelStart = 0, channelCount = 3, universeId = 0),
                position = Vec3.ZERO
            ),
            Fixture3D(
                fixture = Fixture("f2", "Par 2", channelStart = 0, channelCount = 3, universeId = 1),
                position = Vec3.ZERO
            )
        )
        val colors = arrayOf(Color.RED, Color.BLUE)
        val bridge = DmxBridge(fixtures, profiles)

        val result = bridge.convert(colors)

        assertEquals(2, result.size)
        // Universe 0: RED
        assertEquals(255, result[0]!![0].toInt() and 0xFF)
        // Universe 1: BLUE
        assertEquals(255, result[1]!![2].toInt() and 0xFF)
    }

    @Test
    fun profileWithDimmerChannel() {
        val washProfiles = mapOf(
            "generic-wash" to BuiltInProfiles.GENERIC_WASH
        )
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("w1", "Wash 1", channelStart = 0, channelCount = 5,
                    universeId = 0, profileId = "generic-wash"),
                position = Vec3.ZERO
            )
        )
        val colors = arrayOf(Color(0.5f, 0.5f, 0.5f))
        val bridge = DmxBridge(fixtures, washProfiles)

        val result = bridge.convert(colors)
        val data = result[0]!!
        // Dimmer at offset 0 should be 255 (full on when color is present)
        assertEquals(255, data[0].toInt() and 0xFF)
        // RGB at offsets 1-3
        assertEquals(128, data[1].toInt() and 0xFF, "Red should be ~128")
    }

    @Test
    fun emptyFixturesProducesEmptyResult() {
        val bridge = DmxBridge(emptyList(), profiles)
        val result = bridge.convert(emptyArray())
        assertTrue(result.isEmpty())
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:engine:testAndroidHostTest --tests "com.chromadmx.engine.bridge.DmxBridgeTest" -x :shared:agent:testAndroidHostTest`

**Step 3: Implement DmxBridge**

Create `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/bridge/DmxBridge.kt`:

```kotlin
package com.chromadmx.engine.bridge

import com.chromadmx.core.model.BuiltInProfiles
import com.chromadmx.core.model.ChannelType
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureProfile

/**
 * Converts per-fixture RGB colors from the effect engine into
 * per-universe DMX byte arrays for the output service.
 *
 * Uses [FixtureProfile] channel mappings to write color values
 * to the correct DMX addresses. Fixtures without a matching profile
 * fall back to simple 3-channel RGB mapping.
 */
class DmxBridge(
    private val fixtures: List<Fixture3D>,
    private val profiles: Map<String, FixtureProfile> = emptyMap()
) {
    /**
     * Convert an array of per-fixture colors into per-universe DMX data.
     *
     * @param colors One [Color] per fixture, parallel to the [fixtures] list.
     * @return Map of universe ID to 512-byte DMX channel data.
     */
    fun convert(colors: Array<Color>): Map<Int, ByteArray> {
        if (fixtures.isEmpty()) return emptyMap()

        val universes = mutableMapOf<Int, ByteArray>()

        for (i in fixtures.indices) {
            val fixture = fixtures[i].fixture
            val color = colors.getOrElse(i) { Color.BLACK }
            val data = universes.getOrPut(fixture.universeId) { ByteArray(512) }
            val profile = profiles[fixture.profileId]
                ?: BuiltInProfiles.findById(fixture.profileId)

            if (profile != null && profile.hasRgb) {
                writeProfileChannels(data, fixture.channelStart, profile, color)
            } else {
                // Fallback: write as simple 3-channel RGB
                writeSimpleRgb(data, fixture.channelStart, color)
            }
        }

        return universes
    }

    private fun writeProfileChannels(
        data: ByteArray,
        channelStart: Int,
        profile: FixtureProfile,
        color: Color
    ) {
        val clamped = color.clamped()

        for (channel in profile.channels) {
            val addr = channelStart + channel.offset
            if (addr < 0 || addr >= 512) continue

            data[addr] = when (channel.type) {
                ChannelType.RED -> (clamped.r * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                ChannelType.GREEN -> (clamped.g * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                ChannelType.BLUE -> (clamped.b * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                ChannelType.DIMMER -> {
                    // Set dimmer to 255 when any color component is non-zero
                    val brightness = maxOf(clamped.r, clamped.g, clamped.b)
                    (brightness * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.WHITE -> {
                    // White = minimum of RGB (conservative approach)
                    val white = minOf(clamped.r, clamped.g, clamped.b)
                    (white * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
                ChannelType.STROBE -> channel.defaultValue.toByte()
                else -> channel.defaultValue.toByte()
            }
        }
    }

    private fun writeSimpleRgb(data: ByteArray, channelStart: Int, color: Color) {
        val bytes = color.toDmxBytes()
        for (i in bytes.indices) {
            val addr = channelStart + i
            if (addr in 0 until 512) {
                data[addr] = bytes[i]
            }
        }
    }
}
```

**Step 4: Run tests**

Run: `./gradlew :shared:engine:testAndroidHostTest --tests "com.chromadmx.engine.bridge.DmxBridgeTest" -x :shared:agent:testAndroidHostTest`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/engine/src/commonMain/kotlin/com/chromadmx/engine/bridge/DmxBridge.kt
git add shared/engine/src/commonTest/kotlin/com/chromadmx/engine/bridge/DmxBridgeTest.kt
git commit -m "feat(engine): add DmxBridge to convert engine colors to DMX bytes (#16)"
```

---

### Task 6: Wire Engine → DmxBridge → DmxOutputService in DI

**Files:**
- Create: `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/bridge/DmxOutputBridge.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt`
- Test: `shared/engine/src/commonTest/kotlin/com/chromadmx/engine/bridge/DmxOutputBridgeTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.engine.bridge

import com.chromadmx.core.model.*
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.core.EffectParams
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DmxOutputBridgeTest {
    @Test
    fun bridgeReadsFromEngineAndProducesFrames() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("f1", "Par 1", 0, 3, 0),
                position = Vec3.ZERO
            )
        )
        val engine = EffectEngine(TestScope(), fixtures)
        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams.EMPTY.with("color", Color.RED)
            )
        )
        engine.tick()

        val bridge = DmxBridge(fixtures, emptyMap())
        val colors = engine.colorOutput.let {
            it.swapRead()
            it.readSlot()
        }
        val result = bridge.convert(colors)

        assertTrue(result.containsKey(0))
        assertEquals(255, result[0]!![0].toInt() and 0xFF)
    }
}
```

**Step 2: Run test to verify it passes (this is an integration test verifying existing pieces work together)**

Run: `./gradlew :shared:engine:testAndroidHostTest --tests "com.chromadmx.engine.bridge.DmxOutputBridgeTest" -x :shared:agent:testAndroidHostTest`

**Step 3: Create DmxOutputBridge coroutine loop**

Create `shared/engine/src/commonMain/kotlin/com/chromadmx/engine/bridge/DmxOutputBridge.kt`:

```kotlin
package com.chromadmx.engine.bridge

import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.FixtureProfile
import com.chromadmx.engine.pipeline.TripleBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Coroutine-based bridge that reads from the engine's triple buffer
 * and feeds converted DMX data to an output callback.
 *
 * Runs at a configurable rate (default 40Hz to match DMX output).
 */
class DmxOutputBridge(
    private val colorOutput: TripleBuffer<Array<Color>>,
    private val dmxBridge: DmxBridge,
    private val onFrame: (Map<Int, ByteArray>) -> Unit,
    private val scope: CoroutineScope,
    private val intervalMs: Long = 25L // 40Hz
) {
    private var job: Job? = null
    val isRunning: Boolean get() = job?.isActive == true

    fun start() {
        if (isRunning) return
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (colorOutput.swapRead()) {
                    val colors = colorOutput.readSlot()
                    val frame = dmxBridge.convert(colors)
                    if (frame.isNotEmpty()) {
                        onFrame(frame)
                    }
                }
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
```

**Step 4: Wire in ChromaDiModule**

Add to `ChromaDiModule.kt` after the EffectEngine single:

```kotlin
// --- Engine → DMX Bridge ---
single {
    val engine = get<EffectEngine>()
    val fixtures = engine.fixtures // Need to expose fixtures from engine
    DmxBridge(fixtures, emptyMap()) // Profiles will be injected when profile store exists
}
single {
    val engine = get<EffectEngine>()
    val bridge = get<DmxBridge>()
    val outputService = get<DmxOutputService>()
    DmxOutputBridge(
        colorOutput = engine.colorOutput,
        dmxBridge = bridge,
        onFrame = { frame -> outputService.updateFrame(frame) },
        scope = get()
    ).apply { start() }
}
```

Note: You'll need to make `EffectEngine.fixtures` public (currently it's a `private val` — change to `val`).

**Step 5: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: PASS

**Step 6: Commit**

```bash
git add shared/engine/src/commonMain/kotlin/com/chromadmx/engine/bridge/DmxOutputBridge.kt
git add shared/engine/src/commonTest/kotlin/com/chromadmx/engine/bridge/DmxOutputBridgeTest.kt
git add shared/engine/src/commonMain/kotlin/com/chromadmx/engine/pipeline/EffectEngine.kt
git add shared/src/commonMain/kotlin/com/chromadmx/di/ChromaDiModule.kt
git commit -m "feat(engine): wire DmxOutputBridge connecting engine to DMX output (#16)"
```

---

### Task 7: Update Simulation Module to Use Profiles

**Files:**
- Modify: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/SmallDjRig.kt`
- Modify: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/TrussRig.kt`
- Modify: `shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/FestivalStageRig.kt`
- Test: `shared/simulation/src/commonTest/kotlin/com/chromadmx/simulation/fixtures/SimulatedFixtureRigTest.kt` (existing tests should still pass)

**Step 1: Update rig presets to use profileId**

For each rig file, update the `Fixture` constructors to include the correct `profileId`:
- `SmallDjRig`: 8 pars → `profileId = "generic-rgb-par"`
- `TrussRig`: 30 pixel bars → `profileId = "pixel-bar-8"`
- `FestivalStageRig`: mixed → appropriate profileIds per fixture type

Since `Fixture` now defaults `profileId = "generic-rgb-par"`, the SmallDjRig may already be correct. Check and update TrussRig and FestivalStageRig.

**Step 2: Run all simulation tests**

Run: `./gradlew :shared:simulation:testAndroidHostTest -x :shared:agent:testAndroidHostTest`
Expected: ALL PASS

**Step 3: Commit**

```bash
git add shared/simulation/src/commonMain/kotlin/com/chromadmx/simulation/rigs/
git commit -m "feat(simulation): update rig presets with fixture profile IDs (#16)"
```

---

### Task 8: Update Agent Tools for Fixture Capabilities

**Files:**
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/FixtureTools.kt`
- Modify: `shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/StateTools.kt`
- Test: existing agent tests should still pass

**Step 1: Update ListFixturesTool**

The `ListFixturesTool` should now include profile information in its output. Update the `execute()` method to include fixture type and capabilities in the returned string.

**Step 2: Update GetEngineStateTool**

Include available profiles in the engine state output.

**Step 3: Run agent tests**

Run: `./gradlew :shared:agent:testAndroidHostTest`
Expected: ALL PASS

**Step 4: Commit**

```bash
git add shared/agent/src/commonMain/kotlin/com/chromadmx/agent/tools/
git commit -m "feat(agent): include fixture profile info in agent tool responses (#16)"
```

---

### Task 9: Profile Serialization Test

**Files:**
- Test: `shared/core/src/commonTest/kotlin/com/chromadmx/core/model/ProfileSerializationTest.kt`

**Step 1: Write the test**

```kotlin
package com.chromadmx.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileSerializationTest {
    private val json = Json { prettyPrint = false }

    @Test
    fun roundTripBuiltInProfile() {
        val original = BuiltInProfiles.GENERIC_RGB_PAR
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<FixtureProfile>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTripMovingHeadProfile() {
        val original = BuiltInProfiles.GENERIC_MOVING_HEAD
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<FixtureProfile>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTripCustomProfile() {
        val custom = FixtureProfile(
            profileId = "custom-rgbw",
            name = "Custom RGBW Fixture",
            type = FixtureType.OTHER,
            channels = listOf(
                Channel("Red", ChannelType.RED, 0),
                Channel("Green", ChannelType.GREEN, 1),
                Channel("Blue", ChannelType.BLUE, 2),
                Channel("White", ChannelType.WHITE, 3),
            ),
            capabilities = Capabilities(colorMixing = ColorMixing.RGB),
        )
        val encoded = json.encodeToString(custom)
        val decoded = json.decodeFromString<FixtureProfile>(encoded)
        assertEquals(custom, decoded)
    }
}
```

**Step 2: Run test**

Run: `./gradlew :shared:core:testAndroidHostTest --tests "com.chromadmx.core.model.ProfileSerializationTest" -x :shared:agent:testAndroidHostTest`
Expected: PASS (all data classes are already @Serializable)

**Step 3: Commit**

```bash
git add shared/core/src/commonTest/kotlin/com/chromadmx/core/model/ProfileSerializationTest.kt
git commit -m "test(core): add fixture profile serialization round-trip tests (#16)"
```

---

### Task 10: Full Module Integration Test

**Step 1: Run all tests across all modules**

```bash
./gradlew :shared:core:testAndroidHostTest :shared:engine:testAndroidHostTest :shared:networking:testAndroidHostTest :shared:simulation:testAndroidHostTest :shared:tempo:testAndroidHostTest :shared:vision:testAndroidHostTest :shared:agent:testAndroidHostTest
```

Expected: ALL PASS

**Step 2: Compile Android app**

```bash
./gradlew :android:app:assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 3: Final commit if any fixups were needed**

```bash
git add -A
git commit -m "fix: resolve integration issues from fixture profile overhaul (#16)"
```

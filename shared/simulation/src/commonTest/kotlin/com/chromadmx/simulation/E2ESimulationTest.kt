package com.chromadmx.simulation

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effects.Chase3DEffect
import com.chromadmx.engine.effects.GradientSweep3DEffect
import com.chromadmx.engine.effects.PerlinNoise3DEffect
import com.chromadmx.engine.effects.RadialPulse3DEffect
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.effects.StrobeEffect
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.simulation.camera.SimulatedCamera
import com.chromadmx.simulation.camera.SimulatedCamera.FixtureState
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.simulation.network.SimulatedDmxNode
import com.chromadmx.simulation.network.SimulatedNetwork
import com.chromadmx.simulation.network.SimulatedNetworkBus
import com.chromadmx.tempo.tap.TapTempoClock
import com.chromadmx.vision.camera.GrayscaleFrame
import com.chromadmx.vision.detection.BlobDetector
import com.chromadmx.vision.mapping.SpatialMapBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * End-to-end simulation integration tests.
 *
 * These tests validate the full pipeline (fixtures -> effects -> engine -> output)
 * in simulation mode without any hardware. Each test constructs real objects from
 * the production modules and asserts on the computed output.
 */
class E2ESimulationTest {

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /** Check whether a color has any non-zero channel. */
    private fun Color.isNonBlack(): Boolean =
        r > 0.001f || g > 0.001f || b > 0.001f

    /** Sum of RGB channels — a rough proxy for brightness. */
    private fun Color.luminance(): Float =
        0.299f * r + 0.587f * g + 0.114f * b

    // ================================================================== //
    //  1. Full Pipeline Test                                              //
    // ================================================================== //

    @Test
    fun fullPipeline_solidColor_allFixturesLit() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        // Register a SolidColor effect and push it onto the stack
        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.RED),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Tick the engine (writes to triple buffer)
        engine.tick()
        val output = engine.colorOutput.read()

        // Every fixture should be red (non-black)
        assertEquals(rig.fixtureCount, output.size)
        for (i in output.indices) {
            assertTrue(
                output[i].isNonBlack(),
                "Fixture ${rig.fixtures[i].fixture.fixtureId} should be lit"
            )
            assertEquals(1.0f, output[i].r, 0.001f)
            assertEquals(0.0f, output[i].g, 0.001f)
            assertEquals(0.0f, output[i].b, 0.001f)
        }
    }

    @Test
    fun fullPipeline_radialPulse_fixturesInRangeAreLit() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        // Pulse centered at origin, expanding at 2 units/sec
        engine.effectStack.addLayer(
            EffectLayer(
                effect = RadialPulse3DEffect(),
                params = EffectParams()
                    .with("centerX", 0f)
                    .with("centerY", 1f)    // matches rig depth
                    .with("centerZ", 2.5f)  // matches rig height
                    .with("speed", 2.0f)
                    .with("width", 2.0f)    // wide shell so fixtures are inside
                    .with("color", Color.WHITE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Evaluate at time=1.0 -> shell radius = 2.0
        // Fixtures span from x=-3.5 to x=3.5, all at y=1, z=2.5
        // Distance from center for each fixture is |x| (since center is at 0,1,2.5)
        // Shell covers dist from 1.0 to 3.0 (radius=2, halfWidth=1.0)
        // So fixtures at x=-2,-1,0,1,2 should be lit
        val frame = engine.evaluateFrame(1.0f, BeatState.IDLE)

        // At least some fixtures should be non-black
        val litCount = frame.count { it.isNonBlack() }
        assertTrue(litCount > 0, "Expected some fixtures lit by radial pulse, got 0")
    }

    @Test
    fun fullPipeline_effectRegistry_allBuiltInsRegistered() {
        val registry = EffectRegistry()
        registry.register(SolidColorEffect())
        registry.register(RadialPulse3DEffect())
        registry.register(GradientSweep3DEffect())
        registry.register(PerlinNoise3DEffect())
        registry.register(Chase3DEffect())
        registry.register(StrobeEffect())

        // Verify all effects are accessible by ID
        assertTrue(registry.size >= 6, "Expected at least 6 built-in effects, got ${registry.size}")
        assertTrue(registry.get("solid-color") is SolidColorEffect)
        assertTrue(registry.get("radial-pulse-3d") is RadialPulse3DEffect)
        assertTrue(registry.get("gradient-sweep-3d") is GradientSweep3DEffect)
        assertTrue(registry.get("perlin-noise-3d") is PerlinNoise3DEffect)
        assertTrue(registry.get("chase-3d") is Chase3DEffect)
        assertTrue(registry.get("strobe") is StrobeEffect)
    }

    @Test
    fun fullPipeline_engineTickWritesToTripleBuffer() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.BLUE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Tick writes to the triple buffer
        engine.tick()

        // Read from triple buffer should yield the blue frame
        val output = engine.colorOutput.read()
        for (color in output) {
            assertEquals(0.0f, color.r, 0.001f)
            assertEquals(0.0f, color.g, 0.001f)
            assertEquals(1.0f, color.b, 0.001f)
        }
    }

    // ================================================================== //
    //  2. Effect Pipeline Test                                            //
    // ================================================================== //

    @Test
    fun effectPipeline_twoLayers_colorsChangeOverTime() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        // Layer 1: GradientSweep (scrolls along x-axis)
        engine.effectStack.addLayer(
            EffectLayer(
                effect = GradientSweep3DEffect(),
                params = EffectParams()
                    .with("axis", "x")
                    .with("speed", 2.0f)
                    .with("palette", listOf(Color.RED, Color.BLUE)),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Layer 2: PerlinNoise (additive, half opacity)
        engine.effectStack.addLayer(
            EffectLayer(
                effect = PerlinNoise3DEffect(),
                params = EffectParams()
                    .with("scale", 1.0f)
                    .with("speed", 1.0f)
                    .with("palette", listOf(Color.BLACK, Color.GREEN)),
                blendMode = BlendMode.ADDITIVE,
                opacity = 0.5f
            )
        )

        // Evaluate at two different times
        val frameAtT0 = engine.evaluateFrame(0.0f, BeatState.IDLE)
        val frameAtT1 = engine.evaluateFrame(1.0f, BeatState.IDLE)

        // At least some fixtures should have different colors at t=0 vs t=1
        var changedCount = 0
        for (i in frameAtT0.indices) {
            if (frameAtT0[i] != frameAtT1[i]) changedCount++
        }
        assertTrue(
            changedCount > 0,
            "Expected colors to change over time with GradientSweep + PerlinNoise"
        )
    }

    @Test
    fun effectPipeline_masterDimmerZero_allBlack() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.WHITE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.masterDimmer = 0.0f

        val frame = engine.evaluateFrame(0f, BeatState.IDLE)

        for (color in frame) {
            assertEquals(0.0f, color.r, 0.001f)
            assertEquals(0.0f, color.g, 0.001f)
            assertEquals(0.0f, color.b, 0.001f)
        }
    }

    @Test
    fun effectPipeline_masterDimmerFull_normalOutput() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.WHITE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.masterDimmer = 1.0f

        val frame = engine.evaluateFrame(0f, BeatState.IDLE)

        for (color in frame) {
            assertEquals(1.0f, color.r, 0.001f)
            assertEquals(1.0f, color.g, 0.001f)
            assertEquals(1.0f, color.b, 0.001f)
        }
    }

    @Test
    fun effectPipeline_masterDimmerHalf_scaledOutput() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.WHITE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.masterDimmer = 0.5f

        val frame = engine.evaluateFrame(0f, BeatState.IDLE)

        for (color in frame) {
            assertEquals(0.5f, color.r, 0.001f)
            assertEquals(0.5f, color.g, 0.001f)
            assertEquals(0.5f, color.b, 0.001f)
        }
    }

    @Test
    fun effectPipeline_consecutiveTicks_produceDifferentOutput() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        // Chase effect scrolls over time
        engine.effectStack.addLayer(
            EffectLayer(
                effect = Chase3DEffect(),
                params = EffectParams()
                    .with("axis", "x")
                    .with("speed", 5.0f)
                    .with("color", Color.WHITE)
                    .with("tail", 0.3f),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        val frames = mutableListOf<Array<Color>>()
        for (t in 0 until 5) {
            frames.add(engine.evaluateFrame(t * 0.5f, BeatState.IDLE))
        }

        // At least 2 consecutive frames should differ
        var diffCount = 0
        for (i in 1 until frames.size) {
            val prev = frames[i - 1]
            val curr = frames[i]
            if (prev.zip(curr).any { (a, b) -> a != b }) diffCount++
        }
        assertTrue(diffCount >= 2, "Expected at least 2 frame transitions to differ, got $diffCount")
    }

    // ================================================================== //
    //  3. Vision Pipeline Test                                            //
    // ================================================================== //

    @Test
    fun visionPipeline_simulatedCamera_blobDetection() {
        // Set up a camera pointed at the DJ rig
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,    // no noise for deterministic detection
            ambientLevel = 5,
            blobRadius = 15f,
            cameraPosition = Vec3(0f, -6f, 1.5f),
            cameraTarget = Vec3(0f, 1f, 2.5f)
        )

        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)

        // Turn on 3 specific fixtures
        val onIds = setOf("dj-par-1", "dj-par-3", "dj-par-6")
        val frame = camera.generateFrame(rig.fixtures, onIds)

        // Create a GrayscaleFrame for the BlobDetector
        val pixels = FloatArray(frame.size) { (frame[it].toInt() and 0xFF) / 255f }
        val grayFrame = GrayscaleFrame(pixels, camera.width, camera.height)

        // Subtract ambient to get the difference frame
        val ambient = GrayscaleFrame(
            FloatArray(camera.width * camera.height) { 5f / 255f },
            camera.width,
            camera.height
        )
        val diffFrame = grayFrame.subtract(ambient)

        // Detect blobs
        val detector = BlobDetector(
            brightnessThreshold = 0.15f,
            minBlobSize = 3
        )
        val blobs = detector.detect(diffFrame)

        // We turned on 3 fixtures, so we expect 3 blobs
        // (some may merge if close together, so allow some tolerance)
        assertTrue(
            blobs.size in 2..4,
            "Expected 2-4 detected blobs for 3 on-fixtures, got ${blobs.size}"
        )

        // All detected blobs should be within the image bounds
        for (blob in blobs) {
            assertTrue(blob.centroid.x >= 0f && blob.centroid.x < camera.width.toFloat(),
                "Blob centroid X (${blob.centroid.x}) should be within image width")
            assertTrue(blob.centroid.y >= 0f && blob.centroid.y < camera.height.toFloat(),
                "Blob centroid Y (${blob.centroid.y}) should be within image height")
        }
    }

    @Test
    fun visionPipeline_allFixturesOff_noBlobs() {
        val camera = SimulatedCamera(
            width = 320,
            height = 240,
            noiseLevel = 0f,
            ambientLevel = 5,
            blobRadius = 10f
        )

        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        // All fixtures off
        val frame = camera.generateFrame(rig.fixtures, emptySet())

        val pixels = FloatArray(frame.size) { (frame[it].toInt() and 0xFF) / 255f }
        val grayFrame = GrayscaleFrame(pixels, camera.width, camera.height)
        val ambient = GrayscaleFrame(
            FloatArray(camera.width * camera.height) { 5f / 255f },
            camera.width,
            camera.height
        )
        val diffFrame = grayFrame.subtract(ambient)

        val detector = BlobDetector(brightnessThreshold = 0.15f, minBlobSize = 3)
        val blobs = detector.detect(diffFrame)

        assertEquals(0, blobs.size, "No blobs expected when all fixtures are off")
    }

    @Test
    fun visionPipeline_spatialMapBuilder_producesFixture3D() {
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 0,
            blobRadius = 15f,
            cameraPosition = Vec3(0f, -6f, 1.5f),
            cameraTarget = Vec3(0f, 1f, 2.5f)
        )

        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)

        // Build a spatial map by projecting each fixture
        val builder = SpatialMapBuilder()
        for (fixture3D in rig.fixtures) {
            val projected = camera.projectToImage(fixture3D.position)
            if (projected.isVisible) {
                builder.addSingleCell(
                    fixture3D.fixture.fixtureId,
                    com.chromadmx.vision.detection.Coord2D(projected.x, projected.y)
                )
            }
        }
        val spatialMap = builder.build()

        // All 8 DJ fixtures should be visible
        assertEquals(8, spatialMap.fixturePositions.size,
            "All 8 DJ fixtures should be mapped")

        // Convert to Fixture3D with assigned z-heights
        val fixtureMap = rig.fixtures.associate { it.fixture.fixtureId to it.fixture }
        val zHeights = rig.fixtures.associate { it.fixture.fixtureId to 2.5f }
        val mapped3D = SpatialMapBuilder.toFixture3DList(
            spatialMap, fixtureMap, zHeights, camera.width, camera.height
        )

        assertEquals(8, mapped3D.size, "Should produce 8 Fixture3D instances")
        for (f in mapped3D) {
            assertEquals(2.5f, f.position.z, 0.001f, "Z-height should match assigned value")
            // Normalized positions should be in reasonable range
            assertTrue(f.position.x in -2f..2f,
                "Normalized X (${f.position.x}) should be in reasonable range")
            assertTrue(f.position.y in -2f..2f,
                "Normalized Y (${f.position.y}) should be in reasonable range")
        }
    }

    // ================================================================== //
    //  4. Simulation Manager Test (via DI module components)              //
    // ================================================================== //

    @Test
    fun simulationManager_rigHasFixturesWithPositions() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)

        assertTrue(rig.fixtureCount > 0, "Rig should have fixtures")
        for (fixture in rig.fixtures) {
            // Every fixture should have a valid 3D position
            val pos = fixture.position
            assertTrue(
                pos.x.isFinite() && pos.y.isFinite() && pos.z.isFinite(),
                "Fixture ${fixture.fixture.fixtureId} has non-finite position"
            )
        }
    }

    @Test
    fun simulationManager_nodesAreDiscoverable() = runTest {
        val bus = SimulatedNetworkBus()

        // Controller transport
        val controllerTransport = SimulatedNetwork()
        controllerTransport.connectToBus(bus)

        // Simulated node transport
        val nodeTransport = SimulatedNetwork()
        nodeTransport.connectToBus(bus)

        val node = SimulatedDmxNode(
            transport = nodeTransport,
            ipAddress = "192.168.1.100",
            shortName = "TestNode",
            longName = "Test Art-Net Node",
            universes = listOf(0, 1)
        )

        // Use processPacket() directly (avoids Dispatchers.Default timing issues in tests)
        val artPoll = buildArtPollPacket()
        val handled = node.processPacket(artPoll, "192.168.1.1")

        assertTrue(handled, "ArtPoll packet should be recognized and handled")
        assertTrue(node.pollCount > 0, "Node should have received ArtPoll")
        assertTrue(node.replyCount > 0, "Node should have sent ArtPollReply")

        // The node transport should have sent an ArtPollReply back via the bus,
        // and the controller should receive it
        val buffer = ByteArray(2048)
        val reply = controllerTransport.receive(buffer, timeoutMs = 1000)
        assertTrue(reply != null, "Controller should receive ArtPollReply")
    }

    @Test
    fun simulationManager_stopCleansUp() = runTest {
        val nodeTransport = SimulatedNetwork()
        val node = SimulatedDmxNode(
            transport = nodeTransport,
            ipAddress = "192.168.1.100",
            universes = listOf(0)
        )

        node.start()
        assertTrue(node.isRunning, "Node should be running after start")

        node.stop()
        assertTrue(!node.isRunning, "Node should stop after stop()")
    }

    @Test
    fun simulationManager_allPresetsCreateValidRigs() {
        for (preset in RigPreset.entries) {
            val rig = SimulatedFixtureRig(preset)
            assertTrue(rig.fixtureCount > 0, "$preset should have fixtures")
            assertTrue(rig.universeCount > 0, "$preset should use at least 1 universe")

            // Verify all fixtures have valid positions and unique IDs
            val ids = mutableSetOf<String>()
            for (fixture in rig.fixtures) {
                assertTrue(ids.add(fixture.fixture.fixtureId),
                    "$preset has duplicate fixture ID: ${fixture.fixture.fixtureId}")
                assertTrue(fixture.position.z >= 0f,
                    "$preset fixture ${fixture.fixture.fixtureId} has negative height")
            }
        }
    }

    @Test
    fun simulationManager_nodeReceivesDmxData() = runTest {
        val nodeTransport = SimulatedNetwork()

        val node = SimulatedDmxNode(
            transport = nodeTransport,
            ipAddress = "192.168.1.100",
            universes = listOf(0)
        )

        // Build ArtDmx data for universe 0
        val dmxData = ByteArray(512)
        dmxData[0] = 255.toByte()  // channel 0 = 255 (red)
        dmxData[1] = 128.toByte()  // channel 1 = 128 (green)
        dmxData[2] = 64.toByte()   // channel 2 = 64  (blue)

        val artDmxPacket = buildArtDmxPacket(universe = 0, data = dmxData)

        // Use processPacket() directly (avoids Dispatchers.Default timing issues in tests)
        val handled = node.processPacket(artDmxPacket, "192.168.1.1")

        assertTrue(handled, "ArtDmx packet should be recognized and handled")
        assertTrue(node.dmxCount > 0, "Node should have received ArtDmx")

        val receivedRgb = node.getColorAt(0, 0)
        assertTrue(receivedRgb != null, "Should have received DMX data for universe 0")
        assertEquals(255, receivedRgb.first, "Red channel")
        assertEquals(128, receivedRgb.second, "Green channel")
        assertEquals(64, receivedRgb.third, "Blue channel")
    }

    // ================================================================== //
    //  5. Beat-Synced Effect Test                                         //
    // ================================================================== //

    @Test
    fun beatSynced_strobeVariesWithBeatPhase() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = StrobeEffect(),
                params = EffectParams()
                    .with("color", Color.WHITE)
                    .with("dutyCycle", 0.5f),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // With beatPhase = 0.2 (below dutyCycle 0.5) -> strobe ON
        val onBeat = BeatState(bpm = 120f, beatPhase = 0.2f, barPhase = 0.05f, elapsed = 0.1f)
        val frameOn = engine.evaluateFrame(0f, onBeat)

        // With beatPhase = 0.8 (above dutyCycle 0.5) -> strobe OFF
        val offBeat = BeatState(bpm = 120f, beatPhase = 0.8f, barPhase = 0.2f, elapsed = 0.4f)
        val frameOff = engine.evaluateFrame(0f, offBeat)

        // ON frame should be bright (white)
        for (color in frameOn) {
            assertTrue(color.isNonBlack(), "Strobe should be ON when beatPhase < dutyCycle")
        }

        // OFF frame should be black
        for (color in frameOff) {
            assertEquals(0.0f, color.r, 0.001f)
            assertEquals(0.0f, color.g, 0.001f)
            assertEquals(0.0f, color.b, 0.001f)
        }
    }

    @Test
    fun beatSynced_chaseVariesWithTime() {
        // Use normalized-range fixtures so the Chase wrapping math works correctly
        val fixtures = (0 until 8).map { i ->
            Fixture3D(
                fixture = Fixture(
                    fixtureId = "chase-fix-$i",
                    name = "Chase Fix $i",
                    channelStart = i * 3,
                    channelCount = 3,
                    universeId = 0
                ),
                position = Vec3(x = i / 8f, y = 0f, z = 0f)
            )
        }
        val scope = TestScope()
        val engine = EffectEngine(scope, fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = Chase3DEffect(),
                params = EffectParams()
                    .with("axis", "x")
                    .with("speed", 3.0f)    // non-integer speed so wrapping doesn't alias
                    .with("color", Color.WHITE)
                    .with("tail", 0.3f),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // With speed=3.0: headPos at t=0 is 0.0, at t=0.1 is 0.3, at t=0.2 is 0.6
        val beat0 = BeatState(bpm = 120f, beatPhase = 0.0f, barPhase = 0.0f, elapsed = 0f)
        val beat1 = BeatState(bpm = 120f, beatPhase = 0.5f, barPhase = 0.125f, elapsed = 0.1f)
        val beat2 = BeatState(bpm = 120f, beatPhase = 0.0f, barPhase = 0.25f, elapsed = 0.2f)

        val frame0 = engine.evaluateFrame(0.0f, beat0)
        val frame1 = engine.evaluateFrame(0.1f, beat1)
        val frame2 = engine.evaluateFrame(0.2f, beat2)

        // Chase should move through the fixtures, producing different patterns
        assertTrue(
            frame0.zip(frame1).any { (a, b) -> a != b },
            "Chase at t=0 and t=0.1 should differ"
        )
        assertTrue(
            frame1.zip(frame2).any { (a, b) -> a != b },
            "Chase at t=0.1 and t=0.2 should differ"
        )
    }

    @Test
    fun beatSynced_tapTempoClock_producesBeatState() = runTest {
        // Controllable time source for deterministic testing
        var currentNanos = 0L
        val timeSource: () -> Long = { currentNanos }

        val scope = TestScope()
        val clock = TapTempoClock(
            scope = scope,
            timeSource = timeSource,
            updateIntervalMs = 16L
        )

        // Tap at 120 BPM = 500ms between taps = 500_000_000 nanos
        val intervalNanos = 500_000_000L

        clock.tap()
        currentNanos += intervalNanos
        clock.tap()
        currentNanos += intervalNanos
        clock.tap()
        currentNanos += intervalNanos
        clock.tap()

        // BPM should be approximately 120
        val bpm = clock.bpm.value
        assertTrue(bpm in 115f..125f, "Expected ~120 BPM from taps, got $bpm")

        // Beat state should be valid
        val state = clock.beatState.value
        assertTrue(state.bpm in 115f..125f, "BeatState BPM should match clock BPM")
        assertTrue(state.beatPhase in 0f..1f, "Beat phase should be in [0, 1]")
        assertTrue(state.barPhase in 0f..1f, "Bar phase should be in [0, 1]")

        clock.stop()
    }

    @Test
    fun beatSynced_tapTempoThenEngine_producesVaryingOutput() = runTest {
        var currentNanos = 0L
        val timeSource: () -> Long = { currentNanos }

        val scope = TestScope()
        val clock = TapTempoClock(
            scope = scope,
            timeSource = timeSource,
            updateIntervalMs = 16L
        )

        // Tap at 120 BPM
        val intervalNanos = 500_000_000L
        clock.tap()
        currentNanos += intervalNanos
        clock.tap()
        currentNanos += intervalNanos
        clock.tap()

        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val engineScope = TestScope()
        val engine = EffectEngine(engineScope, rig.fixtures)

        engine.effectStack.addLayer(
            EffectLayer(
                effect = StrobeEffect(),
                params = EffectParams()
                    .with("color", Color.WHITE)
                    .with("dutyCycle", 0.5f),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Strobe with beatPhase near 0 (ON)
        val onState = BeatState(bpm = 120f, beatPhase = 0.1f, barPhase = 0.025f, elapsed = 0f)
        val frameOn = engine.evaluateFrame(0f, onState)

        // Strobe with beatPhase near 1 (OFF)
        val offState = BeatState(bpm = 120f, beatPhase = 0.9f, barPhase = 0.225f, elapsed = 0.375f)
        val frameOff = engine.evaluateFrame(0f, offState)

        // Verify output varies with beat phase
        val onBright = frameOn.any { it.isNonBlack() }
        val offBright = frameOff.any { it.isNonBlack() }

        assertTrue(onBright, "Strobe should be ON at beatPhase=0.1")
        assertTrue(!offBright, "Strobe should be OFF at beatPhase=0.9")

        clock.stop()
    }

    // ================================================================== //
    //  6. Cross-Module Integration: Camera + Engine + BlobDetector        //
    // ================================================================== //

    @Test
    fun crossModule_engineOutputFeedsCameraAndBlobDetector() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        // Light up all fixtures with solid white
        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams().with("color", Color.WHITE),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )

        // Get engine output
        val colors = engine.evaluateFrame(0f, BeatState.IDLE)

        // Feed engine colors into the simulated camera
        val camera = SimulatedCamera(
            width = 640,
            height = 480,
            noiseLevel = 0f,
            ambientLevel = 5,
            blobRadius = 12f,
            cameraPosition = Vec3(0f, -6f, 1.5f),
            cameraTarget = Vec3(0f, 1f, 2.5f)
        )

        val fixtureStates = rig.fixtures.mapIndexed { i, f ->
            FixtureState(f, colors[i])
        }
        val frame = camera.generateFrame(fixtureStates)

        // Convert to GrayscaleFrame and run blob detection
        val pixels = FloatArray(frame.size) { (frame[it].toInt() and 0xFF) / 255f }
        val grayFrame = GrayscaleFrame(pixels, camera.width, camera.height)
        val ambient = GrayscaleFrame(
            FloatArray(camera.width * camera.height) { 5f / 255f },
            camera.width,
            camera.height
        )
        val diffFrame = grayFrame.subtract(ambient)

        val detector = BlobDetector(brightnessThreshold = 0.15f, minBlobSize = 3)
        val blobs = detector.detect(diffFrame)

        // All 8 fixtures are on white, so we should detect blobs
        // Some may merge if they're close together, but we should get at least 3
        assertTrue(
            blobs.size >= 3,
            "Expected at least 3 blobs for 8 white fixtures, got ${blobs.size}"
        )
    }

    @Test
    fun crossModule_festivalRig_engineProducesOutput() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        val scope = TestScope()
        val engine = EffectEngine(scope, rig.fixtures)

        // Multi-layer stack on a large rig
        engine.effectStack.addLayer(
            EffectLayer(
                effect = GradientSweep3DEffect(),
                params = EffectParams()
                    .with("axis", "z")
                    .with("speed", 1.0f)
                    .with("palette", listOf(Color.RED, Color.GREEN, Color.BLUE)),
                blendMode = BlendMode.NORMAL,
                opacity = 1.0f
            )
        )
        engine.effectStack.addLayer(
            EffectLayer(
                effect = PerlinNoise3DEffect(),
                params = EffectParams()
                    .with("scale", 0.5f)
                    .with("speed", 0.3f),
                blendMode = BlendMode.ADDITIVE,
                opacity = 0.3f
            )
        )

        val frame = engine.evaluateFrame(1.0f, BeatState.IDLE)

        // 108 fixtures should all have output
        assertEquals(108, frame.size, "Festival stage should output 108 fixture colors")

        // At least some should be non-black (gradient + perlin ensures variety)
        val litCount = frame.count { it.isNonBlack() }
        assertTrue(litCount > 50, "Expected most festival stage fixtures to be lit, got $litCount")
    }

    // ================================================================== //
    //  Art-Net packet builders (minimal valid packets for simulation)      //
    // ================================================================== //

    /**
     * Build a minimal valid ArtPoll packet.
     *
     * Art-Net header: "Art-Net\0" (8 bytes) + OpCode 0x2000 (2 bytes LE)
     * + ProtVer 14 (2 bytes BE) + Flags/Priority (2 bytes).
     */
    private fun buildArtPollPacket(): ByteArray {
        val packet = ByteArray(14)
        // Art-Net header
        val header = "Art-Net".encodeToByteArray()
        header.copyInto(packet, 0)
        packet[7] = 0  // null terminator

        // OpCode 0x2000 (ArtPoll) in little-endian
        packet[8] = 0x00
        packet[9] = 0x20

        // Protocol version 14 in big-endian
        packet[10] = 0x00
        packet[11] = 0x0E

        // Flags and priority (minimal)
        packet[12] = 0x00
        packet[13] = 0x00

        return packet
    }

    /**
     * Build a minimal valid ArtDmx packet.
     *
     * Art-Net header + OpCode 0x5000 + ProtVer 14 + Sequence + Physical +
     * Universe (2 bytes LE) + Length (2 bytes BE) + data.
     */
    private fun buildArtDmxPacket(universe: Int, data: ByteArray): ByteArray {
        val dataLen = data.size.coerceAtMost(512)
        val packet = ByteArray(18 + dataLen)

        // Art-Net header
        val header = "Art-Net".encodeToByteArray()
        header.copyInto(packet, 0)
        packet[7] = 0

        // OpCode 0x5000 (ArtDmx) in little-endian
        packet[8] = 0x00
        packet[9] = 0x50

        // Protocol version 14 in big-endian
        packet[10] = 0x00
        packet[11] = 0x0E

        // Sequence
        packet[12] = 0x00
        // Physical
        packet[13] = 0x00

        // Universe in little-endian
        packet[14] = (universe and 0xFF).toByte()
        packet[15] = ((universe shr 8) and 0xFF).toByte()

        // Data length in big-endian
        packet[16] = ((dataLen shr 8) and 0xFF).toByte()
        packet[17] = (dataLen and 0xFF).toByte()

        // DMX data
        data.copyInto(packet, 18, 0, dataLen)

        return packet
    }
}

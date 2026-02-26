package com.chromadmx.tempo.link

/**
 * Platform abstraction over the Ableton Link C++ SDK session.
 *
 * Each platform (Android/iOS) provides an `actual` implementation that bridges
 * to the native Link library. The shared Kotlin code interacts with Link
 * exclusively through the [LinkSessionApi] interface; this `expect class`
 * provides the concrete platform instances.
 *
 * ## Threading
 *
 * Link's C++ state is internally thread-safe (lock-free audio-thread reads).
 * The Kotlin layer may safely call any property/method from a coroutine without
 * additional synchronization.
 *
 * ## Lifecycle
 *
 * 1. Create a [LinkSession] instance.
 * 2. Call [enable] to join the Link mesh on the local network.
 * 3. Poll [bpm], [beatPhase], [barPhase], and [peerCount] at your desired rate.
 * 4. Call [disable] when the user turns Link off or the app backgrounds.
 *
 * ## Platform bridges
 *
 * ### Android (JNI)
 * - `System.loadLibrary("ableton_link_jni")`
 * - Native pointer stored as `Long`, passed to `external fun` wrappers.
 * - CMakeLists.txt in `shared/tempo/src/androidMain/cpp/` builds Link + JNI glue.
 *
 * ### iOS (cinterop / LinkKit)
 * - `.def` file references LinkKit ObjC headers or raw C++ headers.
 * - Kotlin/Native cinterop generates Kotlin bindings automatically.
 * - Configured via `cinterops { create("abletonLink") { ... } }` in build.gradle.kts.
 */
expect class LinkSession() : LinkSessionApi {
    override fun enable()
    override fun disable()
    override val isEnabled: Boolean
    override val peerCount: Int
    override val bpm: Double
    override val beatPhase: Double
    override val barPhase: Double
    override fun requestBpm(bpm: Double)
    override fun close()
}

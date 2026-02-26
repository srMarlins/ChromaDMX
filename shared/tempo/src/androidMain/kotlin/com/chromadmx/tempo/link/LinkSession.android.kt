package com.chromadmx.tempo.link

/**
 * Android actual for [LinkSession].
 *
 * Wraps JNI calls to the Ableton Link C++ SDK compiled as a shared library.
 *
 * ## Architecture
 *
 * The native library (`libableton_link_jni.so`) exposes a thin C++ wrapper around
 * the Link SDK. An opaque `Long` pointer represents the native `ableton::Link`
 * instance. All JNI methods accept this pointer as their first argument.
 *
 * ## JNI Native Methods
 *
 * The following native functions must be implemented in `link_jni.cpp`:
 *
 * ```c
 * JNIEXPORT jlong JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeCreate(JNIEnv*, jobject, jdouble);
 *
 * JNIEXPORT void JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeDestroy(JNIEnv*, jobject, jlong);
 *
 * JNIEXPORT void JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeSetEnabled(JNIEnv*, jobject, jlong, jboolean);
 *
 * JNIEXPORT jboolean JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeIsEnabled(JNIEnv*, jobject, jlong);
 *
 * JNIEXPORT jdouble JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeCaptureBpm(JNIEnv*, jobject, jlong);
 *
 * JNIEXPORT jdouble JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeCaptureBeatPhase(JNIEnv*, jobject, jlong, jdouble);
 *
 * JNIEXPORT jdouble JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeCaptureBarPhase(JNIEnv*, jobject, jlong, jdouble);
 *
 * JNIEXPORT jint JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeNumPeers(JNIEnv*, jobject, jlong);
 *
 * JNIEXPORT void JNICALL
 * Java_com_chromadmx_tempo_link_LinkSession_nativeRequestBpm(JNIEnv*, jobject, jlong, jdouble);
 * ```
 *
 * ## Build Setup
 *
 * 1. Clone Ableton Link SDK into `shared/tempo/src/androidMain/cpp/link/`.
 * 2. The CMakeLists.txt at `shared/tempo/src/androidMain/cpp/CMakeLists.txt`
 *    builds Link + JNI glue as `libableton_link_jni.so`.
 * 3. Wire CMake in the android build via `externalNativeBuild`.
 *
 * ## Current Status
 *
 * This is a **stub implementation** — the JNI native library is not yet built.
 * All methods return safe defaults (120 BPM, 0 phase, 0 peers). The enable/disable
 * methods track state locally but do not interact with the native SDK.
 *
 * When the Link SDK is integrated:
 * 1. Uncomment `System.loadLibrary("ableton_link_jni")` in the companion.
 * 2. Replace the stub property implementations with calls to the `external fun` methods.
 * 3. Store the native pointer from `nativeCreate()` and pass it to all other calls.
 */
actual class LinkSession actual constructor() : LinkSessionApi {

    // ---- JNI native method declarations ----
    // These map to the C++ functions in link_jni.cpp.
    // Uncomment when the native library is built.

    // private external fun nativeCreate(initialBpm: Double): Long
    // private external fun nativeDestroy(ptr: Long)
    // private external fun nativeSetEnabled(ptr: Long, enabled: Boolean)
    // private external fun nativeIsEnabled(ptr: Long): Boolean
    // private external fun nativeCaptureBpm(ptr: Long): Double
    // private external fun nativeCaptureBeatPhase(ptr: Long, quantum: Double): Double
    // private external fun nativeCaptureBarPhase(ptr: Long, quantum: Double): Double
    // private external fun nativeNumPeers(ptr: Long): Int
    // private external fun nativeRequestBpm(ptr: Long, bpm: Double)

    // ---- Stub state (replace with native pointer when SDK is integrated) ----

    /**
     * Native session pointer. Will hold the result of `nativeCreate()` once
     * the JNI library is loaded. Currently unused (stub mode).
     */
    @Suppress("unused")
    private var nativePtr: Long = 0L

    private var _enabled = false

    // ---- LinkSessionApi implementation (stub) ----

    actual override fun enable() {
        _enabled = true
        // TODO: When native library is available:
        // if (nativePtr == 0L) nativePtr = nativeCreate(DEFAULT_BPM)
        // nativeSetEnabled(nativePtr, true)
    }

    actual override fun disable() {
        _enabled = false
        // TODO: When native library is available:
        // if (nativePtr != 0L) nativeSetEnabled(nativePtr, false)
    }

    actual override val isEnabled: Boolean
        get() = _enabled
        // TODO: When native library is available:
        // get() = if (nativePtr != 0L) nativeIsEnabled(nativePtr) else false

    actual override val peerCount: Int
        get() = 0
        // TODO: When native library is available:
        // get() = if (nativePtr != 0L) nativeNumPeers(nativePtr) else 0

    actual override val bpm: Double
        get() = DEFAULT_BPM
        // TODO: When native library is available:
        // get() = if (nativePtr != 0L) nativeCaptureBpm(nativePtr) else DEFAULT_BPM

    actual override val beatPhase: Double
        get() = 0.0
        // TODO: When native library is available:
        // get() = if (nativePtr != 0L) nativeCaptureBeatPhase(nativePtr, BEAT_QUANTUM) else 0.0

    actual override val barPhase: Double
        get() = 0.0
        // TODO: When native library is available:
        // get() = if (nativePtr != 0L) nativeCaptureBarPhase(nativePtr, BAR_QUANTUM) else 0.0

    actual override fun requestBpm(bpm: Double) {
        // TODO: When native library is available:
        // if (nativePtr != 0L) nativeRequestBpm(nativePtr, bpm)
    }

    /**
     * Release native resources. Call when the session is no longer needed.
     */
    actual override fun close() {
        // TODO: When native library is available:
        // if (nativePtr != 0L) {
        //     nativeDestroy(nativePtr)
        //     nativePtr = 0L
        // }
        _enabled = false
    }

    companion object {
        private const val DEFAULT_BPM = 120.0

        /** Quantum of 1 beat for beat-phase calculation. */
        @Suppress("unused")
        private const val BEAT_QUANTUM = 1.0

        /** Quantum of 4 beats for bar-phase calculation (4/4 time). */
        @Suppress("unused")
        private const val BAR_QUANTUM = 4.0

        // TODO: Uncomment when native library is built:
        // init {
        //     System.loadLibrary("ableton_link_jni")
        // }
    }
}

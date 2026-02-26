/**
 * link_jni.cpp — JNI bridge between Kotlin LinkSession and Ableton Link C++ SDK.
 *
 * ## Architecture
 *
 * This file provides the native implementations for the JNI methods declared
 * in LinkSession.android.kt. Each function receives an opaque pointer (jlong)
 * that represents the native ableton::Link instance.
 *
 * ## Integration Steps
 *
 * When integrating the real Ableton Link SDK:
 *
 * 1. Clone Link SDK: `git clone https://github.com/Ableton/link.git link_sdk`
 * 2. Uncomment the `#include <ableton/Link.hpp>` and all TODO blocks below.
 * 3. Replace the stub return values with actual Link API calls.
 * 4. Uncomment the AbletonLink target in CMakeLists.txt.
 * 5. Build with NDK — the library will be packaged into the APK automatically.
 *
 * ## Threading
 *
 * Ableton Link is internally thread-safe for audio-thread reads. The JNI
 * methods here can be called from any thread (they are called from a Kotlin
 * coroutine polling loop at ~60-100Hz).
 *
 * ## Memory Management
 *
 * - nativeCreate() allocates a Link instance on the heap, returns its address as jlong.
 * - nativeDestroy() deletes the instance. Must be called when the session is discarded.
 * - All other methods dereference the pointer — caller must ensure it's valid.
 */

#include <jni.h>
#include <cmath>
// TODO: Uncomment when Link SDK is available:
// #include <ableton/Link.hpp>

// TODO: Uncomment when Link SDK is available:
// namespace {
// /**
//  * Common helper for computing normalized phase from the Link timeline.
//  *
//  * Captures the app session state, reads the current beat position, and
//  * normalizes it into [0, 1) relative to the given quantum.
//  *
//  * @param link    Pointer to the ableton::Link instance.
//  * @param quantum Number of beats per phase cycle (1.0 for beat, 4.0 for bar).
//  * @return Phase in [0.0, 1.0).
//  */
// double calculatePhase(ableton::Link* link, double quantum) {
//     auto state = link->captureAppSessionState();
//     auto hostTime = link->clock().micros();
//     double beats = state.beatAtTime(hostTime, quantum);
//     double phase = std::fmod(beats, quantum);
//     if (phase < 0.0) phase += quantum;
//     return phase / quantum;
// }
// } // anonymous namespace

// JNI function naming: Java_<package>_<class>_<method>
// Package: com.chromadmx.tempo.link
// Class:   LinkSession

extern "C" {

/**
 * Create a new Ableton Link session at the given initial tempo.
 *
 * @param initialBpm Initial tempo in BPM (typically 120.0).
 * @return Opaque pointer to the native Link instance (cast to jlong).
 */
JNIEXPORT jlong JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeCreate(
    JNIEnv* /*env*/, jobject /*thiz*/, jdouble initialBpm)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = new ableton::Link(initialBpm);
    // return reinterpret_cast<jlong>(link);

    (void)initialBpm;
    return 0L; // Stub: no native instance
}

/**
 * Destroy the native Link instance and free resources.
 *
 * @param ptr Opaque pointer from nativeCreate().
 */
JNIEXPORT void JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeDestroy(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // delete link;

    (void)ptr;
}

/**
 * Enable or disable the Link session (joins/leaves the network mesh).
 *
 * @param ptr     Opaque pointer from nativeCreate().
 * @param enabled True to join the mesh, false to leave.
 */
JNIEXPORT void JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeSetEnabled(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr, jboolean enabled)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // link->enable(enabled);

    (void)ptr;
    (void)enabled;
}

/**
 * Check if the Link session is currently enabled.
 *
 * @param ptr Opaque pointer from nativeCreate().
 * @return True if the session is active.
 */
JNIEXPORT jboolean JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeIsEnabled(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // return static_cast<jboolean>(link->isEnabled());

    (void)ptr;
    return JNI_FALSE; // Stub
}

/**
 * Capture the current tempo from the Link session timeline.
 *
 * Uses captureAppSessionState() for non-audio-thread access.
 *
 * @param ptr Opaque pointer from nativeCreate().
 * @return Current tempo in BPM.
 */
JNIEXPORT jdouble JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeCaptureBpm(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // auto state = link->captureAppSessionState();
    // return state.tempo();

    (void)ptr;
    return 120.0; // Stub: default BPM
}

/**
 * Capture the current beat phase from the Link timeline.
 *
 * Phase is computed as: beats % quantum / quantum, giving a value in [0, 1).
 *
 * @param ptr     Opaque pointer from nativeCreate().
 * @param quantum The quantum for phase calculation (1.0 for beat phase).
 * @return Phase value in [0.0, 1.0).
 */
JNIEXPORT jdouble JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeCaptureBeatPhase(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr, jdouble quantum)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // return calculatePhase(link, quantum);

    (void)ptr;
    (void)quantum;
    return 0.0; // Stub
}

/**
 * Capture the current bar phase from the Link timeline.
 *
 * @param ptr     Opaque pointer from nativeCreate().
 * @param quantum The quantum for phase calculation (4.0 for bar phase in 4/4).
 * @return Phase value in [0.0, 1.0).
 */
JNIEXPORT jdouble JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeCaptureBarPhase(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr, jdouble quantum)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // return calculatePhase(link, quantum);

    (void)ptr;
    (void)quantum;
    return 0.0; // Stub
}

/**
 * Return the number of peers currently connected to this Link session.
 *
 * @param ptr Opaque pointer from nativeCreate().
 * @return Number of connected peers (0 if none).
 */
JNIEXPORT jint JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeNumPeers(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // return static_cast<jint>(link->numPeers());

    (void)ptr;
    return 0; // Stub
}

/**
 * Request a tempo change that will be propagated to all peers.
 *
 * @param ptr Opaque pointer from nativeCreate().
 * @param bpm Desired tempo in BPM.
 */
JNIEXPORT void JNICALL
Java_com_chromadmx_tempo_link_LinkSession_nativeRequestBpm(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr, jdouble bpm)
{
    // TODO: Replace with real Link SDK call:
    // auto* link = reinterpret_cast<ableton::Link*>(ptr);
    // auto state = link->captureAppSessionState();
    // auto hostTime = link->clock().micros();
    // state.setTempo(bpm, hostTime);
    // link->commitAppSessionState(state);

    (void)ptr;
    (void)bpm;
}

} // extern "C"

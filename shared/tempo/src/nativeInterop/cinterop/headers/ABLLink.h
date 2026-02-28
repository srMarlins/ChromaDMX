/**
 * Stub header for Ableton LinkKit SDK.
 *
 * Provides type declarations and function signatures so the Kotlin/Native
 * cinterop tool can generate bindings. The actual LinkKit.framework must
 * be present at link time for Ableton Link features to work at runtime.
 *
 * Download the real SDK from: https://www.ableton.com/en/link/sdk/
 */

#pragma once

#include <stdint.h>
#include <stdbool.h>
#include <mach/mach_time.h>

#ifdef __cplusplus
extern "C" {
#endif

/** Opaque reference to a Link session. */
typedef struct ABLLink* ABLLinkRef;

/** Opaque reference to a captured session state. */
typedef struct ABLLinkSessionState* ABLLinkSessionStateRef;

/** Create a new Link session with the given initial tempo (BPM). */
ABLLinkRef ABLLinkNew(double bpm);

/** Destroy a Link session and free resources. */
void ABLLinkDelete(ABLLinkRef ref);

/** Activate or deactivate Link on the network. */
void ABLLinkSetActive(ABLLinkRef ref, bool active);

/** Whether Link is currently enabled. */
bool ABLLinkIsEnabled(ABLLinkRef ref);

/** Number of connected peers. */
uint64_t ABLLinkGetNumPeers(ABLLinkRef ref);

/** Capture the current app session state (lock-free, audio-thread safe). */
ABLLinkSessionStateRef ABLLinkCaptureAppSessionState(ABLLinkRef ref);

/** Commit a modified session state back to the Link session. */
void ABLLinkCommitAppSessionState(ABLLinkRef ref, ABLLinkSessionStateRef state);

/** Get the tempo (BPM) from a captured session state. */
double ABLLinkGetTempo(ABLLinkSessionStateRef state);

/** Set the tempo (BPM) at the given host time. */
void ABLLinkSetTempo(ABLLinkSessionStateRef state, double bpm, uint64_t hostTime);

/** Get the beat position at the given host time and quantum. */
double ABLLinkGetBeatAtTime(ABLLinkSessionStateRef state, uint64_t hostTime, double quantum);

#ifdef __cplusplus
}
#endif

#include "../../../shared/tempo/src/nativeInterop/cinterop/headers/ABLLink.h"

ABLLinkRef ABLLinkNew(double bpm) {
    (void)bpm;
    return NULL;
}

void ABLLinkDelete(ABLLinkRef ref) {
    (void)ref;
}

void ABLLinkSetActive(ABLLinkRef ref, bool active) {
    (void)ref;
    (void)active;
}

bool ABLLinkIsEnabled(ABLLinkRef ref) {
    (void)ref;
    return false;
}

uint64_t ABLLinkGetNumPeers(ABLLinkRef ref) {
    (void)ref;
    return 0;
}

ABLLinkSessionStateRef ABLLinkCaptureAppSessionState(ABLLinkRef ref) {
    (void)ref;
    return NULL;
}

void ABLLinkCommitAppSessionState(ABLLinkRef ref, ABLLinkSessionStateRef state) {
    (void)ref;
    (void)state;
}

double ABLLinkGetTempo(ABLLinkSessionStateRef state) {
    (void)state;
    return 120.0;
}

void ABLLinkSetTempo(ABLLinkSessionStateRef state, double bpm, uint64_t hostTime) {
    (void)state;
    (void)bpm;
    (void)hostTime;
}

double ABLLinkGetBeatAtTime(ABLLinkSessionStateRef state, uint64_t hostTime, double quantum) {
    (void)state;
    (void)hostTime;
    (void)quantum;
    return 0.0;
}

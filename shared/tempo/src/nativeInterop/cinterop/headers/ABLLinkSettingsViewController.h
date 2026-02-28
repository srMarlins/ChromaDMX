/**
 * Stub header for Ableton LinkKit settings UI.
 *
 * Provides a minimal declaration so cinterop can generate bindings.
 * The actual LinkKit.framework is required at link time.
 */

#pragma once

#import <UIKit/UIKit.h>

typedef struct ABLLink* ABLLinkRef;

/** Returns a UIViewController for Link settings UI. */
UIViewController* ABLLinkSettingsViewController(ABLLinkRef ref);

package com.chromadmx.ui.components.network

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun systemTimeMs(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

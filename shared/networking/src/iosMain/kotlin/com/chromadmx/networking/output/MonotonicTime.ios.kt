package com.chromadmx.networking.output

import platform.Foundation.NSProcessInfo

internal actual fun monotonicTimeMs(): Long =
    (NSProcessInfo.processInfo.systemUptime * 1000).toLong()

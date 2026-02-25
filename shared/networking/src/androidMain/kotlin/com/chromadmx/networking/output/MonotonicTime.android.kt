package com.chromadmx.networking.output

internal actual fun monotonicTimeMs(): Long =
    System.nanoTime() / 1_000_000L

package com.chromadmx.ui.util

/** Platform-specific epoch millis for health-check delta calculations. */
internal expect fun currentTimeMillis(): Long

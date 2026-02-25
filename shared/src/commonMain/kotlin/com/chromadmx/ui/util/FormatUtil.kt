package com.chromadmx.ui.util

import kotlin.math.abs

/** Format a float to one decimal place without platform-specific formatting. */
internal fun formatFloat(value: Float): String {
    val intPart = value.toInt()
    val fracPart = ((value - intPart) * 10).toInt().let { abs(it) }
    return if (value < 0 && intPart == 0) "-0.$fracPart" else "$intPart.$fracPart"
}

package com.chromadmx

class Greeting {
    private val platform = Platform()

    fun greet(): String {
        return "ChromaDMX running on ${platform.name}"
    }
}

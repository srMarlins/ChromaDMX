package com.chromadmx.agent.config

actual class ApiKeyProvider actual constructor() {
    actual fun getAnthropicKey(): String? {
        return System.getenv("ANTHROPIC_API_KEY")
    }
}

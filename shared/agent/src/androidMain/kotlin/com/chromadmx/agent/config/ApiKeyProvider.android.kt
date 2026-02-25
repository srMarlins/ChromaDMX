package com.chromadmx.agent.config

actual class ApiKeyProvider actual constructor() {
    actual fun getAnthropicKey(): String? {
        return System.getenv("ANTHROPIC_API_KEY")
    }

    actual fun getGoogleKey(): String? {
        return System.getenv("GOOGLE_API_KEY")
    }
}

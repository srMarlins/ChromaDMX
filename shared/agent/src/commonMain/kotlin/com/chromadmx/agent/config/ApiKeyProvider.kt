package com.chromadmx.agent.config

/**
 * Platform-specific provider for LLM API keys.
 *
 * Android reads from system environment; iOS reads from NSProcessInfo.
 */
expect class ApiKeyProvider() {
    fun getAnthropicKey(): String?
    fun getGoogleKey(): String?
}

package com.chromadmx.agent.config

/**
 * Platform-specific provider for the Anthropic API key.
 *
 * Android reads from system environment; iOS reads from NSProcessInfo.
 */
expect class ApiKeyProvider() {
    fun getAnthropicKey(): String?
}

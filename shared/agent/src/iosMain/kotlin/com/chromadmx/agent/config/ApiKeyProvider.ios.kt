package com.chromadmx.agent.config

import platform.Foundation.NSProcessInfo

actual class ApiKeyProvider actual constructor() {
    actual fun getAnthropicKey(): String? {
        return NSProcessInfo.processInfo.environment["ANTHROPIC_API_KEY"] as? String
    }
}

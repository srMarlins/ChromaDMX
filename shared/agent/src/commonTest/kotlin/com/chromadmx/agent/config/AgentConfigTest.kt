package com.chromadmx.agent.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentConfigTest {
    @Test
    fun defaultConfigHasReasonableDefaults() {
        val config = AgentConfig()
        assertEquals(30, config.maxIterations)
        assertEquals(0.7f, config.temperature)
        assertEquals("gemini_2_5_flash", config.modelId)
        assertEquals(50, config.historyCompressionThreshold)
        assertFalse(config.isAvailable)
    }

    @Test
    fun configWithApiKeyIsAvailable() {
        val config = AgentConfig(apiKey = "test-key-123")
        assertTrue(config.isAvailable)
    }

    @Test
    fun configWithBlankApiKeyIsNotAvailable() {
        val config = AgentConfig(apiKey = "  ")
        assertFalse(config.isAvailable)
    }

    @Test
    fun configWithEmptyApiKeyIsNotAvailable() {
        val config = AgentConfig(apiKey = "")
        assertFalse(config.isAvailable)
    }

    @Test
    fun configCopiesCorrectly() {
        val config = AgentConfig(apiKey = "key-1", maxIterations = 50, temperature = 0.9f)
        val copy = config.copy(apiKey = "key-2")
        assertEquals("key-2", copy.apiKey)
        assertEquals(50, copy.maxIterations)
        assertEquals(0.9f, copy.temperature)
    }

    @Test
    fun configModelIdCanBeOverridden() {
        val config = AgentConfig(modelId = "haiku_4_5")
        assertEquals("haiku_4_5", config.modelId)
    }

    @Test
    fun configHistoryCompressionCanBeOverridden() {
        val config = AgentConfig(historyCompressionThreshold = 100)
        assertEquals(100, config.historyCompressionThreshold)
    }

    @Test
    fun geminiModelDetectedAsGoogle() {
        val config = AgentConfig(modelId = "gemini_2_5_flash")
        assertTrue(config.isGoogleModel)
    }

    @Test
    fun anthropicModelNotDetectedAsGoogle() {
        val config = AgentConfig(modelId = "sonnet_4_5")
        assertFalse(config.isGoogleModel)
    }
}

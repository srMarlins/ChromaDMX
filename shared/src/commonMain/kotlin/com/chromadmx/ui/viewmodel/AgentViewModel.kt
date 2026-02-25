package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.ChatMessage
import com.chromadmx.agent.LightingAgent
import com.chromadmx.agent.ToolCallRecord
import com.chromadmx.agent.pregen.PreGenProgress
import com.chromadmx.agent.pregen.PreGenerationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Agent screen.
 *
 * Delegates to the real [LightingAgent] for conversation and tool dispatch,
 * and to [PreGenerationService] for batch scene generation.
 */
class AgentViewModel(
    private val agent: LightingAgent,
    private val preGenService: PreGenerationService,
    private val scope: CoroutineScope,
) {
    val messages: StateFlow<List<ChatMessage>> = agent.conversationHistory

    val isProcessing: StateFlow<Boolean> = agent.isProcessing

    val toolCallsInFlight: StateFlow<List<ToolCallRecord>> = agent.toolCallsInFlight

    val isAgentAvailable: Boolean get() = agent.isAvailable

    val preGenProgress: StateFlow<PreGenProgress> = preGenService.progress

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        scope.launch {
            agent.send(text)
        }
    }

    fun dispatchTool(toolName: String, argsJson: String = "{}") {
        scope.launch {
            agent.dispatchTool(toolName, argsJson)
        }
    }

    fun generateScenes(genre: String, count: Int) {
        scope.launch {
            preGenService.generate(genre, count)
        }
    }

    fun cancelGeneration() {
        preGenService.cancel()
    }

    fun clearHistory() {
        agent.clearHistory()
    }

    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
    }
}

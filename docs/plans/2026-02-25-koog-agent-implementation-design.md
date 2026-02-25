# Koog Agent Implementation Design

**Date**: 2026-02-25
**Status**: Approved
**Scope**: Replace stubbed LLM integration with real Koog SDK wiring

## Problem

The agent module has a well-tested tool dispatch layer (17 tools, 4 controllers, scene management) but the LLM integration is completely stubbed — `LightingAgent.send()` returns `"LLM integration pending (Koog SDK)"`. The custom `ToolRegistry` does manual JSON parsing instead of using Koog's type-safe tool system.

## Solution

Wire the Koog SDK properly using:
- **Class-based `SimpleTool<Args>`** for all 17 tools (KMP-compatible)
- **ReAct strategy** for autonomous multi-step reasoning + tool calling
- **Anthropic executor** (`simpleAnthropicExecutor`) targeting `Sonnet_4_5`
- **History compression** (`WholeHistory`) for long sessions
- **Event handlers** for UI observability of tool calls in flight

## Architecture

### Dependencies

```
koog-agents (umbrella) → agents-core, agents-tools, Anthropic client,
                          memory, event handler, tracing
koog-agents-ext        → reActStrategy, chatAgentStrategy
```

Both published to Maven Central at version `0.6.3`.

### Tool Migration

Each of the 17 existing tool classes gets converted from the custom pattern to Koog's `SimpleTool<Args>`:

- Extend `SimpleTool<Args>` with `argsSerializer`, `name`, `description`
- Add `@property:LLMDescription` to all `Args` fields
- Make `execute()` suspend (already compatible)
- Remove manual JSON dispatch from ToolRegistry

The custom `ToolRegistry` class is replaced with a factory function that returns Koog's `ToolRegistry`.

### Strategy

Custom ReAct-based strategy graph with history compression:

```
nodeStart → callLLM → [assistant message] → nodeFinish
                     → [tool call] → executeTool → [history too long?]
                                                       yes → compress → sendResult
                                                       no  → sendResult
                     sendResult → [assistant message] → nodeFinish
                                → [tool call] → executeTool (loop)
```

Compression triggers when message count exceeds threshold (default 50).

### LightingAgent Rewrite

- Creates `simpleAnthropicExecutor` when API key is available
- Wraps Koog `AIAgent` with the custom ReAct strategy
- Exposes `conversationHistory` and `toolCallsInFlight` as `StateFlow`
- Event handlers bridge Koog lifecycle events to StateFlow emissions
- `dispatchTool()` preserved for direct programmatic access (bypasses LLM)

### Offline Mode

When no API key: no executor/agent created, `send()` returns helpful message, `dispatchTool()` works normally, `PreGenerationService` works for demos. Zero Koog overhead.

### Configuration

`AgentConfig` extended with:
- `modelId: String` — defaults to `"sonnet_4_5"`, maps to `AnthropicModels`
- `historyCompressionThreshold: Int` — message count before compression (default 50)

### Event Tracking

Install `EventHandler` feature on the Koog agent:
- `onToolCallStarting` → emit tool name + args to `_toolCallsInFlight`
- `onToolCallCompleted` → update tool result, remove from in-flight
- `onAgentExecutionFailed` → emit error to conversation history

### UI Integration

`AgentViewModel` adapts:
- `sendMessage()` calls `agent.send()` → runs full Koog agent loop autonomously
- Tool calls visible in real-time via `toolCallsInFlight` StateFlow
- `dispatchTool()` stays for direct UI buttons

## Files Changed

| File | Change |
|------|--------|
| `libs.versions.toml` | Clean up Koog deps |
| `shared/agent/build.gradle.kts` | Add koog-agents, koog-agents-ext |
| 6 tool files (17 tools total) | Extend `SimpleTool<Args>` |
| `ToolRegistry.kt` | Replace with Koog registry factory |
| `LightingAgent.kt` | Wire Koog AIAgent |
| `AgentConfig.kt` | Add model + compression config |
| `AgentSystemPrompt.kt` | Enhance for Koog prompt DSL |
| `ChatMessage.kt` | Enrich with tool call tracking |
| `AgentModule.kt` | Rewire Koin for new architecture |
| `AgentViewModel.kt` | Adapt to enriched flow |
| Test files | Update for new interfaces |

## Key Decisions

- **Sonnet 4.5** default: best agent/tool-calling performance per dollar
- **ReAct over plain chat**: reasoning step improves multi-tool scene building
- **SimpleTool not Tool**: all tools return strings, no need for custom result types
- **WholeHistory compression**: simplest strategy, sufficient for bounded domain
- **Offline-first preserved**: tools work without LLM, Koog is optional runtime

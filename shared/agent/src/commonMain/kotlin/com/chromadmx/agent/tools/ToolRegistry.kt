package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.controller.NetworkController
import com.chromadmx.agent.controller.StateController
import com.chromadmx.agent.scene.SceneStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registry of all agent tools.
 *
 * Maps tool names to their execution functions and provides a JSON-based
 * dispatch interface. Tools are registered in a single map, so tool names
 * and dispatch logic are always in sync.
 */
class ToolRegistry(
    private val engineController: EngineController,
    private val networkController: NetworkController,
    private val fixtureController: FixtureController,
    private val stateController: StateController,
    private val sceneStore: SceneStore
) {
    // Tool instances
    private val setEffectTool = SetEffectTool(engineController)
    private val setBlendModeTool = SetBlendModeTool(engineController)
    private val setMasterDimmerTool = SetMasterDimmerTool(engineController)
    private val setColorPaletteTool = SetColorPaletteTool(engineController)
    private val setTempoMultiplierTool = SetTempoMultiplierTool(engineController)
    private val createSceneTool = CreateSceneTool(engineController, sceneStore)
    private val loadSceneTool = LoadSceneTool(engineController, sceneStore)
    private val scanNetworkTool = ScanNetworkTool(networkController)
    private val getNodeStatusTool = GetNodeStatusTool(networkController)
    private val configureNodeTool = ConfigureNodeTool(networkController)
    private val diagnoseConnectionTool = DiagnoseConnectionTool(networkController)
    private val listFixturesTool = ListFixturesTool(fixtureController)
    private val fireFixtureTool = FireFixtureTool(fixtureController)
    private val setFixtureGroupTool = SetFixtureGroupTool(fixtureController)
    private val getEngineStateTool = GetEngineStateTool(stateController)
    private val getBeatStateTool = GetBeatStateTool(stateController)
    private val getNetworkStateTool = GetNetworkStateTool(stateController)

    /** Single source of truth: tool name -> dispatch function. */
    private val tools: Map<String, suspend (JsonObject) -> String> = mapOf(
        "setEffect" to { args ->
            val layer = args["layer"]?.jsonPrimitive?.int ?: 0
            val effectId = args["effectId"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'effectId'"
            val params = args["params"]?.jsonObject?.mapValues {
                it.value.jsonPrimitive.float
            } ?: emptyMap()
            setEffectTool.execute(SetEffectTool.Args(layer, effectId, params))
        },
        "setBlendMode" to { args ->
            val layer = args["layer"]?.jsonPrimitive?.int ?: 0
            val mode = args["mode"]?.jsonPrimitive?.content ?: "NORMAL"
            setBlendModeTool.execute(SetBlendModeTool.Args(layer, mode))
        },
        "setMasterDimmer" to { args ->
            val value = args["value"]?.jsonPrimitive?.float ?: 1.0f
            setMasterDimmerTool.execute(SetMasterDimmerTool.Args(value))
        },
        "setColorPalette" to { args ->
            val colors = args["colors"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            setColorPaletteTool.execute(SetColorPaletteTool.Args(colors))
        },
        "setTempoMultiplier" to { args ->
            val multiplier = args["multiplier"]?.jsonPrimitive?.float ?: 1.0f
            setTempoMultiplierTool.execute(SetTempoMultiplierTool.Args(multiplier))
        },
        "createScene" to { args ->
            val name = args["name"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'name'"
            createSceneTool.execute(CreateSceneTool.Args(name))
        },
        "loadScene" to { args ->
            val name = args["name"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'name'"
            loadSceneTool.execute(LoadSceneTool.Args(name))
        },
        "scanNetwork" to { _ -> scanNetworkTool.execute() },
        "getNodeStatus" to { args ->
            val nodeId = args["nodeId"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'nodeId'"
            getNodeStatusTool.execute(GetNodeStatusTool.Args(nodeId))
        },
        "configureNode" to { args ->
            val nodeId = args["nodeId"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'nodeId'"
            val universe = args["universe"]?.jsonPrimitive?.int ?: 0
            val startAddress = args["startAddress"]?.jsonPrimitive?.int ?: 1
            configureNodeTool.execute(ConfigureNodeTool.Args(nodeId, universe, startAddress))
        },
        "diagnoseConnection" to { args ->
            val nodeId = args["nodeId"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'nodeId'"
            diagnoseConnectionTool.execute(DiagnoseConnectionTool.Args(nodeId))
        },
        "listFixtures" to { _ -> listFixturesTool.execute() },
        "fireFixture" to { args ->
            val fixtureId = args["fixtureId"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'fixtureId'"
            val colorHex = args["colorHex"]?.jsonPrimitive?.content ?: "#FFFFFF"
            fireFixtureTool.execute(FireFixtureTool.Args(fixtureId, colorHex))
        },
        "setFixtureGroup" to { args ->
            val groupName = args["groupName"]?.jsonPrimitive?.content
                ?: return@to "Error: missing required parameter 'groupName'"
            val fixtureIds = args["fixtureIds"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            setFixtureGroupTool.execute(SetFixtureGroupTool.Args(groupName, fixtureIds))
        },
        "getEngineState" to { _ -> getEngineStateTool.execute() },
        "getBeatState" to { _ -> getBeatStateTool.execute() },
        "getNetworkState" to { _ -> getNetworkStateTool.execute() },
    )

    /** All registered tool names — derived from the dispatch map. */
    val toolNames: List<String> get() = tools.keys.toList()

    /**
     * Dispatch a tool call by name with JSON arguments.
     *
     * @param toolName The tool name to dispatch to.
     * @param argsJson JSON string of the tool arguments.
     * @return The tool's response string.
     */
    suspend fun dispatch(toolName: String, argsJson: String = "{}"): String {
        return try {
            val args = if (argsJson.isBlank() || argsJson == "{}") {
                JsonObject(emptyMap())
            } else {
                json.parseToJsonElement(argsJson).jsonObject
            }
            val handler = tools[toolName]
                ?: return "Unknown tool: '$toolName'. Available: ${toolNames.joinToString(", ")}"
            handler(args)
        } catch (e: Exception) {
            "Error executing tool '$toolName': ${e.message}"
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}

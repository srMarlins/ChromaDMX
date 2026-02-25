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
 * dispatch interface. This is the adapter layer that can be wired to Koog
 * or any other agent framework.
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

    /** All registered tool names. */
    val toolNames: List<String> = listOf(
        "setEffect", "setBlendMode", "setMasterDimmer", "setColorPalette",
        "setTempoMultiplier", "createScene", "loadScene",
        "scanNetwork", "getNodeStatus", "configureNode", "diagnoseConnection",
        "listFixtures", "fireFixture", "setFixtureGroup",
        "getEngineState", "getBeatState", "getNetworkState"
    )

    /**
     * Dispatch a tool call by name with JSON arguments.
     *
     * @param toolName The tool name to dispatch to.
     * @param argsJson JSON string of the tool arguments.
     * @return The tool's response string.
     */
    suspend fun dispatch(toolName: String, argsJson: String = "{}"): String {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val args = if (argsJson.isBlank() || argsJson == "{}") {
                JsonObject(emptyMap())
            } else {
                json.parseToJsonElement(argsJson).jsonObject
            }
            dispatchParsed(toolName, args)
        } catch (e: Exception) {
            "Error executing tool '$toolName': ${e.message}"
        }
    }

    private suspend fun dispatchParsed(toolName: String, args: JsonObject): String {
        return when (toolName) {
            "setEffect" -> {
                val layer = args["layer"]?.jsonPrimitive?.int ?: 0
                val effectId = args["effectId"]?.jsonPrimitive?.content ?: ""
                val params = args["params"]?.jsonObject?.mapValues {
                    it.value.jsonPrimitive.float
                } ?: emptyMap()
                setEffectTool.execute(SetEffectTool.Args(layer, effectId, params))
            }
            "setBlendMode" -> {
                val layer = args["layer"]?.jsonPrimitive?.int ?: 0
                val mode = args["mode"]?.jsonPrimitive?.content ?: "NORMAL"
                setBlendModeTool.execute(SetBlendModeTool.Args(layer, mode))
            }
            "setMasterDimmer" -> {
                val value = args["value"]?.jsonPrimitive?.float ?: 1.0f
                setMasterDimmerTool.execute(SetMasterDimmerTool.Args(value))
            }
            "setColorPalette" -> {
                val colors = args["colors"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                setColorPaletteTool.execute(SetColorPaletteTool.Args(colors))
            }
            "setTempoMultiplier" -> {
                val multiplier = args["multiplier"]?.jsonPrimitive?.float ?: 1.0f
                setTempoMultiplierTool.execute(SetTempoMultiplierTool.Args(multiplier))
            }
            "createScene" -> {
                val name = args["name"]?.jsonPrimitive?.content ?: ""
                createSceneTool.execute(CreateSceneTool.Args(name))
            }
            "loadScene" -> {
                val name = args["name"]?.jsonPrimitive?.content ?: ""
                loadSceneTool.execute(LoadSceneTool.Args(name))
            }
            "scanNetwork" -> scanNetworkTool.execute()
            "getNodeStatus" -> {
                val nodeId = args["nodeId"]?.jsonPrimitive?.content ?: ""
                getNodeStatusTool.execute(GetNodeStatusTool.Args(nodeId))
            }
            "configureNode" -> {
                val nodeId = args["nodeId"]?.jsonPrimitive?.content ?: ""
                val universe = args["universe"]?.jsonPrimitive?.int ?: 0
                val startAddress = args["startAddress"]?.jsonPrimitive?.int ?: 1
                configureNodeTool.execute(ConfigureNodeTool.Args(nodeId, universe, startAddress))
            }
            "diagnoseConnection" -> {
                val nodeId = args["nodeId"]?.jsonPrimitive?.content ?: ""
                diagnoseConnectionTool.execute(DiagnoseConnectionTool.Args(nodeId))
            }
            "listFixtures" -> listFixturesTool.execute()
            "fireFixture" -> {
                val fixtureId = args["fixtureId"]?.jsonPrimitive?.content ?: ""
                val colorHex = args["colorHex"]?.jsonPrimitive?.content ?: "#FFFFFF"
                fireFixtureTool.execute(FireFixtureTool.Args(fixtureId, colorHex))
            }
            "setFixtureGroup" -> {
                val groupName = args["groupName"]?.jsonPrimitive?.content ?: ""
                val fixtureIds = args["fixtureIds"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                setFixtureGroupTool.execute(SetFixtureGroupTool.Args(groupName, fixtureIds))
            }
            "getEngineState" -> getEngineStateTool.execute()
            "getBeatState" -> getBeatStateTool.execute()
            "getNetworkState" -> getNetworkStateTool.execute()
            else -> "Unknown tool: '$toolName'. Available: ${toolNames.joinToString(", ")}"
        }
    }
}

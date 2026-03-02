package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.ToolRegistry
import com.chromadmx.agent.controller.EngineController
import com.chromadmx.agent.controller.FixtureController
import com.chromadmx.agent.controller.NetworkController
import com.chromadmx.agent.controller.StateController
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.wled.WledApiClient
import com.chromadmx.wled.WledDeviceRegistry

/**
 * Build the Koog ToolRegistry containing all lighting agent tools.
 *
 * WLED tools are conditionally registered when both [wledApiClient] and
 * [wledDeviceRegistry] are provided.
 */
fun buildToolRegistry(
    engineController: EngineController,
    networkController: NetworkController,
    fixtureController: FixtureController,
    stateController: StateController,
    presetLibrary: PresetLibrary,
    wledApiClient: WledApiClient? = null,
    wledDeviceRegistry: WledDeviceRegistry? = null,
): ToolRegistry {
    return ToolRegistry {
        tool(SetEffectTool(engineController))
        tool(SetBlendModeTool(engineController))
        tool(SetMasterDimmerTool(engineController))
        tool(SetColorPaletteTool(engineController))
        tool(SetTempoMultiplierTool(engineController))
        tool(CreateSceneTool(engineController, presetLibrary))
        tool(LoadSceneTool(engineController, presetLibrary))
        tool(ScanNetworkTool(networkController))
        tool(GetNodeStatusTool(networkController))
        tool(ConfigureNodeTool(networkController))
        tool(DiagnoseConnectionTool(networkController))
        tool(ListFixturesTool(fixtureController))
        tool(FireFixtureTool(fixtureController))
        tool(SetFixtureGroupTool(fixtureController))
        tool(GetEngineStateTool(stateController))
        tool(GetBeatStateTool(stateController))
        tool(GetNetworkStateTool(stateController))

        if (wledApiClient != null && wledDeviceRegistry != null) {
            tool(ListWledDevicesTool(wledDeviceRegistry))
            tool(SetWledBrightnessTool(wledApiClient, wledDeviceRegistry))
            tool(SetWledColorTool(wledApiClient, wledDeviceRegistry))
            tool(SetWledPowerTool(wledApiClient, wledDeviceRegistry))
        }
    }
}

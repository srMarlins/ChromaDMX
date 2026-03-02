package com.chromadmx.subscription.model

sealed class Entitlement {
    data object RealHardware : Entitlement()
    data class Effect(val effectId: String) : Entitlement()
    data object FixtureLimit : Entitlement()
    data object PresetSaves : Entitlement()
    data class GenrePack(val genre: String) : Entitlement()
    data object BleProvisioning : Entitlement()
    data object CameraMapping : Entitlement()
    data object AiAgent : Entitlement()
    data object DataExport : Entitlement()
    data object WledBasic : Entitlement()       // Free tier — 2 WLED devices
    data object WledMultiSegment : Entitlement() // Pro — multi-segment per device
    data object StageView : Entitlement()        // Pro — full stage editor
}

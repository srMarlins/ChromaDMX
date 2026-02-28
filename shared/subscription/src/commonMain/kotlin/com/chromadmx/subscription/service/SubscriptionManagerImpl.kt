package com.chromadmx.subscription.service

import com.chromadmx.subscription.model.Entitlement
import com.chromadmx.subscription.model.EntitlementConfig
import com.chromadmx.subscription.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SubscriptionManagerImpl(
    private val tierFlow: StateFlow<SubscriptionTier>,
    private val config: EntitlementConfig,
) : SubscriptionManager {

    override val currentTier: StateFlow<SubscriptionTier> = tierFlow
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)

    private val tier: SubscriptionTier get() = tierFlow.value

    override fun hasEntitlement(entitlement: Entitlement): Boolean {
        val required = requiredTier(entitlement)
        return tier.hasAccess(required)
    }

    override fun requiredTier(entitlement: Entitlement): SubscriptionTier = when (entitlement) {
        is Entitlement.Effect ->
            config.effectTiers[entitlement.effectId] ?: SubscriptionTier.FREE
        is Entitlement.GenrePack ->
            config.genrePackTiers[entitlement.genre] ?: SubscriptionTier.FREE
        Entitlement.RealHardware ->
            config.capabilityTiers["real_hardware"] ?: SubscriptionTier.PRO
        Entitlement.BleProvisioning ->
            config.capabilityTiers["ble_provisioning"] ?: SubscriptionTier.PRO
        Entitlement.CameraMapping ->
            config.capabilityTiers["camera_mapping"] ?: SubscriptionTier.PRO
        Entitlement.AiAgent ->
            config.capabilityTiers["ai_agent"] ?: SubscriptionTier.ULTIMATE
        Entitlement.DataExport ->
            config.capabilityTiers["data_export"] ?: SubscriptionTier.ULTIMATE
        Entitlement.FixtureLimit -> SubscriptionTier.FREE
        Entitlement.PresetSaves -> SubscriptionTier.FREE
    }

    override fun getFixtureLimit(): Int =
        config.fixtureLimits[tier] ?: config.fixtureLimits[SubscriptionTier.FREE] ?: 4

    override fun getPresetSaveLimit(): Int =
        config.presetSaveLimits[tier] ?: config.presetSaveLimits[SubscriptionTier.FREE] ?: 3

    override fun getAvailableEffects(): Set<String> =
        config.effectTiers.filter { (_, requiredTier) -> tier.hasAccess(requiredTier) }.keys

    override fun getAvailableGenrePacks(): Set<String> =
        config.genrePackTiers.filter { (_, requiredTier) -> tier.hasAccess(requiredTier) }.keys
}

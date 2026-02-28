package com.chromadmx.subscription.service

import com.chromadmx.subscription.model.Entitlement
import com.chromadmx.subscription.model.EntitlementConfig
import com.chromadmx.subscription.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Test fake for SubscriptionManager.
 * Allows tests to control the tier without RevenueCat or SQLDelight.
 */
class FakeSubscriptionManager(
    initialTier: SubscriptionTier = SubscriptionTier.FREE,
    config: EntitlementConfig = EntitlementConfig(),
) : SubscriptionManager {

    private val _tier = MutableStateFlow(initialTier)
    private val delegate = SubscriptionManagerImpl(tierFlow = _tier, config = config)

    override val currentTier: StateFlow<SubscriptionTier> = _tier
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override fun hasEntitlement(entitlement: Entitlement) = delegate.hasEntitlement(entitlement)
    override fun getFixtureLimit() = delegate.getFixtureLimit()
    override fun getPresetSaveLimit() = delegate.getPresetSaveLimit()
    override fun getAvailableEffects() = delegate.getAvailableEffects()
    override fun getAvailableGenrePacks() = delegate.getAvailableGenrePacks()
    override fun requiredTier(entitlement: Entitlement) = delegate.requiredTier(entitlement)

    /** Change tier at runtime to simulate upgrade/downgrade in tests. */
    fun setTier(tier: SubscriptionTier) { _tier.value = tier }
}

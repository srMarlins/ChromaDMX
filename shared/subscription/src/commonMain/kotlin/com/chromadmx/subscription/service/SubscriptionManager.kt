package com.chromadmx.subscription.service

import com.chromadmx.subscription.model.Entitlement
import com.chromadmx.subscription.model.SubscriptionTier
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionManager {
    val currentTier: StateFlow<SubscriptionTier>
    val isLoading: StateFlow<Boolean>
    fun hasEntitlement(entitlement: Entitlement): Boolean
    fun getFixtureLimit(): Int
    fun getPresetSaveLimit(): Int
    fun getAvailableEffects(): Set<String>
    fun getAvailableGenrePacks(): Set<String>
    fun requiredTier(entitlement: Entitlement): SubscriptionTier
}

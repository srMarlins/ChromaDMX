package com.chromadmx.subscription.paywall

import com.chromadmx.subscription.model.SubscriptionTier

/**
 * Represents a context that triggers the paywall.
 * Each variant carries enough info to render a contextual upgrade prompt.
 */
sealed class PaywallTrigger {
    abstract val requiredTier: SubscriptionTier

    data class LockedEffect(
        val effectId: String,
        val effectName: String,
        override val requiredTier: SubscriptionTier,
    ) : PaywallTrigger()

    data class LockedGenrePack(
        val genre: String,
        override val requiredTier: SubscriptionTier,
    ) : PaywallTrigger()

    data class FixtureLimitReached(
        val currentCount: Int,
        val limit: Int,
        override val requiredTier: SubscriptionTier,
    ) : PaywallTrigger()

    data class PresetSaveLimitReached(
        val currentCount: Int,
        val limit: Int,
        override val requiredTier: SubscriptionTier,
    ) : PaywallTrigger()

    data object RealHardwareRequired : PaywallTrigger() {
        override val requiredTier = SubscriptionTier.PRO
    }

    data object BleProvisioningRequired : PaywallTrigger() {
        override val requiredTier = SubscriptionTier.PRO
    }

    data object CameraMappingRequired : PaywallTrigger() {
        override val requiredTier = SubscriptionTier.PRO
    }

    data object AiAgentRequired : PaywallTrigger() {
        override val requiredTier = SubscriptionTier.ULTIMATE
    }

    data object DataExportRequired : PaywallTrigger() {
        override val requiredTier = SubscriptionTier.ULTIMATE
    }

    data class General(
        override val requiredTier: SubscriptionTier = SubscriptionTier.PRO,
    ) : PaywallTrigger()
}

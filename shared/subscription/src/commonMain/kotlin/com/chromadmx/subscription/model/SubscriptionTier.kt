package com.chromadmx.subscription.model

enum class SubscriptionTier {
    FREE,
    PRO,
    ULTIMATE;

    fun hasAccess(required: SubscriptionTier): Boolean = ordinal >= required.ordinal
}

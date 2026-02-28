package com.chromadmx.subscription.repository

import com.chromadmx.subscription.model.SubscriptionTier
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for subscription state persistence, enabling fake implementations in tests.
 */
interface SubscriptionStore {
    val currentTier: Flow<SubscriptionTier>
    val isTrial: Flow<Boolean>
    val productId: Flow<String?>
    val expiresAt: Flow<String?>

    suspend fun setTier(tier: SubscriptionTier)
    suspend fun updateSubscription(
        tier: SubscriptionTier,
        expiresAt: String?,
        productId: String?,
        isTrial: Boolean,
    )
}

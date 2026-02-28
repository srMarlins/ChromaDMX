package com.chromadmx.subscription.billing

import com.chromadmx.subscription.model.SubscriptionTier

/**
 * iOS billing stub. Replace with RevenueCat SDK when App Store is configured.
 */
class BillingPlatformIos : BillingPlatform {
    override suspend fun configure(apiKey: String) { /* TODO: RevenueCat */ }
    override suspend fun getAvailableProducts(): List<BillingProduct> = emptyList()
    override suspend fun purchase(productId: String): PurchaseResult =
        PurchaseResult.Error("Billing not configured yet")
    override suspend fun restorePurchases(): SubscriptionTier = SubscriptionTier.FREE
    override suspend fun getCurrentTier(): SubscriptionTier = SubscriptionTier.FREE
}

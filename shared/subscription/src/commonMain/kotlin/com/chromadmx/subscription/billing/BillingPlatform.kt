package com.chromadmx.subscription.billing

import com.chromadmx.subscription.model.SubscriptionTier

data class BillingProduct(
    val id: String,
    val title: String,
    val price: String,
    val period: BillingPeriod,
)

enum class BillingPeriod { MONTHLY, ANNUAL }

sealed class PurchaseResult {
    data class Success(val tier: SubscriptionTier) : PurchaseResult()
    data class Cancelled(val message: String = "Purchase cancelled") : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

/**
 * Abstraction for platform billing.
 * Implementations will wrap RevenueCat SDKs.
 * Stub implementations are provided for initial development.
 */
interface BillingPlatform {
    suspend fun configure(apiKey: String)
    suspend fun getAvailableProducts(): List<BillingProduct>
    suspend fun purchase(productId: String): PurchaseResult
    suspend fun restorePurchases(): SubscriptionTier
    suspend fun getCurrentTier(): SubscriptionTier
}

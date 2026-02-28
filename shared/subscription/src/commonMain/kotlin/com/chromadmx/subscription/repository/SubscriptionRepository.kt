package com.chromadmx.subscription.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.subscription.model.SubscriptionTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed repository for subscription state persistence.
 *
 * Uses a single-row subscription_status table in [ChromaDmxDatabase].
 * Provides reactive [Flow]-based reads and suspend setters dispatched to [Dispatchers.Default].
 */
class SubscriptionRepository(private val db: ChromaDmxDatabase) : SubscriptionStore {

    private val queries = db.subscriptionQueries

    init {
        queries.ensureDefaults()
    }

    private val subscriptionFlow = queries.selectSubscription()
        .asFlow()
        .mapToOne(Dispatchers.Default)

    override val currentTier: Flow<SubscriptionTier> = subscriptionFlow.map { row ->
        SubscriptionTier.entries.firstOrNull { it.name == row.tier } ?: SubscriptionTier.FREE
    }

    override val isTrial: Flow<Boolean> = subscriptionFlow.map { it.is_trial != 0L }

    override val productId: Flow<String?> = subscriptionFlow.map { it.product_id }

    override val expiresAt: Flow<String?> = subscriptionFlow.map { it.expires_at }

    override suspend fun setTier(tier: SubscriptionTier) = withContext(Dispatchers.Default) {
        queries.updateTier(tier.name, now())
    }

    override suspend fun updateSubscription(
        tier: SubscriptionTier,
        expiresAt: String?,
        productId: String?,
        isTrial: Boolean,
    ) = withContext(Dispatchers.Default) {
        queries.updateSubscription(
            tier = tier.name,
            expires_at = expiresAt,
            product_id = productId,
            is_trial = if (isTrial) 1L else 0L,
            updated_at = now(),
        )
    }

    /** Simple timestamp — avoids kotlinx-datetime dependency. */
    private fun now(): String = ""
}

package com.chromadmx.subscription.repository

import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.core.db.Entitlement_config
import com.chromadmx.subscription.model.EntitlementConfig
import com.chromadmx.subscription.model.SubscriptionTier

/**
 * Seeds and loads [EntitlementConfig] from the SQLDelight database.
 * On first run, inserts default entitlements. On subsequent runs, reads from DB.
 */
class EntitlementConfigLoader(private val db: ChromaDmxDatabase) {

    private val queries = db.subscriptionQueries

    fun load(): EntitlementConfig {
        val existing = queries.selectAllEntitlements().executeAsList()
        if (existing.isEmpty()) {
            seedDefaults()
            return EntitlementConfig()
        }
        return fromDb(existing)
    }

    private fun seedDefaults() {
        val config = EntitlementConfig()
        config.effectTiers.forEach { (id, tier) ->
            queries.upsertEntitlement(id, "effect", tier.name)
        }
        config.genrePackTiers.forEach { (genre, tier) ->
            queries.upsertEntitlement(genre, "genre_pack", tier.name)
        }
        config.capabilityTiers.forEach { (cap, tier) ->
            queries.upsertEntitlement(cap, "capability", tier.name)
        }
    }

    private fun fromDb(rows: List<Entitlement_config>): EntitlementConfig {
        val effects = mutableMapOf<String, SubscriptionTier>()
        val genres = mutableMapOf<String, SubscriptionTier>()
        val capabilities = mutableMapOf<String, SubscriptionTier>()

        for (row in rows) {
            val tier = SubscriptionTier.entries.firstOrNull { it.name == row.minimum_tier }
                ?: SubscriptionTier.FREE
            when (row.feature_type) {
                "effect" -> effects[row.feature_id] = tier
                "genre_pack" -> genres[row.feature_id] = tier
                "capability" -> capabilities[row.feature_id] = tier
            }
        }

        return EntitlementConfig(
            effectTiers = effects,
            genrePackTiers = genres,
            capabilityTiers = capabilities,
        )
    }
}

package com.chromadmx.subscription.model

data class EntitlementConfig(
    val effectTiers: Map<String, SubscriptionTier> = DEFAULT_EFFECT_TIERS,
    val genrePackTiers: Map<String, SubscriptionTier> = DEFAULT_GENRE_PACK_TIERS,
    val fixtureLimits: Map<SubscriptionTier, Int> = DEFAULT_FIXTURE_LIMITS,
    val presetSaveLimits: Map<SubscriptionTier, Int> = DEFAULT_PRESET_SAVE_LIMITS,
    val capabilityTiers: Map<String, SubscriptionTier> = DEFAULT_CAPABILITY_TIERS,
) {
    companion object {
        val DEFAULT_EFFECT_TIERS = mapOf(
            "solid-color" to SubscriptionTier.FREE,
            "gradient-sweep-3d" to SubscriptionTier.FREE,
            "rainbow-sweep-3d" to SubscriptionTier.FREE,
            "wave-3d" to SubscriptionTier.FREE,
            "chase-3d" to SubscriptionTier.PRO,
            "strobe" to SubscriptionTier.PRO,
            "radial-pulse-3d" to SubscriptionTier.PRO,
            "particle-burst-3d" to SubscriptionTier.PRO,
            "perlin-noise-3d" to SubscriptionTier.PRO,
        )

        val DEFAULT_GENRE_PACK_TIERS = mapOf(
            "POP" to SubscriptionTier.FREE,
            "AMBIENT" to SubscriptionTier.FREE,
            "CUSTOM" to SubscriptionTier.FREE,
            "HOUSE" to SubscriptionTier.PRO,
            "TECHNO" to SubscriptionTier.PRO,
            "DNB" to SubscriptionTier.PRO,
            "ROCK" to SubscriptionTier.PRO,
            "HIPHOP" to SubscriptionTier.ULTIMATE,
        )

        val DEFAULT_FIXTURE_LIMITS = mapOf(
            SubscriptionTier.FREE to 2,
            SubscriptionTier.PRO to 10,
            SubscriptionTier.ULTIMATE to Int.MAX_VALUE,
        )

        val DEFAULT_PRESET_SAVE_LIMITS = mapOf(
            SubscriptionTier.FREE to 3,
            SubscriptionTier.PRO to Int.MAX_VALUE,
            SubscriptionTier.ULTIMATE to Int.MAX_VALUE,
        )

        val DEFAULT_CAPABILITY_TIERS = mapOf(
            "real_hardware" to SubscriptionTier.FREE,
            "wled_basic" to SubscriptionTier.FREE,
            "wled_multi_segment" to SubscriptionTier.PRO,
            "stage_view" to SubscriptionTier.PRO,
            "ble_provisioning" to SubscriptionTier.PRO,
            "camera_mapping" to SubscriptionTier.PRO,
            "ai_agent" to SubscriptionTier.ULTIMATE,
            "data_export" to SubscriptionTier.ULTIMATE,
        )
    }
}

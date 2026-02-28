package com.chromadmx.subscription.service

import com.chromadmx.subscription.model.Entitlement
import com.chromadmx.subscription.model.EntitlementConfig
import com.chromadmx.subscription.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionManagerTest {

    private fun createManager(tier: SubscriptionTier = SubscriptionTier.FREE): SubscriptionManagerImpl {
        return SubscriptionManagerImpl(
            tierFlow = MutableStateFlow(tier),
            config = EntitlementConfig(),
        )
    }

    @Test
    fun freeTierIsDefault() = runTest {
        val mgr = createManager()
        assertEquals(SubscriptionTier.FREE, mgr.currentTier.first())
    }

    // Effect entitlements
    @Test
    fun freeUserHasSolidColor() {
        assertTrue(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.Effect("solid-color")))
    }

    @Test
    fun freeUserHasChase() {
        assertTrue(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.Effect("chase-3d")))
    }

    @Test
    fun freeUserHasRainbowSweep() {
        assertTrue(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.Effect("rainbow-sweep-3d")))
    }

    @Test
    fun freeUserLacksStrobe() {
        assertFalse(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.Effect("strobe")))
    }

    @Test
    fun proUserHasStrobe() {
        assertTrue(createManager(SubscriptionTier.PRO).hasEntitlement(Entitlement.Effect("strobe")))
    }

    @Test
    fun freeUserLacksPerlinNoise() {
        assertFalse(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.Effect("perlin-noise-3d")))
    }

    @Test
    fun ultimateUserHasAllEffects() {
        val mgr = createManager(SubscriptionTier.ULTIMATE)
        assertTrue(mgr.hasEntitlement(Entitlement.Effect("perlin-noise-3d")))
        assertTrue(mgr.hasEntitlement(Entitlement.Effect("strobe")))
        assertTrue(mgr.hasEntitlement(Entitlement.Effect("solid-color")))
    }

    // Capability entitlements
    @Test
    fun freeUserLacksRealHardware() {
        assertFalse(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.RealHardware))
    }

    @Test
    fun proUserHasRealHardware() {
        assertTrue(createManager(SubscriptionTier.PRO).hasEntitlement(Entitlement.RealHardware))
    }

    @Test
    fun freeUserLacksAiAgent() {
        assertFalse(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.AiAgent))
    }

    @Test
    fun proUserLacksAiAgent() {
        assertFalse(createManager(SubscriptionTier.PRO).hasEntitlement(Entitlement.AiAgent))
    }

    @Test
    fun ultimateUserHasAiAgent() {
        assertTrue(createManager(SubscriptionTier.ULTIMATE).hasEntitlement(Entitlement.AiAgent))
    }

    @Test
    fun freeUserLacksDataExport() {
        assertFalse(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.DataExport))
    }

    @Test
    fun ultimateUserHasDataExport() {
        assertTrue(createManager(SubscriptionTier.ULTIMATE).hasEntitlement(Entitlement.DataExport))
    }

    @Test
    fun proUserHasBleProvisioning() {
        assertTrue(createManager(SubscriptionTier.PRO).hasEntitlement(Entitlement.BleProvisioning))
    }

    @Test
    fun freeUserLacksBleProvisioning() {
        assertFalse(createManager(SubscriptionTier.FREE).hasEntitlement(Entitlement.BleProvisioning))
    }

    @Test
    fun proUserHasCameraMapping() {
        assertTrue(createManager(SubscriptionTier.PRO).hasEntitlement(Entitlement.CameraMapping))
    }

    // Fixture limits
    @Test
    fun freeFixtureLimit() {
        assertEquals(4, createManager(SubscriptionTier.FREE).getFixtureLimit())
    }

    @Test
    fun proFixtureLimit() {
        assertEquals(8, createManager(SubscriptionTier.PRO).getFixtureLimit())
    }

    @Test
    fun ultimateFixtureLimitUnlimited() {
        assertEquals(Int.MAX_VALUE, createManager(SubscriptionTier.ULTIMATE).getFixtureLimit())
    }

    // Preset save limits
    @Test
    fun freePresetSaveLimit() {
        assertEquals(3, createManager(SubscriptionTier.FREE).getPresetSaveLimit())
    }

    @Test
    fun proPresetSaveLimitUnlimited() {
        assertEquals(Int.MAX_VALUE, createManager(SubscriptionTier.PRO).getPresetSaveLimit())
    }

    // Available effects
    @Test
    fun freeAvailableEffects() {
        val effects = createManager(SubscriptionTier.FREE).getAvailableEffects()
        assertEquals(setOf("solid-color", "chase-3d", "rainbow-sweep-3d"), effects)
    }

    @Test
    fun proAvailableEffectsHasAll() {
        assertEquals(9, createManager(SubscriptionTier.PRO).getAvailableEffects().size)
    }

    // Available genre packs
    @Test
    fun freeGenrePacks() {
        val packs = createManager(SubscriptionTier.FREE).getAvailableGenrePacks()
        assertTrue(packs.contains("POP"))
        assertTrue(packs.contains("AMBIENT"))
        assertTrue(packs.contains("CUSTOM"))
        assertFalse(packs.contains("TECHNO"))
    }

    @Test
    fun proGenrePacks() {
        val packs = createManager(SubscriptionTier.PRO).getAvailableGenrePacks()
        assertTrue(packs.contains("TECHNO"))
        assertTrue(packs.contains("HOUSE"))
        assertFalse(packs.contains("HIPHOP"))
    }

    @Test
    fun ultimateGenrePacksHasAll() {
        assertEquals(8, createManager(SubscriptionTier.ULTIMATE).getAvailableGenrePacks().size)
    }

    // Required tier
    @Test
    fun requiredTierForEffect() {
        val mgr = createManager()
        assertEquals(SubscriptionTier.PRO, mgr.requiredTier(Entitlement.Effect("strobe")))
        assertEquals(SubscriptionTier.FREE, mgr.requiredTier(Entitlement.Effect("solid-color")))
    }

    @Test
    fun requiredTierForAiAgent() {
        assertEquals(SubscriptionTier.ULTIMATE, createManager().requiredTier(Entitlement.AiAgent))
    }
}

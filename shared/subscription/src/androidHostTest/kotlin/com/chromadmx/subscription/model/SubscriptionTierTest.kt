package com.chromadmx.subscription.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SubscriptionTierTest {
    @Test
    fun tierOrdering() {
        assertTrue(SubscriptionTier.FREE.ordinal < SubscriptionTier.PRO.ordinal)
        assertTrue(SubscriptionTier.PRO.ordinal < SubscriptionTier.ULTIMATE.ordinal)
    }

    @Test
    fun tierCount() {
        assertEquals(3, SubscriptionTier.entries.size)
    }

    @Test
    fun freeIsDefault() {
        assertEquals(SubscriptionTier.FREE, SubscriptionTier.entries.first())
    }

    @Test
    fun hasAccessSameTier() {
        assertTrue(SubscriptionTier.PRO.hasAccess(SubscriptionTier.PRO))
    }

    @Test
    fun hasAccessHigherTier() {
        assertTrue(SubscriptionTier.ULTIMATE.hasAccess(SubscriptionTier.PRO))
    }

    @Test
    fun noAccessLowerTier() {
        assertFalse(SubscriptionTier.FREE.hasAccess(SubscriptionTier.PRO))
    }
}

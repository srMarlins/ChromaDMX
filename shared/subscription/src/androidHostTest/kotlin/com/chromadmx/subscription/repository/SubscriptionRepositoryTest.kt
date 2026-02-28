package com.chromadmx.subscription.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.chromadmx.core.db.ChromaDmxDatabase
import com.chromadmx.subscription.model.SubscriptionTier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SubscriptionRepositoryTest {

    private lateinit var db: ChromaDmxDatabase
    private lateinit var repo: SubscriptionRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChromaDmxDatabase.Schema.create(driver)
        db = ChromaDmxDatabase(driver)
        repo = SubscriptionRepository(db)
    }

    @Test
    fun defaultTierIsFree() = runTest {
        assertEquals(SubscriptionTier.FREE, repo.currentTier.first())
    }

    @Test
    fun defaultIsNotTrial() = runTest {
        assertFalse(repo.isTrial.first())
    }

    @Test
    fun defaultProductIdIsNull() = runTest {
        assertNull(repo.productId.first())
    }

    @Test
    fun defaultExpiresAtIsNull() = runTest {
        assertNull(repo.expiresAt.first())
    }

    @Test
    fun updateTier() = runTest {
        repo.setTier(SubscriptionTier.PRO)
        assertEquals(SubscriptionTier.PRO, repo.currentTier.first())
    }

    @Test
    fun updateFullSubscription() = runTest {
        repo.updateSubscription(
            tier = SubscriptionTier.ULTIMATE,
            expiresAt = "2026-12-31T00:00:00Z",
            productId = "chromadmx_ultimate_annual",
            isTrial = false,
        )
        assertEquals(SubscriptionTier.ULTIMATE, repo.currentTier.first())
        assertEquals("chromadmx_ultimate_annual", repo.productId.first())
        assertEquals("2026-12-31T00:00:00Z", repo.expiresAt.first())
        assertFalse(repo.isTrial.first())
    }

    @Test
    fun tierDowngrade() = runTest {
        repo.setTier(SubscriptionTier.ULTIMATE)
        assertEquals(SubscriptionTier.ULTIMATE, repo.currentTier.first())
        repo.setTier(SubscriptionTier.FREE)
        assertEquals(SubscriptionTier.FREE, repo.currentTier.first())
    }
}

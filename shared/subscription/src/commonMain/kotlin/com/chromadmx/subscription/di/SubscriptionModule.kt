package com.chromadmx.subscription.di

import com.chromadmx.subscription.model.EntitlementConfig
import com.chromadmx.subscription.model.SubscriptionTier
import com.chromadmx.subscription.repository.SubscriptionRepository
import com.chromadmx.subscription.repository.SubscriptionStore
import com.chromadmx.subscription.service.SubscriptionManager
import com.chromadmx.subscription.service.SubscriptionManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.dsl.bind
import org.koin.dsl.module

val subscriptionModule = module {
    // Persistence
    single { SubscriptionRepository(get()) } bind SubscriptionStore::class

    // Entitlement config (defaults)
    single { EntitlementConfig() }

    // SubscriptionManager — the main API
    single<SubscriptionManager> {
        val store: SubscriptionStore = get()
        val config: EntitlementConfig = get()
        val scope: CoroutineScope = get()

        val tierStateFlow: StateFlow<SubscriptionTier> = store.currentTier
            .stateIn(scope, SharingStarted.Eagerly, SubscriptionTier.FREE)

        SubscriptionManagerImpl(tierFlow = tierStateFlow, config = config)
    }
}

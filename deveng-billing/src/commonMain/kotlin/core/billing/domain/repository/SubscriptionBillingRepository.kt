package core.billing.domain.repository

import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.SubscriptionPurchaseResult

interface SubscriptionBillingRepository {
    val isSupported: Boolean

    fun configure(appUserId: String?)

    suspend fun syncAppUserId(appUserId: String?)

    suspend fun hasActiveEntitlement(): Boolean

    suspend fun restorePurchases(): Boolean

    suspend fun fetchOfferingPrices(): Map<String, SubscriptionOfferPrice>

    suspend fun purchasePackage(packageIdentifier: String): SubscriptionPurchaseResult
}

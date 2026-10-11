package core.billing.data

import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.SubscriptionPurchaseResult
import core.billing.domain.repository.SubscriptionBillingRepository

private const val UNSUPPORTED_MESSAGE = "unsupported"

class NoOpSubscriptionBillingRepository : SubscriptionBillingRepository {
    override val isSupported: Boolean = false

    override fun configure(appUserId: String?) = Unit

    override suspend fun syncAppUserId(appUserId: String?) = Unit

    override suspend fun hasActiveEntitlement(): Boolean = false

    override suspend fun restorePurchases(): Boolean = false

    override suspend fun getOfferingPrices(): Map<String, SubscriptionOfferPrice> = emptyMap()

    override suspend fun purchasePackage(packageIdentifier: String): SubscriptionPurchaseResult =
        SubscriptionPurchaseResult.Failed(message = UNSUPPORTED_MESSAGE, storeErrorCode = null)
}

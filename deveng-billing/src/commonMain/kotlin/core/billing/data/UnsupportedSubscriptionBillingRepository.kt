package core.billing.data

import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.UNSUPPORTED_PURCHASE_MESSAGE
import core.billing.domain.model.SubscriptionPurchaseResult
import core.billing.domain.repository.SubscriptionBillingRepository

internal class UnsupportedSubscriptionBillingRepository : SubscriptionBillingRepository {
    override val isSupported: Boolean = false

    override fun configure(appUserId: String?) = Unit

    override suspend fun syncAppUserId(appUserId: String?) = Unit

    override suspend fun hasActiveEntitlement(): Boolean = false

    override suspend fun restorePurchases(): Boolean = false

    override suspend fun fetchOfferingPrices(): Map<String, SubscriptionOfferPrice> = emptyMap()

    override suspend fun purchasePackage(packageIdentifier: String): SubscriptionPurchaseResult =
        SubscriptionPurchaseResult.Failed(message = UNSUPPORTED_PURCHASE_MESSAGE, storeErrorCode = null)
}

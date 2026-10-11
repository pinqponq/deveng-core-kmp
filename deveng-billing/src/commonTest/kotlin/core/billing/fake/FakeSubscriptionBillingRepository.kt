package core.billing.fake

import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.SubscriptionPurchaseResult
import core.billing.domain.repository.SubscriptionBillingRepository

class FakeSubscriptionBillingRepository(
    override val isSupported: Boolean = true,
) : SubscriptionBillingRepository {

    var purchaseResult: SubscriptionPurchaseResult = SubscriptionPurchaseResult.Cancelled
    var hasActiveEntitlement: Boolean = false
    var isRestoreSuccessful: Boolean = false
    var offeringPrices: Map<String, SubscriptionOfferPrice> = emptyMap()

    val configuredAppUserIds = mutableListOf<String?>()
    val syncedAppUserIds = mutableListOf<String?>()
    val purchasedPackageIdentifiers = mutableListOf<String>()

    override fun configure(appUserId: String?) {
        configuredAppUserIds += appUserId
    }

    override suspend fun syncAppUserId(appUserId: String?) {
        syncedAppUserIds += appUserId
    }

    override suspend fun hasActiveEntitlement(): Boolean = hasActiveEntitlement

    override suspend fun restorePurchases(): Boolean = isRestoreSuccessful

    override suspend fun getOfferingPrices(): Map<String, SubscriptionOfferPrice> = offeringPrices

    override suspend fun purchasePackage(packageIdentifier: String): SubscriptionPurchaseResult {
        purchasedPackageIdentifiers += packageIdentifier
        return purchaseResult
    }
}

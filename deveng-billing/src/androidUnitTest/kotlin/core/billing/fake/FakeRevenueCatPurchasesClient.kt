package core.billing.fake

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.models.StoreProduct
import core.billing.data.RevenueCatPurchasesClient

internal class FakeRevenueCatPurchasesClient(
    override var isConfigured: Boolean = true,
    override var currentAppUserId: String = "",
) : RevenueCatPurchasesClient {

    var activeEntitlementIds: Set<String> = emptySet()
    var currentOfferingProducts: Map<String, StoreProduct> = emptyMap()
    var purchasablePackageIdentifiers: Set<String> = emptySet()
    var storeFailure: Exception? = null

    val configurations = mutableListOf<Configuration>()
    val loggedInAppUserIds = mutableListOf<String>()
    var logOutCount: Int = 0
    val purchasedPackageIdentifiers = mutableListOf<String>()

    override fun configure(apiKey: String, appUserId: String?, logLevel: LogLevel) {
        configurations += Configuration(apiKey = apiKey, appUserId = appUserId, logLevel = logLevel)
        isConfigured = true
    }

    override suspend fun logIn(appUserId: String) {
        loggedInAppUserIds += appUserId
    }

    override suspend fun logOut() {
        logOutCount++
    }

    override suspend fun hasActiveEntitlement(entitlementId: String): Boolean = entitlementId in activeEntitlementIds

    override suspend fun restorePurchases(entitlementId: String): Boolean = entitlementId in activeEntitlementIds

    override suspend fun fetchCurrentOfferingProducts(): Map<String, StoreProduct> {
        storeFailure?.let { failure -> throw failure }
        return currentOfferingProducts
    }

    override suspend fun purchasePackage(packageIdentifier: String, entitlementId: String): Boolean? {
        storeFailure?.let { failure -> throw failure }
        if (packageIdentifier !in purchasablePackageIdentifiers) return null

        purchasedPackageIdentifiers += packageIdentifier
        return entitlementId in activeEntitlementIds
    }

    data class Configuration(val apiKey: String, val appUserId: String?, val logLevel: LogLevel)
}

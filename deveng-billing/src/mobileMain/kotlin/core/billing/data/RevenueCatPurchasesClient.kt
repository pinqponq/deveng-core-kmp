package core.billing.data

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.models.StoreProduct

internal interface RevenueCatPurchasesClient {
    val isConfigured: Boolean

    val currentAppUserId: String

    fun configure(apiKey: String, appUserId: String?, logLevel: LogLevel)

    suspend fun logIn(appUserId: String)

    suspend fun logOut()

    suspend fun hasActiveEntitlement(entitlementId: String): Boolean

    suspend fun restorePurchases(entitlementId: String): Boolean

    suspend fun fetchCurrentOfferingProducts(): Map<String, StoreProduct>

    /** Returns whether [entitlementId] is active after the purchase, or null when the current offering has no such package. */
    suspend fun purchasePackage(packageIdentifier: String, entitlementId: String): Boolean?
}

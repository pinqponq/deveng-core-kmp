package core.billing.data

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitLogIn
import com.revenuecat.purchases.kmp.ktx.awaitLogOut
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.StoreProduct

internal class DefaultRevenueCatPurchasesClient : RevenueCatPurchasesClient {

    override val isConfigured: Boolean
        get() = Purchases.isConfigured

    override val currentAppUserId: String
        get() = Purchases.sharedInstance.appUserID

    override fun configure(apiKey: String, appUserId: String?, logLevel: LogLevel) {
        Purchases.logLevel = logLevel
        Purchases.configure(apiKey = apiKey) {
            if (!appUserId.isNullOrBlank()) {
                this.appUserId = appUserId
            }
        }
    }

    override suspend fun logIn(appUserId: String) {
        Purchases.sharedInstance.awaitLogIn(newAppUserID = appUserId)
    }

    override suspend fun logOut() {
        Purchases.sharedInstance.awaitLogOut()
    }

    override suspend fun hasActiveEntitlement(entitlementId: String): Boolean =
        isEntitlementActive(customerInfo = Purchases.sharedInstance.awaitCustomerInfo(), entitlementId = entitlementId)

    override suspend fun restorePurchases(entitlementId: String): Boolean =
        isEntitlementActive(customerInfo = Purchases.sharedInstance.awaitRestore(), entitlementId = entitlementId)

    override suspend fun fetchCurrentOfferingProducts(): Map<String, StoreProduct> =
        Purchases.sharedInstance.awaitOfferings().current?.availablePackages
            ?.associate { it.identifier to it.storeProduct }
            ?: emptyMap()

    override suspend fun purchasePackage(packageIdentifier: String, entitlementId: String): Boolean? {
        val packageToPurchase = Purchases.sharedInstance.awaitOfferings().current
            ?.getPackage(identifier = packageIdentifier)
            ?: return null

        val successfulPurchase = Purchases.sharedInstance.awaitPurchase(packageToPurchase = packageToPurchase)
        return isEntitlementActive(customerInfo = successfulPurchase.customerInfo, entitlementId = entitlementId)
    }

    private fun isEntitlementActive(customerInfo: CustomerInfo, entitlementId: String): Boolean =
        customerInfo.entitlements[entitlementId]?.isActive == true
}

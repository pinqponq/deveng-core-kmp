package core.billing.data

import co.touchlab.kermit.Logger
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
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import core.billing.data.mapper.RevenueCatOfferPriceMapper
import core.billing.domain.model.BillingConfig
import core.billing.domain.model.BillingLogLevel
import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.SubscriptionPurchaseResult
import core.billing.domain.repository.SubscriptionBillingRepository

private const val ANONYMOUS_APP_USER_ID_PREFIX = "\$RCAnonymousID:"

internal class RevenueCatSubscriptionBillingRepositoryImpl(
    private val billingConfig: BillingConfig,
    private val revenueCatOfferPriceMapper: RevenueCatOfferPriceMapper,
) : SubscriptionBillingRepository {

    override val isSupported: Boolean = true

    private val isConfigured: Boolean
        get() = Purchases.isConfigured

    override fun configure(appUserId: String?) {
        if (isConfigured) return

        val platformApiKey = billingConfig.resolvePlatformApiKey()
        require(platformApiKey.isNotBlank()) { "BillingConfig has no RevenueCat API key for this platform" }

        Purchases.logLevel = mapLogLevel(billingLogLevel = billingConfig.logLevel)
        Purchases.configure(apiKey = platformApiKey) {
            if (!appUserId.isNullOrBlank()) {
                this.appUserId = appUserId
            }
        }
    }

    override suspend fun syncAppUserId(appUserId: String?) {
        if (!isConfigured) {
            configure(appUserId = appUserId)
            return
        }

        val purchases = Purchases.sharedInstance
        val currentUserId = purchases.appUserID
        val targetUserId = appUserId?.takeIf { it.isNotBlank() }

        if (targetUserId == null) {
            if (!currentUserId.startsWith(prefix = ANONYMOUS_APP_USER_ID_PREFIX)) {
                purchases.awaitLogOut()
            }

            return
        }

        if (currentUserId == targetUserId) return

        purchases.awaitLogIn(newAppUserID = targetUserId)
    }

    override suspend fun hasActiveEntitlement(): Boolean {
        if (!isConfigured) return false

        val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
        return isEntitlementActive(customerInfo = customerInfo)
    }

    override suspend fun restorePurchases(): Boolean {
        if (!isConfigured) return false

        val customerInfo = Purchases.sharedInstance.awaitRestore()
        return isEntitlementActive(customerInfo = customerInfo)
    }

    override suspend fun fetchOfferingPrices(): Map<String, SubscriptionOfferPrice> {
        if (!isConfigured) return emptyMap()

        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            offerings.current?.availablePackages
                ?.associate { it.identifier to revenueCatOfferPriceMapper.mapToDomain(storeProduct = it.storeProduct) }
                ?: emptyMap()
        } catch (purchasesException: PurchasesException) {
            Logger.w(purchasesException) { "RevenueCat offerings could not be loaded; showing prices without the store" }
            emptyMap()
        }
    }

    override suspend fun purchasePackage(packageIdentifier: String): SubscriptionPurchaseResult {
        if (!isConfigured) {
            return SubscriptionPurchaseResult.Failed(message = "RevenueCat is not configured", storeErrorCode = null)
        }

        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val packageToPurchase = offerings.current?.getPackage(identifier = packageIdentifier)
                ?: return SubscriptionPurchaseResult.Failed(
                    message = "Package not found: $packageIdentifier",
                    storeErrorCode = null,
                )

            val successfulPurchase = Purchases.sharedInstance.awaitPurchase(packageToPurchase = packageToPurchase)
            val hasEntitlement = isEntitlementActive(customerInfo = successfulPurchase.customerInfo)

            SubscriptionPurchaseResult.Success(hasEntitlement = hasEntitlement)
        } catch (transactionException: PurchasesTransactionException) {
            if (transactionException.userCancelled) {
                SubscriptionPurchaseResult.Cancelled
            } else {
                SubscriptionPurchaseResult.Failed(
                    message = transactionException.message,
                    storeErrorCode = transactionException.code.name,
                    cause = transactionException,
                )
            }
        } catch (purchasesException: PurchasesException) {
            SubscriptionPurchaseResult.Failed(
                message = purchasesException.message,
                storeErrorCode = purchasesException.code.name,
                cause = purchasesException,
            )
        }
    }

    private fun isEntitlementActive(customerInfo: CustomerInfo): Boolean =
        customerInfo.entitlements[billingConfig.entitlementId]?.isActive == true

    private fun mapLogLevel(billingLogLevel: BillingLogLevel): LogLevel = when (billingLogLevel) {
        BillingLogLevel.VERBOSE -> LogLevel.VERBOSE
        BillingLogLevel.DEBUG -> LogLevel.DEBUG
        BillingLogLevel.INFO -> LogLevel.INFO
        BillingLogLevel.WARN -> LogLevel.WARN
        BillingLogLevel.ERROR -> LogLevel.ERROR
    }
}

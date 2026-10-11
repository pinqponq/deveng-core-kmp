package core.billing.data

import co.touchlab.kermit.Logger
import com.revenuecat.purchases.kmp.LogLevel
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
    private val revenueCatPurchasesClient: RevenueCatPurchasesClient,
    private val revenueCatOfferPriceMapper: RevenueCatOfferPriceMapper,
) : SubscriptionBillingRepository {

    override val isSupported: Boolean = true

    override fun configure(appUserId: String?) {
        if (revenueCatPurchasesClient.isConfigured) return

        val platformApiKey = billingConfig.resolvePlatformApiKey()
        require(platformApiKey.isNotBlank()) { "BillingConfig has no RevenueCat API key for this platform" }

        revenueCatPurchasesClient.configure(
            apiKey = platformApiKey,
            appUserId = appUserId,
            logLevel = mapLogLevel(billingLogLevel = billingConfig.logLevel),
        )
    }

    override suspend fun syncAppUserId(appUserId: String?) {
        if (!revenueCatPurchasesClient.isConfigured) {
            configure(appUserId = appUserId)
            return
        }

        val currentUserId = revenueCatPurchasesClient.currentAppUserId
        val targetUserId = appUserId?.takeIf { it.isNotBlank() }

        if (targetUserId == null) {
            if (!currentUserId.startsWith(prefix = ANONYMOUS_APP_USER_ID_PREFIX)) {
                revenueCatPurchasesClient.logOut()
            }

            return
        }

        if (currentUserId == targetUserId) return

        revenueCatPurchasesClient.logIn(appUserId = targetUserId)
    }

    override suspend fun hasActiveEntitlement(): Boolean {
        if (!revenueCatPurchasesClient.isConfigured) return false

        return revenueCatPurchasesClient.hasActiveEntitlement(entitlementId = billingConfig.entitlementId)
    }

    override suspend fun restorePurchases(): Boolean {
        if (!revenueCatPurchasesClient.isConfigured) return false

        return revenueCatPurchasesClient.restorePurchases(entitlementId = billingConfig.entitlementId)
    }

    override suspend fun fetchOfferingPrices(): Map<String, SubscriptionOfferPrice> {
        if (!revenueCatPurchasesClient.isConfigured) return emptyMap()

        return try {
            revenueCatPurchasesClient.fetchCurrentOfferingProducts()
                .mapValues { (_, storeProduct) -> revenueCatOfferPriceMapper.mapToDomain(storeProduct = storeProduct) }
        } catch (purchasesException: PurchasesException) {
            Logger.w(purchasesException) { "RevenueCat offerings could not be loaded; showing prices without the store" }
            emptyMap()
        }
    }

    override suspend fun purchasePackage(packageIdentifier: String): SubscriptionPurchaseResult {
        if (!revenueCatPurchasesClient.isConfigured) {
            return SubscriptionPurchaseResult.Failed(message = "RevenueCat is not configured", storeErrorCode = null)
        }

        return try {
            val hasEntitlement = revenueCatPurchasesClient.purchasePackage(
                packageIdentifier = packageIdentifier,
                entitlementId = billingConfig.entitlementId,
            ) ?: return SubscriptionPurchaseResult.Failed(
                message = "Package not found: $packageIdentifier",
                storeErrorCode = null,
            )

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

    private fun mapLogLevel(billingLogLevel: BillingLogLevel): LogLevel = when (billingLogLevel) {
        BillingLogLevel.VERBOSE -> LogLevel.VERBOSE
        BillingLogLevel.DEBUG -> LogLevel.DEBUG
        BillingLogLevel.INFO -> LogLevel.INFO
        BillingLogLevel.WARN -> LogLevel.WARN
        BillingLogLevel.ERROR -> LogLevel.ERROR
    }
}

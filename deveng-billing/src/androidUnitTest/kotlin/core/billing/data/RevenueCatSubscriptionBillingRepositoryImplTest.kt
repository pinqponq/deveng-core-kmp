package core.billing.data

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PeriodUnit
import com.revenuecat.purchases.kmp.models.Price
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import core.billing.data.mapper.RevenueCatOfferPriceMapper
import core.billing.domain.model.BillingConfig
import core.billing.domain.model.BillingLogLevel
import core.billing.domain.model.SubscriptionPurchaseResult
import core.billing.fake.FakeRevenueCatPurchasesClient
import core.billing.fake.FakeRevenueCatPurchasesClient.Configuration
import core.billing.fake.FakeStoreProduct
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevenueCatSubscriptionBillingRepositoryImplTest {

    private val revenueCatPurchasesClient = FakeRevenueCatPurchasesClient()

    @Test
    fun configure_whenNotConfigured_configuresWithAndroidKeyUserAndLogLevel() {
        revenueCatPurchasesClient.isConfigured = false

        createRepository().configure(appUserId = APP_USER_ID)

        assertEquals(
            listOf(Configuration(apiKey = ANDROID_API_KEY, appUserId = APP_USER_ID, logLevel = LogLevel.WARN)),
            revenueCatPurchasesClient.configurations,
        )
    }

    @Test
    fun configure_whenAlreadyConfigured_keepsTheExistingConfiguration() {
        createRepository().configure(appUserId = APP_USER_ID)

        assertEquals(emptyList(), revenueCatPurchasesClient.configurations)
    }

    @Test
    fun configure_withoutApiKeyForThisPlatform_failsAtStartup() {
        revenueCatPurchasesClient.isConfigured = false

        assertFailsWith<IllegalArgumentException> {
            createRepository(billingConfig = BILLING_CONFIG.copy(androidApiKey = " ")).configure(appUserId = APP_USER_ID)
        }
    }

    @Test
    fun configure_withEachLogLevel_passesTheMatchingRevenueCatLevel() {
        val logLevels = mapOf(
            BillingLogLevel.VERBOSE to LogLevel.VERBOSE,
            BillingLogLevel.DEBUG to LogLevel.DEBUG,
            BillingLogLevel.INFO to LogLevel.INFO,
            BillingLogLevel.WARN to LogLevel.WARN,
            BillingLogLevel.ERROR to LogLevel.ERROR,
        )

        logLevels.forEach { (billingLogLevel, expectedLogLevel) ->
            val purchasesClient = FakeRevenueCatPurchasesClient(isConfigured = false)

            createRepository(
                billingConfig = BILLING_CONFIG.copy(logLevel = billingLogLevel),
                purchasesClient = purchasesClient,
            ).configure(appUserId = null)

            assertEquals(expectedLogLevel, purchasesClient.configurations.single().logLevel)
        }
    }

    @Test
    fun syncAppUserId_whenNotConfigured_configuresWithTheUser() = runTest {
        revenueCatPurchasesClient.isConfigured = false

        createRepository().syncAppUserId(appUserId = APP_USER_ID)

        assertEquals(APP_USER_ID, revenueCatPurchasesClient.configurations.single().appUserId)
        assertEquals(emptyList(), revenueCatPurchasesClient.loggedInAppUserIds)
    }

    @Test
    fun syncAppUserId_whenSignedOutFromIdentifiedUser_logsOut() = runTest {
        revenueCatPurchasesClient.currentAppUserId = APP_USER_ID

        createRepository().syncAppUserId(appUserId = null)

        assertEquals(1, revenueCatPurchasesClient.logOutCount)
    }

    @Test
    fun syncAppUserId_whenSignedOutAndAlreadyAnonymous_staysAnonymous() = runTest {
        revenueCatPurchasesClient.currentAppUserId = ANONYMOUS_APP_USER_ID

        createRepository().syncAppUserId(appUserId = " ")

        assertEquals(0, revenueCatPurchasesClient.logOutCount)
        assertEquals(emptyList(), revenueCatPurchasesClient.loggedInAppUserIds)
    }

    @Test
    fun syncAppUserId_whenSameUser_doesNotLogInAgain() = runTest {
        revenueCatPurchasesClient.currentAppUserId = APP_USER_ID

        createRepository().syncAppUserId(appUserId = APP_USER_ID)

        assertEquals(emptyList(), revenueCatPurchasesClient.loggedInAppUserIds)
    }

    @Test
    fun syncAppUserId_whenAnotherUserSignsIn_logsInThatUser() = runTest {
        revenueCatPurchasesClient.currentAppUserId = ANONYMOUS_APP_USER_ID

        createRepository().syncAppUserId(appUserId = APP_USER_ID)

        assertEquals(listOf(APP_USER_ID), revenueCatPurchasesClient.loggedInAppUserIds)
    }

    @Test
    fun hasActiveEntitlement_whenConfiguredEntitlementIsActive_returnsTrue() = runTest {
        revenueCatPurchasesClient.activeEntitlementIds = setOf(ENTITLEMENT_ID)

        assertTrue(createRepository().hasActiveEntitlement())
    }

    @Test
    fun hasActiveEntitlement_whenOnlyAnotherEntitlementIsActive_returnsFalse() = runTest {
        revenueCatPurchasesClient.activeEntitlementIds = setOf(OTHER_ENTITLEMENT_ID)

        assertFalse(createRepository().hasActiveEntitlement())
    }

    @Test
    fun hasActiveEntitlement_beforeConfiguration_returnsFalse() = runTest {
        revenueCatPurchasesClient.isConfigured = false
        revenueCatPurchasesClient.activeEntitlementIds = setOf(ENTITLEMENT_ID)

        assertFalse(createRepository().hasActiveEntitlement())
    }

    @Test
    fun restorePurchases_whenConfiguredEntitlementIsRestored_returnsTrue() = runTest {
        revenueCatPurchasesClient.activeEntitlementIds = setOf(ENTITLEMENT_ID)

        assertTrue(createRepository().restorePurchases())
    }

    @Test
    fun restorePurchases_beforeConfiguration_returnsFalse() = runTest {
        revenueCatPurchasesClient.isConfigured = false
        revenueCatPurchasesClient.activeEntitlementIds = setOf(ENTITLEMENT_ID)

        assertFalse(createRepository().restorePurchases())
    }

    @Test
    fun fetchOfferingPrices_withCurrentOffering_returnsMappedPricesByPackage() = runTest {
        revenueCatPurchasesClient.currentOfferingProducts = mapOf(ANNUAL_PACKAGE_IDENTIFIER to ANNUAL_PRODUCT)

        val offeringPrices = createRepository().fetchOfferingPrices()

        assertEquals(
            mapOf(ANNUAL_PACKAGE_IDENTIFIER to RevenueCatOfferPriceMapper().mapToDomain(storeProduct = ANNUAL_PRODUCT)),
            offeringPrices,
        )
    }

    @Test
    fun fetchOfferingPrices_whenStoreFails_returnsNoPrices() = runTest {
        revenueCatPurchasesClient.storeFailure = PurchasesException(PurchasesError(code = PurchasesErrorCode.NetworkError))

        assertEquals(emptyMap(), createRepository().fetchOfferingPrices())
    }

    @Test
    fun fetchOfferingPrices_beforeConfiguration_returnsNoPrices() = runTest {
        revenueCatPurchasesClient.isConfigured = false
        revenueCatPurchasesClient.currentOfferingProducts = mapOf(ANNUAL_PACKAGE_IDENTIFIER to ANNUAL_PRODUCT)

        assertEquals(emptyMap(), createRepository().fetchOfferingPrices())
    }

    @Test
    fun purchasePackage_whenPurchaseGrantsEntitlement_succeedsWithEntitlement() = runTest {
        revenueCatPurchasesClient.purchasablePackageIdentifiers = setOf(ANNUAL_PACKAGE_IDENTIFIER)
        revenueCatPurchasesClient.activeEntitlementIds = setOf(ENTITLEMENT_ID)

        val purchaseResult = createRepository().purchasePackage(packageIdentifier = ANNUAL_PACKAGE_IDENTIFIER)

        assertEquals(SubscriptionPurchaseResult.Success(hasEntitlement = true), purchaseResult)
        assertEquals(listOf(ANNUAL_PACKAGE_IDENTIFIER), revenueCatPurchasesClient.purchasedPackageIdentifiers)
    }

    @Test
    fun purchasePackage_whenPackageIsNotInCurrentOffering_failsWithoutStoreErrorCode() = runTest {
        val purchaseResult = createRepository().purchasePackage(packageIdentifier = ANNUAL_PACKAGE_IDENTIFIER)

        assertEquals(
            SubscriptionPurchaseResult.Failed(message = "Package not found: $ANNUAL_PACKAGE_IDENTIFIER", storeErrorCode = null),
            purchaseResult,
        )
    }

    @Test
    fun purchasePackage_beforeConfiguration_failsWithoutReachingTheStore() = runTest {
        revenueCatPurchasesClient.isConfigured = false
        revenueCatPurchasesClient.purchasablePackageIdentifiers = setOf(ANNUAL_PACKAGE_IDENTIFIER)

        val purchaseResult = createRepository().purchasePackage(packageIdentifier = ANNUAL_PACKAGE_IDENTIFIER)

        assertEquals(
            SubscriptionPurchaseResult.Failed(message = "RevenueCat is not configured", storeErrorCode = null),
            purchaseResult,
        )
        assertEquals(emptyList(), revenueCatPurchasesClient.purchasedPackageIdentifiers)
    }

    @Test
    fun purchasePackage_whenUserCancelsTheStoreSheet_isCancelled() = runTest {
        revenueCatPurchasesClient.storeFailure = PurchasesTransactionException(
            purchasesError = PurchasesError(code = PurchasesErrorCode.PurchaseCancelledError),
            userCancelled = true,
        )

        val purchaseResult = createRepository().purchasePackage(packageIdentifier = ANNUAL_PACKAGE_IDENTIFIER)

        assertEquals(SubscriptionPurchaseResult.Cancelled, purchaseResult)
    }

    @Test
    fun purchasePackage_whenTransactionFails_failsWithStoreErrorCodeAndCause() = runTest {
        val transactionException = PurchasesTransactionException(
            purchasesError = PurchasesError(code = PurchasesErrorCode.StoreProblemError),
            userCancelled = false,
        )
        revenueCatPurchasesClient.storeFailure = transactionException

        val purchaseResult = createRepository().purchasePackage(packageIdentifier = ANNUAL_PACKAGE_IDENTIFIER)

        assertEquals(
            SubscriptionPurchaseResult.Failed(
                message = transactionException.message,
                storeErrorCode = PurchasesErrorCode.StoreProblemError.name,
                cause = transactionException,
            ),
            purchaseResult,
        )
    }

    @Test
    fun purchasePackage_whenOfferingsCannotBeLoaded_failsWithStoreErrorCodeAndCause() = runTest {
        val purchasesException = PurchasesException(PurchasesError(code = PurchasesErrorCode.NetworkError))
        revenueCatPurchasesClient.storeFailure = purchasesException

        val purchaseResult = createRepository().purchasePackage(packageIdentifier = ANNUAL_PACKAGE_IDENTIFIER)

        assertEquals(
            SubscriptionPurchaseResult.Failed(
                message = purchasesException.message,
                storeErrorCode = PurchasesErrorCode.NetworkError.name,
                cause = purchasesException,
            ),
            purchaseResult,
        )
    }

    private fun createRepository(
        billingConfig: BillingConfig = BILLING_CONFIG,
        purchasesClient: FakeRevenueCatPurchasesClient = revenueCatPurchasesClient,
    ) = RevenueCatSubscriptionBillingRepositoryImpl(
        billingConfig = billingConfig,
        revenueCatPurchasesClient = purchasesClient,
        revenueCatOfferPriceMapper = RevenueCatOfferPriceMapper(),
    )

    private companion object {
        const val ENTITLEMENT_ID = "pro"
        const val OTHER_ENTITLEMENT_ID = "legacy"
        const val ANDROID_API_KEY = "goog_test"
        const val IOS_API_KEY = "appl_test"
        const val APP_USER_ID = "42"
        const val ANONYMOUS_APP_USER_ID = "\$RCAnonymousID:1234"
        const val ANNUAL_PACKAGE_IDENTIFIER = "\$rc_annual"
        val BILLING_CONFIG = BillingConfig(
            entitlementId = ENTITLEMENT_ID,
            androidApiKey = ANDROID_API_KEY,
            iosApiKey = IOS_API_KEY,
            logLevel = BillingLogLevel.WARN,
        )
        val ANNUAL_PRODUCT = FakeStoreProduct(
            price = Price(formatted = "₺499,99", amountMicros = 499_990_000, currencyCode = "TRY"),
            period = Period(value = 1, unit = PeriodUnit.YEAR),
            pricePerMonth = Price(formatted = "₺41,66", amountMicros = 41_660_000, currencyCode = "TRY"),
        )
    }
}

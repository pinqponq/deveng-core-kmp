package core.billing.domain.usecase

import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.SubscriptionPeriodUnit
import core.billing.fake.FakeSubscriptionBillingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionBillingQueryUseCasesTest {

    @Test
    fun hasActiveEntitlement_whenStoreReportsEntitlement_returnsTrue() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository().apply { hasActiveEntitlement = true }

        assertTrue(HasActiveEntitlementUseCase(subscriptionBillingRepository = subscriptionBillingRepository)())
    }

    @Test
    fun hasActiveEntitlement_onPlatformWithoutBilling_returnsFalse() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository(isSupported = false).apply {
            hasActiveEntitlement = true
        }

        assertFalse(HasActiveEntitlementUseCase(subscriptionBillingRepository = subscriptionBillingRepository)())
    }

    @Test
    fun restorePurchases_whenStoreRestoresEntitlement_returnsTrue() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository().apply { isRestoreSuccessful = true }

        assertTrue(RestorePurchasesUseCase(subscriptionBillingRepository = subscriptionBillingRepository)())
    }

    @Test
    fun restorePurchases_onPlatformWithoutBilling_returnsFalse() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository(isSupported = false).apply {
            isRestoreSuccessful = true
        }

        assertFalse(RestorePurchasesUseCase(subscriptionBillingRepository = subscriptionBillingRepository)())
    }

    @Test
    fun fetchOfferPrices_whenStoreHasOfferings_returnsPricesByPackage() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository().apply { offeringPrices = OFFERING_PRICES }

        assertEquals(OFFERING_PRICES, FetchOfferPricesUseCase(subscriptionBillingRepository = subscriptionBillingRepository)())
    }

    @Test
    fun fetchOfferPrices_onPlatformWithoutBilling_returnsNoPrices() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository(isSupported = false).apply {
            offeringPrices = OFFERING_PRICES
        }

        assertEquals(emptyMap(), FetchOfferPricesUseCase(subscriptionBillingRepository = subscriptionBillingRepository)())
    }

    private companion object {
        val OFFERING_PRICES = mapOf(
            "\$rc_monthly" to SubscriptionOfferPrice(
                totalPriceText = "$4.99",
                totalPriceMicros = 4_990_000,
                pricePerMonthText = "$4.99",
                pricePerMonthMicros = 4_990_000,
                periodUnit = SubscriptionPeriodUnit.MONTH,
                periodValue = 1,
            ),
        )
    }
}

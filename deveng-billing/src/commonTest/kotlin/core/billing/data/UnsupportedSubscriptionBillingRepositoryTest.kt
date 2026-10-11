package core.billing.data

import core.billing.domain.model.SubscriptionPurchaseResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UnsupportedSubscriptionBillingRepositoryTest {

    private val unsupportedSubscriptionBillingRepository = UnsupportedSubscriptionBillingRepository()

    @Test
    fun purchasePackage_onPlatformWithoutBilling_failsWithoutStoreErrorCode() = runTest {
        val purchaseResult = unsupportedSubscriptionBillingRepository.purchasePackage(packageIdentifier = PACKAGE_IDENTIFIER)

        assertEquals(
            SubscriptionPurchaseResult.Failed(message = UNSUPPORTED_MESSAGE, storeErrorCode = null),
            purchaseResult,
        )
    }

    @Test
    fun isSupported_onPlatformWithoutBilling_isFalse() {
        assertFalse(unsupportedSubscriptionBillingRepository.isSupported)
    }

    @Test
    fun hasActiveEntitlement_onPlatformWithoutBilling_returnsFalse() = runTest {
        assertFalse(unsupportedSubscriptionBillingRepository.hasActiveEntitlement())
    }

    @Test
    fun restorePurchases_onPlatformWithoutBilling_returnsFalse() = runTest {
        assertFalse(unsupportedSubscriptionBillingRepository.restorePurchases())
    }

    @Test
    fun fetchOfferingPrices_onPlatformWithoutBilling_returnsNoPrices() = runTest {
        assertEquals(emptyMap(), unsupportedSubscriptionBillingRepository.fetchOfferingPrices())
    }

    private companion object {
        const val PACKAGE_IDENTIFIER = "\$rc_annual"
        const val UNSUPPORTED_MESSAGE = "unsupported"
    }
}

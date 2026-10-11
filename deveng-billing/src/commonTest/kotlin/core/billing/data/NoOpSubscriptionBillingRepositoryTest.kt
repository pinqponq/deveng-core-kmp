package core.billing.data

import core.billing.domain.model.SubscriptionPurchaseResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NoOpSubscriptionBillingRepositoryTest {

    private val noOpSubscriptionBillingRepository = NoOpSubscriptionBillingRepository()

    @Test
    fun purchasePackage_onPlatformWithoutBilling_failsWithoutStoreErrorCode() = runTest {
        val purchaseResult = noOpSubscriptionBillingRepository.purchasePackage(packageIdentifier = PACKAGE_IDENTIFIER)

        assertEquals(
            SubscriptionPurchaseResult.Failed(message = UNSUPPORTED_MESSAGE, storeErrorCode = null),
            purchaseResult,
        )
    }

    @Test
    fun isSupported_onPlatformWithoutBilling_isFalse() {
        assertFalse(noOpSubscriptionBillingRepository.isSupported)
    }

    @Test
    fun hasActiveEntitlement_onPlatformWithoutBilling_returnsFalse() = runTest {
        assertFalse(noOpSubscriptionBillingRepository.hasActiveEntitlement())
    }

    @Test
    fun restorePurchases_onPlatformWithoutBilling_returnsFalse() = runTest {
        assertFalse(noOpSubscriptionBillingRepository.restorePurchases())
    }

    @Test
    fun fetchOfferingPrices_onPlatformWithoutBilling_returnsNoPrices() = runTest {
        assertEquals(emptyMap(), noOpSubscriptionBillingRepository.fetchOfferingPrices())
    }

    private companion object {
        const val PACKAGE_IDENTIFIER = "\$rc_annual"
        const val UNSUPPORTED_MESSAGE = "unsupported"
    }
}

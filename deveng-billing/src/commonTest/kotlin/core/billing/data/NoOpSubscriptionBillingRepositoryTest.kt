package core.billing.data

import core.billing.domain.model.SubscriptionPurchaseResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NoOpSubscriptionBillingRepositoryTest {

    private val noOpSubscriptionBillingRepository = NoOpSubscriptionBillingRepository()

    @Test
    fun purchasePackage_failsWithoutStoreErrorCode() = runTest {
        val purchaseResult = noOpSubscriptionBillingRepository.purchasePackage(packageIdentifier = PACKAGE_IDENTIFIER)

        assertEquals(
            SubscriptionPurchaseResult.Failed(message = UNSUPPORTED_MESSAGE, storeErrorCode = null),
            purchaseResult,
        )
    }

    @Test
    fun queries_reportNoBillingSupportEntitlementOrPrices() = runTest {
        assertFalse(noOpSubscriptionBillingRepository.isSupported)
        assertFalse(noOpSubscriptionBillingRepository.hasActiveEntitlement())
        assertFalse(noOpSubscriptionBillingRepository.restorePurchases())
        assertEquals(emptyMap(), noOpSubscriptionBillingRepository.getOfferingPrices())
    }

    private companion object {
        const val PACKAGE_IDENTIFIER = "\$rc_annual"
        const val UNSUPPORTED_MESSAGE = "unsupported"
    }
}

package core.billing.domain.usecase

import core.billing.domain.model.SubscriptionPurchaseResult
import core.billing.domain.model.SubscriptionPurchaseStep
import core.billing.fake.FakePurchaseEventListener
import core.billing.fake.FakePurchaseEventListener.ReceivedPurchaseEvent
import core.billing.fake.FakeSubscriptionBillingRepository
import core.billing.fake.FakePurchaseEntryPoint
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PurchaseSubscriptionPlanUseCaseTest {

    private val purchaseEventListener = FakePurchaseEventListener()

    @Test
    fun invoke_whenPurchaseCompletes_notifiesStartedThenCompletedAndReturnsSuccess() = runTest {
        val purchaseResult = SubscriptionPurchaseResult.Success(hasEntitlement = true)

        val returnedPurchaseResult = purchaseWithStoreResult(storePurchaseResult = purchaseResult)

        assertEquals(purchaseResult, returnedPurchaseResult)
        assertEquals(expectedPurchaseEvents(finishedPurchaseResult = purchaseResult), purchaseEventListener.receivedPurchaseEvents)
    }

    @Test
    fun invoke_whenUserCancels_notifiesStartedThenCancelled() = runTest {
        val returnedPurchaseResult = purchaseWithStoreResult(storePurchaseResult = SubscriptionPurchaseResult.Cancelled)

        assertEquals(SubscriptionPurchaseResult.Cancelled, returnedPurchaseResult)
        assertEquals(
            expectedPurchaseEvents(finishedPurchaseResult = SubscriptionPurchaseResult.Cancelled),
            purchaseEventListener.receivedPurchaseEvents,
        )
    }

    @Test
    fun invoke_whenStoreFails_notifiesStartedThenFailedWithStoreErrorCode() = runTest {
        val purchaseResult = SubscriptionPurchaseResult.Failed(message = STORE_ERROR_MESSAGE, storeErrorCode = STORE_ERROR_CODE)

        val returnedPurchaseResult = purchaseWithStoreResult(storePurchaseResult = purchaseResult)

        assertEquals(purchaseResult, returnedPurchaseResult)
        assertEquals(expectedPurchaseEvents(finishedPurchaseResult = purchaseResult), purchaseEventListener.receivedPurchaseEvents)
    }

    @Test
    fun invoke_onPlatformWithoutBilling_failsWithoutStoreErrorCodeAndSkipsTheStore() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository(isSupported = false)
        val unsupportedPurchaseResult = SubscriptionPurchaseResult.Failed(message = UNSUPPORTED_MESSAGE, storeErrorCode = null)

        val returnedPurchaseResult = createUseCase(subscriptionBillingRepository = subscriptionBillingRepository)(
            packageIdentifier = PACKAGE_IDENTIFIER,
            entryPoint = FakePurchaseEntryPoint.PAYWALL,
        )

        assertEquals(unsupportedPurchaseResult, returnedPurchaseResult)
        assertEquals(emptyList(), subscriptionBillingRepository.purchasedPackageIdentifiers)
        assertEquals(
            expectedPurchaseEvents(finishedPurchaseResult = unsupportedPurchaseResult),
            purchaseEventListener.receivedPurchaseEvents,
        )
    }

    private suspend fun purchaseWithStoreResult(storePurchaseResult: SubscriptionPurchaseResult): SubscriptionPurchaseResult {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository().apply {
            purchaseResult = storePurchaseResult
        }

        val returnedPurchaseResult = createUseCase(subscriptionBillingRepository = subscriptionBillingRepository)(
            packageIdentifier = PACKAGE_IDENTIFIER,
            entryPoint = FakePurchaseEntryPoint.PAYWALL,
        )

        assertEquals(listOf(PACKAGE_IDENTIFIER), subscriptionBillingRepository.purchasedPackageIdentifiers)
        return returnedPurchaseResult
    }

    private fun createUseCase(subscriptionBillingRepository: FakeSubscriptionBillingRepository) =
        PurchaseSubscriptionPlanUseCase(
            subscriptionBillingRepository = subscriptionBillingRepository,
            purchaseEventListener = purchaseEventListener,
        )

    private fun expectedPurchaseEvents(finishedPurchaseResult: SubscriptionPurchaseResult) = listOf(
        ReceivedPurchaseEvent(
            purchaseStep = SubscriptionPurchaseStep.Started,
            packageIdentifier = PACKAGE_IDENTIFIER,
            entryPointName = ENTRY_POINT_NAME,
        ),
        ReceivedPurchaseEvent(
            purchaseStep = SubscriptionPurchaseStep.Finished(purchaseResult = finishedPurchaseResult),
            packageIdentifier = PACKAGE_IDENTIFIER,
            entryPointName = ENTRY_POINT_NAME,
        ),
    )

    private companion object {
        const val PACKAGE_IDENTIFIER = "\$rc_annual"
        const val ENTRY_POINT_NAME = "PAYWALL"
        const val UNSUPPORTED_MESSAGE = "unsupported"
        const val STORE_ERROR_CODE = "STORE_PROBLEM_ERROR"
        const val STORE_ERROR_MESSAGE = "There was a problem with the store."
    }
}

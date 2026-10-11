package core.billing.domain.usecase

import core.billing.domain.PurchaseEventListener
import core.billing.domain.model.PurchaseEntryPoint
import core.billing.domain.model.SubscriptionPurchaseResult
import core.billing.domain.model.SubscriptionPurchaseStep
import core.billing.domain.model.UNSUPPORTED_PURCHASE_MESSAGE
import core.billing.domain.repository.SubscriptionBillingRepository

class PurchaseSubscriptionPlanUseCase(
    private val subscriptionBillingRepository: SubscriptionBillingRepository,
    private val purchaseEventListener: PurchaseEventListener
) {
    suspend operator fun invoke(
        packageIdentifier: String,
        entryPoint: PurchaseEntryPoint,
    ): SubscriptionPurchaseResult {
        purchaseEventListener.onPurchaseStep(
            purchaseStep = SubscriptionPurchaseStep.Started,
            packageIdentifier = packageIdentifier,
            entryPoint = entryPoint,
        )

        val purchaseResult = purchasePackage(packageIdentifier = packageIdentifier)
        purchaseEventListener.onPurchaseStep(
            purchaseStep = SubscriptionPurchaseStep.Finished(purchaseResult = purchaseResult),
            packageIdentifier = packageIdentifier,
            entryPoint = entryPoint,
        )

        return purchaseResult
    }

    private suspend fun purchasePackage(packageIdentifier: String): SubscriptionPurchaseResult {
        if (!subscriptionBillingRepository.isSupported) {
            return SubscriptionPurchaseResult.Failed(message = UNSUPPORTED_PURCHASE_MESSAGE, storeErrorCode = null)
        }
        return subscriptionBillingRepository.purchasePackage(packageIdentifier = packageIdentifier)
    }
}

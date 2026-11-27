package core.billing.domain.usecase

import core.billing.domain.repository.SubscriptionBillingRepository

class RestorePurchasesUseCase(
    private val subscriptionBillingRepository: SubscriptionBillingRepository
) {
    suspend operator fun invoke(): Boolean {
        if (!subscriptionBillingRepository.isSupported) return false
        return subscriptionBillingRepository.restorePurchases()
    }
}

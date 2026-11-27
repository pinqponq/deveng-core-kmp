package core.billing.domain.usecase

import core.billing.domain.repository.SubscriptionBillingRepository

class HasActiveEntitlementUseCase(
    private val subscriptionBillingRepository: SubscriptionBillingRepository
) {
    suspend operator fun invoke(): Boolean {
        if (!subscriptionBillingRepository.isSupported) return false
        return subscriptionBillingRepository.hasActiveEntitlement()
    }
}

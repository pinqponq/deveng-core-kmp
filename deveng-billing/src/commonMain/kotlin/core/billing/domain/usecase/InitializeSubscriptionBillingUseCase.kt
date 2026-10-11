package core.billing.domain.usecase

import core.billing.domain.BillingAppUserIdProvider
import core.billing.domain.repository.SubscriptionBillingRepository

class InitializeSubscriptionBillingUseCase(
    private val subscriptionBillingRepository: SubscriptionBillingRepository,
    private val billingAppUserIdProvider: BillingAppUserIdProvider
) {
    operator fun invoke() {
        if (!subscriptionBillingRepository.isSupported) return

        val appUserId = billingAppUserIdProvider.provideAppUserId()
        subscriptionBillingRepository.configure(appUserId = appUserId)
    }
}

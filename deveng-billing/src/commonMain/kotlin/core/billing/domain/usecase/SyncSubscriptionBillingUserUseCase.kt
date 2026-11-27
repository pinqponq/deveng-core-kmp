package core.billing.domain.usecase

import core.billing.domain.BillingAppUserIdProvider
import core.billing.domain.repository.SubscriptionBillingRepository

class SyncSubscriptionBillingUserUseCase(
    private val subscriptionBillingRepository: SubscriptionBillingRepository,
    private val billingAppUserIdProvider: BillingAppUserIdProvider
) {
    suspend operator fun invoke() {
        if (!subscriptionBillingRepository.isSupported) return

        subscriptionBillingRepository.syncAppUserId(
            appUserId = billingAppUserIdProvider.provideAppUserId()
        )
    }
}

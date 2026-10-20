package core.billing.domain.usecase

import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.repository.SubscriptionBillingRepository

class FetchOfferPricesUseCase(
    private val subscriptionBillingRepository: SubscriptionBillingRepository
) {
    suspend operator fun invoke(): Map<String, SubscriptionOfferPrice> {
        if (!subscriptionBillingRepository.isSupported) return emptyMap()
        return subscriptionBillingRepository.fetchOfferingPrices()
    }
}

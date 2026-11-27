package core.billing.data

import core.billing.data.mapper.RevenueCatOfferPriceMapper
import core.billing.domain.model.BillingConfig
import core.billing.domain.repository.SubscriptionBillingRepository

actual fun createSubscriptionBillingRepository(billingConfig: BillingConfig): SubscriptionBillingRepository =
    RevenueCatSubscriptionBillingRepositoryImpl(
        billingConfig = billingConfig,
        revenueCatPurchasesClient = DefaultRevenueCatPurchasesClient(),
        revenueCatOfferPriceMapper = RevenueCatOfferPriceMapper(),
    )

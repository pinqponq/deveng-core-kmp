package core.billing.data

import core.billing.domain.model.BillingConfig
import core.billing.domain.repository.SubscriptionBillingRepository

expect fun createSubscriptionBillingRepository(billingConfig: BillingConfig): SubscriptionBillingRepository

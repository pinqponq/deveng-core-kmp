package core.billing.data

import core.billing.domain.model.BillingConfig
import core.billing.domain.repository.SubscriptionBillingRepository

actual fun createSubscriptionBillingRepository(billingConfig: BillingConfig): SubscriptionBillingRepository =
    UnsupportedSubscriptionBillingRepository()

package core.billing.data

import core.billing.domain.model.BillingConfig

internal expect fun BillingConfig.resolvePlatformApiKey(): String

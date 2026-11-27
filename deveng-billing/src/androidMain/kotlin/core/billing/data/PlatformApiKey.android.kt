package core.billing.data

import core.billing.domain.model.BillingConfig

internal actual fun BillingConfig.resolvePlatformApiKey(): String = androidApiKey

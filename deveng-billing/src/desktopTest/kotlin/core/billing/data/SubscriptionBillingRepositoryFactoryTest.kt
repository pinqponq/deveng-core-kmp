package core.billing.data

import core.billing.domain.model.BillingConfig
import core.billing.domain.model.BillingLogLevel
import kotlin.test.Test
import kotlin.test.assertIs

class SubscriptionBillingRepositoryFactoryTest {

    @Test
    fun create_onDesktop_returnsUnsupportedRepository() {
        val subscriptionBillingRepository = createSubscriptionBillingRepository(
            billingConfig = BillingConfig(
                entitlementId = ENTITLEMENT_ID,
                androidApiKey = ANDROID_API_KEY,
                iosApiKey = IOS_API_KEY,
                logLevel = BillingLogLevel.WARN,
            ),
        )

        assertIs<UnsupportedSubscriptionBillingRepository>(subscriptionBillingRepository)
    }

    private companion object {
        const val ENTITLEMENT_ID = "pro"
        const val ANDROID_API_KEY = "goog_test"
        const val IOS_API_KEY = "appl_test"
    }
}

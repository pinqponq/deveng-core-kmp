package core.billing.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith

class BillingConfigTest {

    @Test
    fun create_withBlankEntitlementId_failsAtStartup() {
        assertFailsWith<IllegalArgumentException> {
            BillingConfig(
                entitlementId = " ",
                androidApiKey = ANDROID_API_KEY,
                iosApiKey = IOS_API_KEY,
                logLevel = BillingLogLevel.WARN,
            )
        }
    }

    private companion object {
        const val ANDROID_API_KEY = "goog_test"
        const val IOS_API_KEY = "appl_test"
    }
}

package core.analytics.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonAnalyticsEventTest {

    @Test
    fun entries_keepTheNamesAlreadySentToPinqloqFirebaseAndMeta() {
        val sentNames = CommonAnalyticsEvent.entries.map { event ->
            listOf(event.pinqloqName, event.firebaseName, event.metaEventName, event.shouldFireOnce.toString())
        }

        assertEquals(
            listOf(
                listOf("INSTALL_ATTRIBUTED", "install_attributed", "install_attributed", "true"),
                listOf("ONBOARDING_COMPLETED", "onboarding_completed", "onboarding_completed", "true"),
                listOf("SIGNUP_COMPLETED", "signup_completed", "fb_mobile_complete_registration", "true"),
                listOf("ACCOUNT_VERIFIED", "account_verified", "account_verified", "true"),
                listOf("LOGIN_COMPLETED", "login_completed", "login_completed", "false"),
                listOf("SUBSCRIPTION_PURCHASED", "subscription_purchased", "Subscribe", "false"),
                listOf("SUBSCRIPTION_RESTORED", "subscription_restored", "subscription_restored", "false"),
                listOf("ACCOUNT_DELETED", "account_deleted", "account_deleted", "false"),
                listOf("REWARDED_AD_COMPLETED", "rewarded_ad_completed", "rewarded_ad_completed", "false"),
            ),
            sentNames,
        )
    }
}

package core.analytics.domain.model

enum class CommonAnalyticsEvent(
    override val pinqloqName: String,
    override val firebaseName: String,
    override val metaEventName: String,
    override val shouldFireOnce: Boolean
) : AnalyticsEvent {
    INSTALL_ATTRIBUTED("INSTALL_ATTRIBUTED", "install_attributed", "install_attributed", shouldFireOnce = true),
    ONBOARDING_COMPLETED("ONBOARDING_COMPLETED", "onboarding_completed", "onboarding_completed", shouldFireOnce = true),
    SIGNUP_COMPLETED("SIGNUP_COMPLETED", "signup_completed", "fb_mobile_complete_registration", shouldFireOnce = true),
    ACCOUNT_VERIFIED("ACCOUNT_VERIFIED", "account_verified", "account_verified", shouldFireOnce = true),
    LOGIN_COMPLETED("LOGIN_COMPLETED", "login_completed", "login_completed", shouldFireOnce = false),
    SUBSCRIPTION_PURCHASED("SUBSCRIPTION_PURCHASED", "subscription_purchased", "Subscribe", shouldFireOnce = false),
    SUBSCRIPTION_RESTORED("SUBSCRIPTION_RESTORED", "subscription_restored", "subscription_restored", shouldFireOnce = false),
    ACCOUNT_DELETED("ACCOUNT_DELETED", "account_deleted", "account_deleted", shouldFireOnce = false),
    REWARDED_AD_COMPLETED("REWARDED_AD_COMPLETED", "rewarded_ad_completed", "rewarded_ad_completed", shouldFireOnce = false)
}

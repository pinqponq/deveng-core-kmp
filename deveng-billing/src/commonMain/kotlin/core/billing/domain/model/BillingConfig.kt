package core.billing.domain.model

data class BillingConfig(
    val entitlementId: String,
    val androidApiKey: String,
    val iosApiKey: String,
    val logLevel: BillingLogLevel,
) {
    init {
        require(entitlementId.isNotBlank()) { "BillingConfig.entitlementId must not be blank" }
    }
}

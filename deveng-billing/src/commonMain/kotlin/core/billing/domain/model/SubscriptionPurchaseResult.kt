package core.billing.domain.model

sealed interface SubscriptionPurchaseResult {
    data class Success(val hasEntitlement: Boolean) : SubscriptionPurchaseResult
    data object Cancelled : SubscriptionPurchaseResult
    /** [storeErrorCode] is the RevenueCat error code name; null when the purchase failed before reaching the store. */
    data class Failed(val message: String?, val storeErrorCode: String?) : SubscriptionPurchaseResult
}

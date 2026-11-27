package core.billing.domain.model

sealed class SubscriptionPurchaseStep {
    data object Started : SubscriptionPurchaseStep()

    data class Finished(val purchaseResult: SubscriptionPurchaseResult) : SubscriptionPurchaseStep()
}

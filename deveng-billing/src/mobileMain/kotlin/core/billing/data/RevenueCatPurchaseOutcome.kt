package core.billing.data

internal sealed interface RevenueCatPurchaseOutcome {
    data class Purchased(val hasEntitlement: Boolean) : RevenueCatPurchaseOutcome
    data object PackageNotFound : RevenueCatPurchaseOutcome
}

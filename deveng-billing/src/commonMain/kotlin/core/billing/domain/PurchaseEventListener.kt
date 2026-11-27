package core.billing.domain

import core.billing.domain.model.PurchaseEntryPoint
import core.billing.domain.model.SubscriptionPurchaseStep

/**
 * Receives each step of a purchase attempt. RevenueCat only records completed purchases, so this is
 * the only place a cancelled store sheet or a store error can be reported (e.g. to logging or analytics).
 */
fun interface PurchaseEventListener {
    suspend fun onPurchaseStep(
        purchaseStep: SubscriptionPurchaseStep,
        packageIdentifier: String,
        entryPoint: PurchaseEntryPoint,
    )
}

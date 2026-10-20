package core.billing.fake

import core.billing.domain.PurchaseEventListener
import core.billing.domain.model.PurchaseEntryPoint
import core.billing.domain.model.SubscriptionPurchaseStep

class FakePurchaseEventListener : PurchaseEventListener {

    val receivedPurchaseEvents = mutableListOf<ReceivedPurchaseEvent>()

    override suspend fun onPurchaseStep(
        purchaseStep: SubscriptionPurchaseStep,
        packageIdentifier: String,
        entryPoint: PurchaseEntryPoint,
    ) {
        receivedPurchaseEvents += ReceivedPurchaseEvent(
            purchaseStep = purchaseStep,
            packageIdentifier = packageIdentifier,
            entryPointName = entryPoint.name,
        )
    }

    data class ReceivedPurchaseEvent(
        val purchaseStep: SubscriptionPurchaseStep,
        val packageIdentifier: String,
        val entryPointName: String,
    )
}

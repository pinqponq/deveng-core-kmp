package core.billing.domain.model

data class SubscriptionOfferPrice(
    val totalPriceText: String,
    val totalPriceMicros: Long,
    val pricePerMonthText: String?,
    val pricePerMonthMicros: Long?,
    val periodUnit: SubscriptionPeriodUnit?,
    val periodValue: Int?
)

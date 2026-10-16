package core.billing.data.mapper

import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PeriodUnit
import com.revenuecat.purchases.kmp.models.StoreProduct
import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.SubscriptionPeriodUnit

internal class RevenueCatOfferPriceMapper {
    fun mapToDomain(storeProduct: StoreProduct): SubscriptionOfferPrice {
        return SubscriptionOfferPrice(
            totalPriceText = storeProduct.price.formatted,
            totalPriceMicros = storeProduct.price.amountMicros,
            pricePerMonthText = storeProduct.pricePerMonth?.formatted,
            pricePerMonthMicros = storeProduct.pricePerMonth?.amountMicros,
            periodUnit = storeProduct.period?.let { mapPeriodUnit(period = it) },
            periodValue = storeProduct.period?.value
        )
    }

    private fun mapPeriodUnit(period: Period): SubscriptionPeriodUnit? = when (period.unit) {
        PeriodUnit.DAY -> SubscriptionPeriodUnit.DAY
        PeriodUnit.WEEK -> SubscriptionPeriodUnit.WEEK
        PeriodUnit.MONTH -> SubscriptionPeriodUnit.MONTH
        PeriodUnit.YEAR -> SubscriptionPeriodUnit.YEAR
        PeriodUnit.UNKNOWN -> null
    }
}

package core.billing.data.mapper

import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PeriodUnit
import com.revenuecat.purchases.kmp.models.Price
import core.billing.domain.model.SubscriptionOfferPrice
import core.billing.domain.model.SubscriptionPeriodUnit
import core.billing.fake.FakeStoreProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RevenueCatOfferPriceMapperTest {

    private val revenueCatOfferPriceMapper = RevenueCatOfferPriceMapper()

    @Test
    fun mapToDomain_withAnnualProduct_mapsTotalAndMonthlyPricesAndPeriod() {
        val offerPrice = revenueCatOfferPriceMapper.mapToDomain(
            storeProduct = FakeStoreProduct(
                price = ANNUAL_PRICE,
                period = Period(value = 1, unit = PeriodUnit.YEAR),
                pricePerMonth = MONTHLY_EQUIVALENT_PRICE,
            ),
        )

        assertEquals(
            SubscriptionOfferPrice(
                totalPriceText = ANNUAL_PRICE.formatted,
                totalPriceMicros = ANNUAL_PRICE.amountMicros,
                pricePerMonthText = MONTHLY_EQUIVALENT_PRICE.formatted,
                pricePerMonthMicros = MONTHLY_EQUIVALENT_PRICE.amountMicros,
                periodUnit = SubscriptionPeriodUnit.YEAR,
                periodValue = 1,
            ),
            offerPrice,
        )
    }

    @Test
    fun mapToDomain_withEachKnownPeriodUnit_mapsToTheSameUnit() {
        val periodUnits = mapOf(
            PeriodUnit.DAY to SubscriptionPeriodUnit.DAY,
            PeriodUnit.WEEK to SubscriptionPeriodUnit.WEEK,
            PeriodUnit.MONTH to SubscriptionPeriodUnit.MONTH,
            PeriodUnit.YEAR to SubscriptionPeriodUnit.YEAR,
        )

        periodUnits.forEach { (storePeriodUnit, expectedPeriodUnit) ->
            val offerPrice = revenueCatOfferPriceMapper.mapToDomain(
                storeProduct = FakeStoreProduct(
                    price = ANNUAL_PRICE,
                    period = Period(value = 3, unit = storePeriodUnit),
                    pricePerMonth = null,
                ),
            )

            assertEquals(expectedPeriodUnit, offerPrice.periodUnit)
            assertEquals(3, offerPrice.periodValue)
        }
    }

    @Test
    fun mapToDomain_withUnknownPeriodUnit_leavesPeriodUnitEmpty() {
        val offerPrice = revenueCatOfferPriceMapper.mapToDomain(
            storeProduct = FakeStoreProduct(
                price = ANNUAL_PRICE,
                period = Period(value = 1, unit = PeriodUnit.UNKNOWN),
                pricePerMonth = null,
            ),
        )

        assertNull(offerPrice.periodUnit)
        assertEquals(1, offerPrice.periodValue)
    }

    @Test
    fun mapToDomain_withLifetimeProduct_leavesPeriodAndMonthlyPriceEmpty() {
        val offerPrice = revenueCatOfferPriceMapper.mapToDomain(
            storeProduct = FakeStoreProduct(price = ANNUAL_PRICE, period = null, pricePerMonth = null),
        )

        assertNull(offerPrice.periodUnit)
        assertNull(offerPrice.periodValue)
        assertNull(offerPrice.pricePerMonthText)
        assertNull(offerPrice.pricePerMonthMicros)
    }

    private companion object {
        val ANNUAL_PRICE = Price(formatted = "₺499,99", amountMicros = 499_990_000, currencyCode = "TRY")
        val MONTHLY_EQUIVALENT_PRICE = Price(formatted = "₺41,66", amountMicros = 41_660_000, currencyCode = "TRY")
    }
}

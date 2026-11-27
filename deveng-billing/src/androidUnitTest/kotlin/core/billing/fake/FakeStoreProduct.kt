package core.billing.fake

import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PresentedOfferingContext
import com.revenuecat.purchases.kmp.models.Price
import com.revenuecat.purchases.kmp.models.ProductCategory
import com.revenuecat.purchases.kmp.models.ProductType
import com.revenuecat.purchases.kmp.models.PurchasingData
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreProductDiscount
import com.revenuecat.purchases.kmp.models.SubscriptionOption
import com.revenuecat.purchases.kmp.models.SubscriptionOptions

class FakeStoreProduct(
    override val price: Price,
    override val period: Period?,
    override val pricePerMonth: Price?,
) : StoreProduct {
    override val id: String get() = notFaked()
    override val type: ProductType get() = notFaked()
    override val category: ProductCategory? get() = notFaked()
    override val title: String get() = notFaked()
    override val localizedDescription: String? get() = notFaked()
    override val subscriptionOptions: SubscriptionOptions? get() = notFaked()
    override val defaultOption: SubscriptionOption? get() = notFaked()
    override val discounts: List<StoreProductDiscount> get() = notFaked()
    override val introductoryDiscount: StoreProductDiscount? get() = notFaked()
    override val purchasingData: PurchasingData get() = notFaked()
    override val presentedOfferingContext: PresentedOfferingContext? get() = notFaked()
    override val pricePerWeek: Price? get() = notFaked()
    override val pricePerYear: Price? get() = notFaked()

    private fun notFaked(): Nothing = throw UnsupportedOperationException("Not faked in FakeStoreProduct")
}

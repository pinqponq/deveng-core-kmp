package core.billing.domain

fun interface BillingAppUserIdProvider {
    fun provideAppUserId(): String?
}

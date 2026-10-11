package core.billing.domain.usecase

import core.billing.domain.BillingAppUserIdProvider
import core.billing.fake.FakeSubscriptionBillingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionBillingUserUseCasesTest {

    private val billingAppUserIdProvider = BillingAppUserIdProvider { APP_USER_ID }

    @Test
    fun initialize_configuresStoreWithProvidedAppUserId() {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository()

        InitializeSubscriptionBillingUseCase(
            subscriptionBillingRepository = subscriptionBillingRepository,
            billingAppUserIdProvider = billingAppUserIdProvider,
        )()

        assertEquals(listOf<String?>(APP_USER_ID), subscriptionBillingRepository.configuredAppUserIds)
    }

    @Test
    fun initialize_onPlatformWithoutBilling_skipsConfiguration() {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository(isSupported = false)

        InitializeSubscriptionBillingUseCase(
            subscriptionBillingRepository = subscriptionBillingRepository,
            billingAppUserIdProvider = billingAppUserIdProvider,
        )()

        assertEquals(emptyList(), subscriptionBillingRepository.configuredAppUserIds)
    }

    @Test
    fun sync_whenSignedOut_syncsAnonymousUser() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository()

        SyncSubscriptionBillingUserUseCase(
            subscriptionBillingRepository = subscriptionBillingRepository,
            billingAppUserIdProvider = { null },
        )()

        assertEquals(listOf<String?>(null), subscriptionBillingRepository.syncedAppUserIds)
    }

    @Test
    fun sync_whenSignedIn_syncsProvidedAppUserId() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository()

        SyncSubscriptionBillingUserUseCase(
            subscriptionBillingRepository = subscriptionBillingRepository,
            billingAppUserIdProvider = billingAppUserIdProvider,
        )()

        assertEquals(listOf<String?>(APP_USER_ID), subscriptionBillingRepository.syncedAppUserIds)
    }

    @Test
    fun sync_onPlatformWithoutBilling_skipsSync() = runTest {
        val subscriptionBillingRepository = FakeSubscriptionBillingRepository(isSupported = false)

        SyncSubscriptionBillingUserUseCase(
            subscriptionBillingRepository = subscriptionBillingRepository,
            billingAppUserIdProvider = billingAppUserIdProvider,
        )()

        assertEquals(emptyList(), subscriptionBillingRepository.syncedAppUserIds)
    }

    private companion object {
        const val APP_USER_ID = "42"
    }
}

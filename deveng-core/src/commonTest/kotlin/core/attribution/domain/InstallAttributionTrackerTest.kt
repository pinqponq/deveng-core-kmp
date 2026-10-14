package core.attribution.domain

import core.analytics.domain.ConversionEventTracker
import core.analytics.domain.model.AnalyticsEvent
import core.analytics.domain.model.CommonAnalyticsEvent
import core.analytics.fake.FakeAnalyticsLogSink
import core.analytics.fake.FakeConversionAnalyticsClient
import core.analytics.fake.FakeConversionEventStore
import core.attribution.data.datasource.remote.model.mapper.InstallReferrerAttributionMapper
import core.attribution.data.repository.InstallReferrerAttributionRepositoryImpl
import core.attribution.domain.model.InstallAttribution
import core.attribution.domain.repository.AttributionRepository
import core.attribution.fake.FakeAttributionRepository
import core.attribution.fake.FakeAttributionTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InstallAttributionTrackerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val analyticsLogSink = FakeAnalyticsLogSink()
    private val conversionAnalyticsClient = FakeConversionAnalyticsClient()
    private val conversionEventStore = FakeConversionEventStore()

    @Test
    fun track_withGoogleAdsInstallReferrer_firesInstallAttributedWithCampaignParams() {
        createTracker(installReferrer = GOOGLE_ADS_REFERRER).track()

        assertEquals(listOf<AnalyticsEvent>(CommonAnalyticsEvent.INSTALL_ATTRIBUTED), conversionAnalyticsClient.loggedEvents)
        assertEquals(
            mapOf(
                "googleClickId" to GOOGLE_CLICK_ID,
                "campaignSource" to "google",
                "campaignMedium" to "cpc",
                "campaignName" to "ukus3",
                "campaignTerm" to "couple app",
                "campaignContent" to "video_1",
            ),
            analyticsLogSink.savedLogs.single().detail,
        )
        assertTrue(conversionEventStore.hasFired(INSTALL_ATTRIBUTED_KEY))
    }

    @Test
    fun track_withInviteCodeInGoogleAdsReferrer_firesCampaignParamsWithoutInviteCode() {
        createTracker(installReferrer = "$GOOGLE_ADS_REFERRER&invite_code=$INVITE_CODE").track()

        val trackedParams = analyticsLogSink.savedLogs.single().detail
        assertEquals(GOOGLE_CLICK_ID, trackedParams["googleClickId"])
        assertFalse(trackedParams.values.contains(INVITE_CODE))
    }

    @Test
    fun track_withAppleSearchAdsAttribution_firesInstallAttributedWithAdIds() {
        val attributionRepository = FakeAttributionRepository(attribution = APPLE_SEARCH_ADS_ATTRIBUTION)

        createTracker(installReferrer = ATTRIBUTION_TOKEN, attributionRepository = attributionRepository).track()

        assertEquals(
            mapOf(
                "campaignId" to "11",
                "adGroupId" to "12",
                "keywordId" to "13",
                "orgId" to "14",
                "adId" to "15",
                "conversionType" to "Download",
                "countryOrRegion" to "US",
                "clickDate" to CLICK_DATE,
            ),
            analyticsLogSink.savedLogs.single().detail,
        )
    }

    @Test
    fun track_withOrganicInstallReferrer_marksFiredWithoutFiringEvent() {
        createTracker(installReferrer = ORGANIC_REFERRER).track()

        assertTrue(conversionAnalyticsClient.loggedEvents.isEmpty())
        assertTrue(analyticsLogSink.savedLogs.isEmpty())
        assertTrue(conversionEventStore.hasFired(INSTALL_ATTRIBUTED_KEY))
    }

    @Test
    fun track_withoutInstallReferrer_firesNothingAndRetriesLater() {
        createTracker(installReferrer = null).track()

        assertTrue(conversionAnalyticsClient.loggedEvents.isEmpty())
        assertFalse(conversionEventStore.hasFired(INSTALL_ATTRIBUTED_KEY))
    }

    @Test
    fun track_whenTokenCannotBeResolved_firesNothingAndRetriesLater() {
        val attributionRepository = FakeAttributionRepository(attribution = null)

        createTracker(installReferrer = ATTRIBUTION_TOKEN, attributionRepository = attributionRepository).track()

        assertTrue(conversionAnalyticsClient.loggedEvents.isEmpty())
        assertFalse(conversionEventStore.hasFired(INSTALL_ATTRIBUTED_KEY))
    }

    @Test
    fun track_whenResolvingFails_firesNothingAndRetriesLater() {
        val attributionRepository = FakeAttributionRepository(failure = IllegalStateException(NETWORK_ERROR_MESSAGE))

        createTracker(installReferrer = ATTRIBUTION_TOKEN, attributionRepository = attributionRepository).track()

        assertTrue(conversionAnalyticsClient.loggedEvents.isEmpty())
        assertFalse(conversionEventStore.hasFired(INSTALL_ATTRIBUTED_KEY))
    }

    @Test
    fun track_afterAttributionAlreadyResolved_firesNothing() {
        conversionEventStore.markFired(INSTALL_ATTRIBUTED_KEY)

        createTracker(installReferrer = GOOGLE_ADS_REFERRER).track()

        assertTrue(conversionAnalyticsClient.loggedEvents.isEmpty())
    }

    private fun createTracker(
        installReferrer: String?,
        attributionRepository: AttributionRepository = InstallReferrerAttributionRepositoryImpl(
            installReferrerAttributionMapper = InstallReferrerAttributionMapper(),
        ),
    ): InstallAttributionTracker {
        val applicationScope = CoroutineScope(testDispatcher)
        val conversionEventTracker = ConversionEventTracker(
            analyticsLogSink = analyticsLogSink,
            conversionAnalyticsClient = conversionAnalyticsClient,
            conversionEventStore = conversionEventStore,
            applicationScope = applicationScope,
        )

        return InstallAttributionTracker(
            attributionTokenProvider = FakeAttributionTokenProvider(attributionToken = installReferrer),
            attributionRepository = attributionRepository,
            conversionEventStore = conversionEventStore,
            conversionEventTracker = conversionEventTracker,
            applicationScope = applicationScope,
        )
    }

    private companion object {
        const val INSTALL_ATTRIBUTED_KEY = "INSTALL_ATTRIBUTED"
        const val INVITE_CODE = "ABC123"
        const val GOOGLE_CLICK_ID = "Cj0KCQjwtest"
        const val GOOGLE_ADS_REFERRER = "gclid=$GOOGLE_CLICK_ID&utm_source=google&utm_medium=cpc" +
            "&utm_campaign=ukus3&utm_term=couple%20app&utm_content=video_1"
        const val ORGANIC_REFERRER = "utm_source=google-play&utm_medium=organic"
        const val ATTRIBUTION_TOKEN = "apple-attribution-token"
        const val CLICK_DATE = "2026-10-01T10:00Z"
        const val NETWORK_ERROR_MESSAGE = "Network unreachable."
        val APPLE_SEARCH_ADS_ATTRIBUTION = InstallAttribution(
            isAttributed = true,
            organizationId = 14,
            campaignId = 11,
            adGroupId = 12,
            keywordId = 13,
            adId = 15,
            conversionType = "Download",
            countryOrRegion = "US",
            clickDate = CLICK_DATE,
            googleClickId = null,
            campaignSource = null,
            campaignMedium = null,
            campaignName = null,
            campaignTerm = null,
            campaignContent = null,
            inviteCode = null,
        )
    }
}

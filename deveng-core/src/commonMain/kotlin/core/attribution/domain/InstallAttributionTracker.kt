package core.attribution.domain

import core.analytics.domain.ConversionEventTracker
import core.analytics.domain.model.CommonAnalyticsEvent
import core.analytics.domain.store.ConversionEventStore
import core.attribution.domain.model.InstallAttribution
import core.attribution.domain.repository.AttributionRepository
import core.util.CustomLogger.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val PARAM_CAMPAIGN_ID = "campaignId"
private const val PARAM_AD_GROUP_ID = "adGroupId"
private const val PARAM_KEYWORD_ID = "keywordId"
private const val PARAM_ORG_ID = "orgId"
private const val PARAM_AD_ID = "adId"
private const val PARAM_CONVERSION_TYPE = "conversionType"
private const val PARAM_COUNTRY_OR_REGION = "countryOrRegion"
private const val PARAM_CLICK_DATE = "clickDate"
private const val PARAM_GOOGLE_CLICK_ID = "googleClickId"
private const val PARAM_CAMPAIGN_SOURCE = "campaignSource"
private const val PARAM_CAMPAIGN_MEDIUM = "campaignMedium"
private const val PARAM_CAMPAIGN_NAME = "campaignName"
private const val PARAM_CAMPAIGN_TERM = "campaignTerm"
private const val PARAM_CAMPAIGN_CONTENT = "campaignContent"

/**
 * Resolves the install-attribution source once per install and fires it as
 * [CommonAnalyticsEvent.INSTALL_ATTRIBUTED] to pinqloq (and the other analytics sinks) through
 * [ConversionEventTracker], with the campaign / ad-group / keyword ids as params. Runs on
 * [applicationScope] so it survives navigation.
 */
class InstallAttributionTracker(
    private val attributionTokenProvider: AttributionTokenProvider,
    private val attributionRepository: AttributionRepository,
    private val conversionEventStore: ConversionEventStore,
    private val conversionEventTracker: ConversionEventTracker,
    private val applicationScope: CoroutineScope,
) {
    fun track() {
        applicationScope.launch {
            if (conversionEventStore.hasFired(CommonAnalyticsEvent.INSTALL_ATTRIBUTED.pinqloqName)) return@launch

            try {
                val attributionToken = attributionTokenProvider.fetchAttributionToken() ?: return@launch
                val attribution = attributionRepository.resolveAttribution(attributionToken) ?: return@launch

                if (!attribution.attributed) {
                    conversionEventStore.markFired(CommonAnalyticsEvent.INSTALL_ATTRIBUTED.pinqloqName)
                    return@launch
                }

                conversionEventTracker.track(
                    event = CommonAnalyticsEvent.INSTALL_ATTRIBUTED,
                    params = attribution.toParams(),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                // Fire-and-forget: attribution is a background measurement, never surfaced to the user.
                log("InstallAttributionTracker error: $e")
            }
        }
    }

    private fun InstallAttribution.toParams(): Map<String, String> = buildMap {
        campaignId?.let { put(PARAM_CAMPAIGN_ID, it.toString()) }
        adGroupId?.let { put(PARAM_AD_GROUP_ID, it.toString()) }
        keywordId?.let { put(PARAM_KEYWORD_ID, it.toString()) }
        organizationId?.let { put(PARAM_ORG_ID, it.toString()) }
        adId?.let { put(PARAM_AD_ID, it.toString()) }
        conversionType?.let { put(PARAM_CONVERSION_TYPE, it) }
        countryOrRegion?.let { put(PARAM_COUNTRY_OR_REGION, it) }
        clickDate?.let { put(PARAM_CLICK_DATE, it) }
        googleClickId?.let { put(PARAM_GOOGLE_CLICK_ID, it) }
        campaignSource?.let { put(PARAM_CAMPAIGN_SOURCE, it) }
        campaignMedium?.let { put(PARAM_CAMPAIGN_MEDIUM, it) }
        campaignName?.let { put(PARAM_CAMPAIGN_NAME, it) }
        campaignTerm?.let { put(PARAM_CAMPAIGN_TERM, it) }
        campaignContent?.let { put(PARAM_CAMPAIGN_CONTENT, it) }
    }
}

package core.analytics.domain

import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.log.AnalyticsLogLevel
import core.analytics.domain.log.AnalyticsLogSink
import core.analytics.domain.model.AnalyticsEvent
import core.analytics.domain.store.ConversionEventStore
import core.util.CustomLogger.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val KEY_SOURCE = "source"
private const val KEY_TAG = "tag"
private const val SOURCE = "ConversionEventTracker"
private const val CONVERSION_TAG = "CONVERSION"

/**
 * Fires a conversion event to pinqloq (via [AnalyticsLogSink]) and to every platform analytics sink
 * (via [ConversionAnalyticsClient], which fans out to Firebase and Meta on mobile). Runs on
 * [applicationScope] so an event fired right before navigation is not cancelled with the caller's
 * scope. [AnalyticsEvent.fireOnce] milestones are de-duplicated per install through
 * [ConversionEventStore].
 */
class ConversionEventTracker(
    private val analyticsLogSink: AnalyticsLogSink,
    private val conversionAnalyticsClient: ConversionAnalyticsClient,
    private val conversionEventStore: ConversionEventStore,
    private val applicationScope: CoroutineScope,
) {
    fun track(event: AnalyticsEvent, params: Map<String, String> = emptyMap()) {
        applicationScope.launch {
            if (event.fireOnce && conversionEventStore.hasFired(event.pinqloqName)) return@launch

            analyticsLogSink.log(
                logLevel = AnalyticsLogLevel.INFORMATION,
                message = event.pinqloqName,
                metadata = mapOf(
                    KEY_SOURCE to SOURCE,
                    KEY_TAG to CONVERSION_TAG
                ),
                detail = params
            )

            try {
                conversionAnalyticsClient.logEvent(event, params)
            } catch (e: Exception) {
                log("ConversionEventTracker analytics error: $e")
            }

            if (event.fireOnce) conversionEventStore.markFired(event.pinqloqName)
        }
    }
}

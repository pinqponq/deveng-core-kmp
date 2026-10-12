package core.analytics.domain.log

/**
 * The app's own log pipeline (pinqloq) for conversion events and ad outcomes. The app implements it
 * with its pinqloq client, so this library does not depend on pinqloq directly.
 */
interface AnalyticsLogSink {
    suspend fun log(
        logLevel: AnalyticsLogLevel,
        message: String,
        metadata: Map<String, String>,
        detail: Map<String, String>
    )
}

package core.analytics.fake

import core.analytics.domain.log.AnalyticsLogLevel
import core.analytics.domain.log.AnalyticsLogSink

class FakeAnalyticsLogSink : AnalyticsLogSink {

    val savedLogs = mutableListOf<SavedLog>()

    override suspend fun log(
        logLevel: AnalyticsLogLevel,
        message: String,
        metadata: Map<String, String>,
        detail: Map<String, String>
    ) {
        savedLogs += SavedLog(logLevel = logLevel, message = message, metadata = metadata, detail = detail)
    }

    data class SavedLog(
        val logLevel: AnalyticsLogLevel,
        val message: String,
        val metadata: Map<String, String>,
        val detail: Map<String, String>
    )
}

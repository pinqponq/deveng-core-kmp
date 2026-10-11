package core.ads.domain.usecase

import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.analytics.domain.ConversionEventTracker
import core.analytics.domain.log.AnalyticsLogLevel
import core.analytics.domain.log.AnalyticsLogSink
import core.analytics.domain.model.CommonAnalyticsEvent

private const val SOURCE = "SendRewardedAdOutcomeUseCase"
private const val REWARDED_AD_TAG = "REWARDED_AD"
private const val UNKNOWN_EXCEPTION_TYPE = "Unknown"

class SendRewardedAdOutcomeUseCase(
    private val conversionEventTracker: ConversionEventTracker,
    private val analyticsLogSink: AnalyticsLogSink,
) {
    suspend operator fun invoke(outcome: RewardedAdOutcome, surface: RewardedAdSurface) {
        when (outcome) {
            RewardedAdOutcome.RewardEarned -> conversionEventTracker.track(
                event = CommonAnalyticsEvent.REWARDED_AD_COMPLETED,
                params = mapOf("surface" to surface.name),
            )

            is RewardedAdOutcome.Failure -> sendFailureLog(failure = outcome, surface = surface)
        }
    }

    private suspend fun sendFailureLog(failure: RewardedAdOutcome.Failure, surface: RewardedAdSurface) {
        analyticsLogSink.log(
            logLevel = AnalyticsLogLevel.WARNING,
            message = resolveEventName(failure = failure),
            metadata = mapOf(
                "source" to SOURCE,
                "tag" to REWARDED_AD_TAG,
            ),
            detail = buildMap {
                put("surface", surface.name)
                resolveException(failure = failure)?.let { exception ->
                    put("exceptionType", exception::class.simpleName ?: UNKNOWN_EXCEPTION_TYPE)
                    exception.message?.let { message -> put("errorMessage", message) }
                }
            },
        )
    }

    private fun resolveEventName(failure: RewardedAdOutcome.Failure): String = when (failure) {
        is RewardedAdOutcome.LoadFailed -> "RewardedAdLoadFailed"
        is RewardedAdOutcome.ShowFailed -> "RewardedAdShowFailed"
        RewardedAdOutcome.ClosedBeforeReward -> "RewardedAdClosedBeforeReward"
    }

    private fun resolveException(failure: RewardedAdOutcome.Failure): Throwable? = when (failure) {
        is RewardedAdOutcome.LoadFailed -> failure.exception
        is RewardedAdOutcome.ShowFailed -> failure.exception
        RewardedAdOutcome.ClosedBeforeReward -> null
    }
}

package core.ads.domain.usecase

import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.analytics.domain.ConversionEventTracker
import core.analytics.domain.log.AnalyticsLogLevel
import core.analytics.domain.log.AnalyticsLogSink
import core.analytics.domain.model.CommonAnalyticsEvent

private const val KEY_SOURCE = "source"
private const val KEY_TAG = "tag"
private const val KEY_SURFACE = "surface"
private const val KEY_EXCEPTION_TYPE = "exceptionType"
private const val KEY_ERROR_MESSAGE = "errorMessage"
private const val SOURCE = "SendRewardedAdOutcomeUseCase"
private const val REWARDED_AD_TAG = "REWARDED_AD"
private const val UNKNOWN_EXCEPTION_TYPE = "Unknown"
private const val LOAD_FAILED_EVENT = "RewardedAdLoadFailed"
private const val SHOW_FAILED_EVENT = "RewardedAdShowFailed"
private const val CLOSED_BEFORE_REWARD_EVENT = "RewardedAdClosedBeforeReward"

class SendRewardedAdOutcomeUseCase(
    private val conversionEventTracker: ConversionEventTracker,
    private val analyticsLogSink: AnalyticsLogSink,
) {
    suspend operator fun invoke(outcome: RewardedAdOutcome, surface: RewardedAdSurface) {
        when (outcome) {
            RewardedAdOutcome.RewardEarned -> conversionEventTracker.track(
                event = CommonAnalyticsEvent.REWARDED_AD_COMPLETED,
                params = mapOf(KEY_SURFACE to surface.name),
            )

            is RewardedAdOutcome.Failure -> sendFailureLog(failure = outcome, surface = surface)
        }
    }

    private suspend fun sendFailureLog(failure: RewardedAdOutcome.Failure, surface: RewardedAdSurface) {
        analyticsLogSink.log(
            logLevel = AnalyticsLogLevel.WARNING,
            message = resolveEventName(failure = failure),
            metadata = mapOf(
                KEY_SOURCE to SOURCE,
                KEY_TAG to REWARDED_AD_TAG,
            ),
            detail = buildMap {
                put(KEY_SURFACE, surface.name)
                resolveException(failure = failure)?.let { exception ->
                    put(KEY_EXCEPTION_TYPE, exception::class.simpleName ?: UNKNOWN_EXCEPTION_TYPE)
                    exception.message?.let { message -> put(KEY_ERROR_MESSAGE, message) }
                }
            },
        )
    }

    private fun resolveEventName(failure: RewardedAdOutcome.Failure): String = when (failure) {
        is RewardedAdOutcome.LoadFailed -> LOAD_FAILED_EVENT
        is RewardedAdOutcome.ShowFailed -> SHOW_FAILED_EVENT
        RewardedAdOutcome.ClosedBeforeReward -> CLOSED_BEFORE_REWARD_EVENT
    }

    private fun resolveException(failure: RewardedAdOutcome.Failure): Throwable? = when (failure) {
        is RewardedAdOutcome.LoadFailed -> failure.exception
        is RewardedAdOutcome.ShowFailed -> failure.exception
        RewardedAdOutcome.ClosedBeforeReward -> null
    }
}

package core.ads.domain.model

sealed class RewardedAdOutcome {
    data object RewardEarned : RewardedAdOutcome()

    sealed class Failure : RewardedAdOutcome()

    data class LoadFailed(val exception: Throwable) : Failure()

    data class ShowFailed(val exception: Throwable) : Failure()

    data object ClosedBeforeReward : Failure()
}

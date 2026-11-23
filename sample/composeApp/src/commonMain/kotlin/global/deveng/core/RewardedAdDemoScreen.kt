package global.deveng.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.ads.RewardedAdZone
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.config.domain.model.AdConfig
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase
import core.analytics.data.client.NoOpConversionAnalyticsClient
import core.analytics.domain.ConversionEventTracker
import core.analytics.domain.log.AnalyticsLogLevel
import core.analytics.domain.log.AnalyticsLogSink
import core.analytics.domain.store.ConversionEventStore
import core.presentation.component.CustomButton

private const val GOOGLE_TEST_REWARDED_AD_UNIT_ID_ANDROID = "ca-app-pub-3940256099942544/5224354917"
private const val GOOGLE_TEST_REWARDED_AD_UNIT_ID_IOS = "ca-app-pub-3940256099942544/1712485313"

private enum class DemoRewardedAdSurface : RewardedAdSurface {
    SAMPLE,
}

/**
 * Shows a rewarded ad from Google's test ad unit. The zone loads the ad when the screen opens, the
 * button asks for it, and the result line shows the reward or the failure. After a failure or a
 * closed ad, the button starts a new attempt. Desktop and Web have no ads, so nothing opens there.
 */
@Composable
internal fun RewardedAdDemoScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val adConfigHolder = remember {
        AdConfigHolder().apply {
            update(
                AdConfig(
                    rewardedAdUnitIdAndroid = GOOGLE_TEST_REWARDED_AD_UNIT_ID_ANDROID,
                    rewardedAdUnitIdIos = GOOGLE_TEST_REWARDED_AD_UNIT_ID_IOS,
                )
            )
        }
    }
    val sendRewardedAdOutcomeUseCase = remember {
        val analyticsLogSink = DemoAnalyticsLogSink()
        SendRewardedAdOutcomeUseCase(
            conversionEventTracker = ConversionEventTracker(
                analyticsLogSink = analyticsLogSink,
                conversionAnalyticsClient = NoOpConversionAnalyticsClient(),
                conversionEventStore = DemoConversionEventStore(),
                applicationScope = coroutineScope,
            ),
            analyticsLogSink = analyticsLogSink,
        )
    }
    var isAdRequested by remember { mutableStateOf(false) }
    var lastOutcomeText by remember { mutableStateOf("No ad shown yet.") }

    RewardedAdZone(
        show = isAdRequested,
        surface = DemoRewardedAdSurface.SAMPLE,
        adConfigHolder = adConfigHolder,
        sendRewardedAdOutcomeUseCase = sendRewardedAdOutcomeUseCase,
        onRewardEarned = {
            isAdRequested = false
            lastOutcomeText = "Reward earned."
        },
        onAdFailed = { failure ->
            isAdRequested = false
            lastOutcomeText = describeFailure(failure = failure)
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CustomButton(modifier = Modifier.fillMaxWidth(), text = "Back", onClick = onBack)
        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Show rewarded ad",
            onClick = { isAdRequested = true },
        )
        if (isAdRequested) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        Text(text = lastOutcomeText)
    }
}

private fun describeFailure(failure: RewardedAdOutcome.Failure): String = when (failure) {
    is RewardedAdOutcome.LoadFailed -> "The ad could not be loaded. Tap again to retry."
    is RewardedAdOutcome.ShowFailed -> "The ad could not be shown. Tap again to retry."
    RewardedAdOutcome.ClosedBeforeReward -> "The ad was closed before the reward."
}

private class DemoAnalyticsLogSink : AnalyticsLogSink {
    override suspend fun log(
        logLevel: AnalyticsLogLevel,
        message: String,
        metadata: Map<String, String>,
        detail: Map<String, String>,
    ) {
        println("RewardedAdDemo $logLevel $message $detail")
    }
}

private class DemoConversionEventStore : ConversionEventStore {
    private val firedEventKeys = mutableSetOf<String>()

    override fun hasFired(eventKey: String): Boolean = eventKey in firedEventKeys

    override fun markFired(eventKey: String) {
        firedEventKeys.add(eventKey)
    }

    override fun clear() {
        firedEventKeys.clear()
    }
}

package core.ads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase
import core.analytics.domain.ConversionEventTracker
import core.analytics.fake.FakeAnalyticsLogSink
import core.analytics.fake.FakeConversionAnalyticsClient
import core.analytics.fake.FakeConversionEventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RewardedAdZoneWasmJsTest {

    private val analyticsLogSink = FakeAnalyticsLogSink()
    private val sendRewardedAdOutcomeUseCase = SendRewardedAdOutcomeUseCase(
        conversionEventTracker = ConversionEventTracker(
            analyticsLogSink = analyticsLogSink,
            conversionAnalyticsClient = FakeConversionAnalyticsClient(),
            conversionEventStore = FakeConversionEventStore(),
            applicationScope = CoroutineScope(Dispatchers.Unconfined),
        ),
        analyticsLogSink = analyticsLogSink,
    )
    private val adFailureList = mutableListOf<RewardedAdOutcome.Failure>()
    private var isAdShowRequested by mutableStateOf(false)

    @Test
    fun show_onWeb_reportsLoadFailedSoTheCallerCanResetItsTrigger() = runComposeUiTest {
        setContent {
            RewardedAdZone(
                show = isAdShowRequested,
                surface = TestRewardedAdSurface.DECK,
                adConfigHolder = AdConfigHolder(),
                sendRewardedAdOutcomeUseCase = sendRewardedAdOutcomeUseCase,
                onRewardEarned = {},
                onAdFailed = { failure ->
                    adFailureList.add(failure)
                    isAdShowRequested = false
                },
            )
        }
        waitForIdle()
        assertTrue(adFailureList.isEmpty())

        isAdShowRequested = true
        waitForIdle()
        isAdShowRequested = true
        waitForIdle()

        assertEquals(2, adFailureList.size)
        assertIs<RewardedAdOutcome.LoadFailed>(adFailureList.first())
        assertTrue(analyticsLogSink.savedLogs.isEmpty())
    }

    private enum class TestRewardedAdSurface : RewardedAdSurface {
        DECK,
    }
}

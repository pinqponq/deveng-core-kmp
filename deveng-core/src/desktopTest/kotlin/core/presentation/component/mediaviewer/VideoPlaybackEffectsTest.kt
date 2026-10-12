package core.presentation.component.mediaviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import chaintech.videoplayer.host.MediaPlayerEvent
import core.presentation.component.mediaviewer.fake.FakeVideoPlaybackControls
import core.presentation.component.mediaviewer.fake.FakeVideoPlaybackControls.Companion.PAUSE
import core.presentation.component.mediaviewer.fake.FakeVideoPlaybackControls.Companion.PLAY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class VideoPlaybackEffectsTest {

    @Test
    fun composeActivePage_afterStartDelay_startsPlayback() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        mainClock.autoAdvance = false
        setContent { PlaybackEffects(controls = controls) }

        mainClock.advanceTimeBy(milliseconds = FRAME_MILLIS)
        assertFalse(PLAY in controls.calls)

        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)
        assertEquals(listOf(PLAY), controls.calls)
    }

    @Test
    fun composeInactivePage_withoutFocus_pausesWithoutPlaying() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        setContent { PlaybackEffects(controls = controls, isActive = false) }
        waitForIdle()

        assertEquals(listOf(PAUSE), controls.calls)
    }

    @Test
    fun losePageFocus_whilePlaying_pausesPlayback() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        var isActive by mutableStateOf(true)
        setContent { PlaybackEffects(controls = controls, isActive = isActive) }
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        isActive = false
        waitForIdle()

        assertEquals(listOf(PLAY, PAUSE), controls.calls)
    }

    @Test
    fun startBlockingTask_whilePlaying_pausesPlayback() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        var isPlaybackPaused by mutableStateOf(false)
        setContent { PlaybackEffects(controls = controls, isPlaybackPaused = isPlaybackPaused) }
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        isPlaybackPaused = true
        waitForIdle()

        assertEquals(listOf(PLAY, PAUSE), controls.calls)
    }

    @Test
    fun composeActivePage_duringBlockingTask_staysPaused() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        setContent { PlaybackEffects(controls = controls, isPlaybackPaused = true) }
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        assertEquals(listOf(PAUSE), controls.calls)
    }

    @Test
    fun replacePlayer_duringBlockingTask_pausesNewPlayer() = runComposeUiTest {
        val firstControls = FakeVideoPlaybackControls()
        val secondControls = FakeVideoPlaybackControls()
        var controls by mutableStateOf(firstControls)
        setContent { PlaybackEffects(controls = controls, isPlaybackPaused = true) }
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        controls = secondControls
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        assertEquals(listOf(PAUSE), secondControls.calls)
    }

    @Test
    fun changeCurrentTime_afterDuration_reportsElapsedFraction() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        val progress = mutableListOf<Float>()
        setContent { PlaybackEffects(controls = controls, onPlaybackProgress = { progress += it }) }
        waitForIdle()

        runOnIdle {
            controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = TOTAL_SECONDS))
            controls.send(MediaPlayerEvent.CurrentTimeChange(currentTime = CURRENT_SECONDS))
        }

        assertEquals(listOf(ELAPSED_FRACTION), progress)
    }

    @Test
    fun changeCurrentTime_beforeDuration_reportsNothing() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        val progress = mutableListOf<Float>()
        setContent { PlaybackEffects(controls = controls, onPlaybackProgress = { progress += it }) }
        waitForIdle()

        runOnIdle { controls.send(MediaPlayerEvent.CurrentTimeChange(currentTime = CURRENT_SECONDS)) }

        assertTrue(progress.isEmpty())
    }

    @Test
    fun changeCurrentTime_pastDuration_reportsCompleteFraction() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        val progress = mutableListOf<Float>()
        setContent { PlaybackEffects(controls = controls, onPlaybackProgress = { progress += it }) }
        waitForIdle()

        runOnIdle {
            controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = TOTAL_SECONDS))
            controls.send(MediaPlayerEvent.CurrentTimeChange(currentTime = PAST_END_SECONDS))
        }

        assertEquals(listOf(1f), progress)
    }

    @Test
    fun endMedia_whilePlaying_reportsFullProgressAndEnd() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        val progress = mutableListOf<Float>()
        var endCount = 0
        setContent {
            PlaybackEffects(
                controls = controls,
                onPlaybackProgress = { progress += it },
                onPlaybackEnded = { endCount++ },
            )
        }
        waitForIdle()

        runOnIdle { controls.send(MediaPlayerEvent.MediaEnd) }

        assertEquals(listOf(1f), progress)
        assertEquals(1, endCount)
    }

    @Test
    fun requestSeek_withKnownDuration_seeksToFractionOfDuration() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        var seekRequest by mutableStateOf<MediaSeekRequest?>(null)
        setContent { PlaybackEffects(controls = controls, seekRequest = seekRequest) }
        waitForIdle()
        runOnIdle { controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = TOTAL_SECONDS)) }
        waitForIdle()

        seekRequest = MediaSeekRequest(fraction = QUARTER, requestId = SEEK_REQUEST_ID)
        waitForIdle()

        assertEquals(listOf(TOTAL_SECONDS * QUARTER), controls.seekedSeconds)
    }

    @Test
    fun requestSeek_beforeDuration_seeksOnceDurationArrives() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        setContent {
            PlaybackEffects(controls = controls, seekRequest = MediaSeekRequest(fraction = HALF, requestId = SEEK_REQUEST_ID))
        }
        waitForIdle()
        assertTrue(controls.seekedSeconds.isEmpty())

        runOnIdle { controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = TOTAL_SECONDS)) }
        waitForIdle()

        assertEquals(listOf(TOTAL_SECONDS * HALF), controls.seekedSeconds)
    }

    @Test
    fun leaveComposition_whilePlaying_detachesListenerAndPauses() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        var isShown by mutableStateOf(true)
        setContent { if (isShown) PlaybackEffects(controls = controls) }
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        isShown = false
        waitForIdle()

        assertNull(controls.eventListener)
        assertEquals(listOf(PLAY, PAUSE), controls.calls)
    }

    @Composable
    private fun PlaybackEffects(
        controls: FakeVideoPlaybackControls,
        isActive: Boolean = true,
        isPlaybackPaused: Boolean = false,
        seekRequest: MediaSeekRequest? = null,
        onPlaybackProgress: (Float) -> Unit = {},
        onPlaybackEnded: () -> Unit = {},
    ) = VideoPlaybackEffects(
        playbackControls = controls,
        isActive = isActive,
        isPlaybackPaused = isPlaybackPaused,
        seekRequest = seekRequest,
        onPlaybackProgress = onPlaybackProgress,
        onPlaybackEnded = onPlaybackEnded,
    )

    private companion object {
        const val PLAYBACK_START_WAIT_MILLIS = 200L
        const val FRAME_MILLIS = 16L
        const val TOTAL_SECONDS = 10f
        const val CURRENT_SECONDS = 4f
        const val ELAPSED_FRACTION = 0.4f
        const val PAST_END_SECONDS = 12f
        const val QUARTER = 0.25f
        const val HALF = 0.5f
        const val SEEK_REQUEST_ID = 1L
    }
}

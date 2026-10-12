package core.presentation.component.mediaviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import chaintech.videoplayer.host.MediaPlayerEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class VideoPlaybackEffectsTest {

    @Test
    fun activePage_startsPlaybackAfterStartDelay() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        mainClock.autoAdvance = false
        setContent { PlaybackEffects(controls = controls) }

        mainClock.advanceTimeBy(milliseconds = 16)
        assertFalse(PLAY in controls.calls)

        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)
        assertEquals(listOf(PLAY), controls.calls)
    }

    @Test
    fun inactivePage_pausesWithoutPlaying() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        setContent { PlaybackEffects(controls = controls, isActive = false) }
        waitForIdle()

        assertEquals(listOf(PAUSE), controls.calls)
    }

    @Test
    fun pageLosingFocus_pausesAfterPlaying() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        var isActive by mutableStateOf(true)
        setContent { PlaybackEffects(controls = controls, isActive = isActive) }
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        isActive = false
        waitForIdle()

        assertEquals(listOf(PLAY, PAUSE), controls.calls)
    }

    @Test
    fun blockingTaskStarting_pausesPlayback() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        var isPlaybackPaused by mutableStateOf(false)
        setContent { PlaybackEffects(controls = controls, isPlaybackPaused = isPlaybackPaused) }
        mainClock.advanceTimeBy(milliseconds = PLAYBACK_START_WAIT_MILLIS)

        isPlaybackPaused = true
        waitForIdle()

        assertEquals(listOf(PLAY, PAUSE), controls.calls)
    }

    @Test
    fun currentTimeChange_afterDuration_reportsElapsedFraction() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        val progress = mutableListOf<Float>()
        setContent { PlaybackEffects(controls = controls, onPlaybackProgress = { progress += it }) }
        waitForIdle()

        runOnIdle {
            controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = 10f))
            controls.send(MediaPlayerEvent.CurrentTimeChange(currentTime = 4f))
        }

        assertEquals(listOf(0.4f), progress)
    }

    @Test
    fun currentTimeChange_beforeDuration_reportsNothing() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        val progress = mutableListOf<Float>()
        setContent { PlaybackEffects(controls = controls, onPlaybackProgress = { progress += it }) }
        waitForIdle()

        runOnIdle { controls.send(MediaPlayerEvent.CurrentTimeChange(currentTime = 4f)) }

        assertTrue(progress.isEmpty())
    }

    @Test
    fun currentTimeChange_pastDuration_reportsCompleteFraction() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        val progress = mutableListOf<Float>()
        setContent { PlaybackEffects(controls = controls, onPlaybackProgress = { progress += it }) }
        waitForIdle()

        runOnIdle {
            controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = 10f))
            controls.send(MediaPlayerEvent.CurrentTimeChange(currentTime = 12f))
        }

        assertEquals(listOf(1f), progress)
    }

    @Test
    fun mediaEnd_reportsFullProgressAndEnd() = runComposeUiTest {
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
    fun seekRequest_withKnownDuration_seeksToFractionOfDuration() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        var seekRequest by mutableStateOf<MediaSeekRequest?>(null)
        setContent { PlaybackEffects(controls = controls, seekRequest = seekRequest) }
        waitForIdle()
        runOnIdle { controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = 20f)) }
        waitForIdle()

        seekRequest = MediaSeekRequest(fraction = 0.25f, requestId = 1L)
        waitForIdle()

        assertEquals(listOf(5f), controls.seekedSeconds)
    }

    @Test
    fun seekRequest_beforeDuration_seeksOnceDurationArrives() = runComposeUiTest {
        val controls = FakeVideoPlaybackControls()
        setContent {
            PlaybackEffects(controls = controls, seekRequest = MediaSeekRequest(fraction = 0.5f, requestId = 1L))
        }
        waitForIdle()
        assertTrue(controls.seekedSeconds.isEmpty())

        runOnIdle { controls.send(MediaPlayerEvent.TotalTimeChange(totalTime = 8f)) }
        waitForIdle()

        assertEquals(listOf(4f), controls.seekedSeconds)
    }

    @Test
    fun leavingComposition_detachesListenerAndPauses() = runComposeUiTest {
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

    private class FakeVideoPlaybackControls : VideoPlaybackControls {
        val calls = mutableListOf<String>()
        val seekedSeconds = mutableListOf<Float>()
        var eventListener: ((MediaPlayerEvent) -> Unit)? = null

        override fun play() {
            calls += PLAY
        }

        override fun pause() {
            calls += PAUSE
        }

        override fun seekTo(seconds: Float) {
            seekedSeconds += seconds
        }

        override fun setOnEvent(onEvent: ((MediaPlayerEvent) -> Unit)?) {
            eventListener = onEvent
        }

        fun send(event: MediaPlayerEvent) {
            eventListener?.invoke(event)
        }
    }

    private companion object {
        const val PLAY = "play"
        const val PAUSE = "pause"
        const val PLAYBACK_START_WAIT_MILLIS = 200L
    }
}

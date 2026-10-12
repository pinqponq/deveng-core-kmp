package core.presentation.component.mediaviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import chaintech.videoplayer.host.MediaPlayerEvent
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import kotlinx.coroutines.delay

private const val PLAYBACK_START_DELAY_MILLIS = 80L

/**
 * Full-screen video page for [MediaViewer] pages.
 *
 * Platform-specific: web (wasmJs) renders a native HTML5 `<video>` element because the
 * bundled Shaka-based player cannot decode progressive MP4 files (it reports zero tracks).
 * Every other platform uses the shared Compose Multiplatform Media Player.
 *
 * Set [arePlayerControlsVisible] to false for surfaces that own the whole gesture surface themselves
 * (e.g. a story viewer that advances on tap): it hides the player's control layer and turns off its
 * zoom and volume gestures, so the player no longer competes for the same touches.
 *
 * Set [isActive] to false for a page that's composed but not the one on screen — a reels-style
 * feed keeps neighboring pages alive off-screen (see [MediaViewerDefaults.VerticalBeyondViewportPageCount]), and
 * without this every one of them would start playing audio at once. Unlike [isPlaybackPaused],
 * this is bidirectional: flipping it back to true resumes playback, since it tracks focus rather
 * than a one-off blocking task.
 *
 * Set [isPlaybackPaused] to true while a blocking background task runs on the same media — saving
 * or sharing it downloads the file, and a player still streaming the same video competes for the
 * connection. Playback is only stopped, never resumed automatically: the viewer decides when to
 * play again rather than overriding a pause the user asked for themselves.
 *
 * [onPlaybackProgress], [seekRequest] and [onPlaybackEnded] let a surface that hides the player's
 * controls put its own in their place: how far the video has got, where the viewer dragged it to,
 * and that it is over — the last of which the player itself never shows, since it rewinds to the
 * first frame and stops, leaving a feed that ignored it looking stalled rather than finished.
 */
@Composable
expect fun MediaViewerVideoPage(
    mediaId: Int,
    videoUrl: String,
    videoContentDescription: String,
    modifier: Modifier = Modifier,
    arePlayerControlsVisible: Boolean = true,
    isActive: Boolean = true,
    isPlaybackPaused: Boolean = false,
    seekRequest: MediaSeekRequest? = null,
    onPlaybackProgress: (progress: Float) -> Unit = {},
    onPlaybackEnded: () -> Unit = {},
)

/**
 * A jump to [fraction] of the video's duration.
 *
 * [requestId] is what makes the request itself distinct: dragging back to a spot the viewer already
 * visited produces the same fraction twice, and without an identity the second drag would look like
 * no change at all and never reach the player.
 */
data class MediaSeekRequest(
    val fraction: Float,
    val requestId: Long,
)

/**
 * Shared Compose Multiplatform Media Player implementation used by every non-web platform.
 */
@Composable
internal fun CmpMediaViewerVideoPage(
    mediaId: Int,
    videoUrl: String,
    videoContentDescription: String,
    modifier: Modifier,
    arePlayerControlsVisible: Boolean,
    isActive: Boolean,
    isPlaybackPaused: Boolean,
    seekRequest: MediaSeekRequest?,
    onPlaybackProgress: (progress: Float) -> Unit,
    onPlaybackEnded: () -> Unit,
) {
    val playerHost = remember(mediaId, videoUrl) {
        MediaPlayerHost(
            mediaUrl = videoUrl,
            autoPlay = false,
            isLooping = false,
            initialVideoFitMode = ScreenResize.FIT,
        )
    }
    // The player reports elapsed and total time as separate events, so the total is held here: the
    // fraction a caller works in needs both, and a seek has to be turned back into seconds.
    var totalSeconds by remember(mediaId, videoUrl) { mutableFloatStateOf(0f) }
    val currentOnPlaybackProgress by rememberUpdatedState(onPlaybackProgress)
    val currentOnPlaybackEnded by rememberUpdatedState(onPlaybackEnded)
    DisposableEffect(playerHost) {
        playerHost.onEvent = { event ->
            when (event) {
                is MediaPlayerEvent.TotalTimeChange -> totalSeconds = event.totalTime
                is MediaPlayerEvent.CurrentTimeChange -> {
                    if (totalSeconds > 0f) {
                        currentOnPlaybackProgress((event.currentTime / totalSeconds).coerceIn(0f, 1f))
                    }
                }

                is MediaPlayerEvent.MediaEnd -> {
                    currentOnPlaybackProgress(1f)
                    currentOnPlaybackEnded()
                }

                else -> Unit
            }
        }
        onDispose {
            playerHost.onEvent = null
            playerHost.pause()
        }
    }
    LaunchedEffect(seekRequest) {
        val requestedFraction = seekRequest?.fraction ?: return@LaunchedEffect
        if (totalSeconds <= 0f) return@LaunchedEffect
        playerHost.seekTo(seconds = requestedFraction * totalSeconds)
    }
    // Keyed on isActive too, so losing focus (e.g. scrolled off in a reels feed) cancels a pending
    // play() from this same effect rather than racing it — restarting the effect on isActive
    // flipping to false takes the pause() branch instead. Callers that never touch isActive (it
    // defaults to true and doesn't change) see the exact same delay-then-play as before.
    LaunchedEffect(mediaId, videoUrl, isActive) {
        if (isActive) {
            delay(timeMillis = PLAYBACK_START_DELAY_MILLIS)
            playerHost.play()
        } else {
            playerHost.pause()
        }
    }
    LaunchedEffect(isPlaybackPaused) {
        if (isPlaybackPaused) {
            playerHost.pause()
        }
    }
    val playerConfig = remember(arePlayerControlsVisible) {
        VideoPlayerConfig(
            showControls = arePlayerControlsVisible,
            isSpeedControlEnabled = false,
            isMuteControlEnabled = false,
            enablePIPControl = false,
            isScreenLockEnabled = false,
            isFullScreenEnabled = false,
            isScreenResizeEnabled = false,
            isFastForwardBackwardEnabled = arePlayerControlsVisible,
            isDurationVisible = arePlayerControlsVisible,
            isZoomEnabled = arePlayerControlsVisible,
            isGestureVolumeControlEnabled = false,
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .semantics { contentDescription = videoContentDescription },
        contentAlignment = Alignment.Center,
    ) {
        VideoPlayerComposable(
            modifier = Modifier.fillMaxSize(),
            playerHost = playerHost,
            playerConfig = playerConfig,
        )
    }
}

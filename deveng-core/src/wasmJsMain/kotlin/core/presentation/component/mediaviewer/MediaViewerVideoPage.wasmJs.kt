@file:OptIn(ExperimentalWasmJsInterop::class)

package core.presentation.component.mediaviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLVideoElement

private const val PLAYBACK_POLL_INTERVAL_MILLIS = 200L

/**
 * Notified when [MediaViewerVideoPage] creates its HTML5 `<video>` element on the web, e.g. to attach
 * an app's own diagnostics listeners. Provide it with [LocalWebVideoElementObserver].
 */
fun interface WebVideoElementObserver {
    fun onVideoElementCreated(mediaId: Int, videoElement: HTMLVideoElement)
}

val LocalWebVideoElementObserver = staticCompositionLocalOf<WebVideoElementObserver?> { null }

/**
 * Web video page backed by a native HTML5 `<video>` element.
 *
 * The bundled Shaka-based player cannot decode progressive MP4 files (it parses an empty
 * codec string and yields zero playable tracks), so the frame never renders. A native
 * `<video>` element plays these files without any MSE/codec negotiation.
 *
 * The element lives outside the Compose canvas, so its viewport rectangle is kept in sync
 * with the composable's bounds. Bounds and element creation arrive in either order, so the
 * latest bounds are cached and re-applied whenever the element appears.
 */
@Composable
actual fun MediaViewerVideoPage(
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
    val density = LocalDensity.current.density
    val videoElementObserver = LocalWebVideoElementObserver.current
    val videoSurface = remember { NativeVideoSurface(videoElementObserver) }

    DisposableEffect(mediaId, videoUrl, arePlayerControlsVisible) {
        videoSurface.attach(
            mediaId = mediaId,
            videoUrl = videoUrl,
            areControlsVisible = arePlayerControlsVisible,
            startPlaying = isActive,
        )
        onDispose {
            videoSurface.detach()
        }
    }

    // Bidirectional, unlike isPlaybackPaused below: a reels feed's off-screen neighbors attach
    // with startPlaying = false, then flip to true once they become the settled page.
    LaunchedEffect(isActive) {
        if (isActive) {
            videoSurface.resumePlayback()
        } else {
            videoSurface.pausePlayback()
        }
    }

    LaunchedEffect(isPlaybackPaused) {
        if (isPlaybackPaused) {
            videoSurface.pausePlayback()
        }
    }

    LaunchedEffect(seekRequest) {
        val requestedFraction = seekRequest?.fraction ?: return@LaunchedEffect
        videoSurface.seekToFraction(fraction = requestedFraction)
    }

    // Progress is polled rather than driven by `timeupdate`/`ended` listeners: the element is built
    // and torn down inside js() blocks (see below), and registering a Kotlin callback on it from
    // there would mean threading a function across the wasm boundary for values the page can just
    // read off the element. Only the page in focus polls, and only while it is on screen.
    LaunchedEffect(mediaId, videoUrl, isActive) {
        if (!isActive) return@LaunchedEffect
        var hasReportedEnd = false
        while (true) {
            delay(timeMillis = PLAYBACK_POLL_INTERVAL_MILLIS)
            val progress = videoSurface.readProgress() ?: continue
            if (videoSurface.hasEnded()) {
                if (!hasReportedEnd) {
                    hasReportedEnd = true
                    onPlaybackProgress(1f)
                    onPlaybackEnded()
                }
            } else {
                hasReportedEnd = false
                onPlaybackProgress(progress)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .semantics { contentDescription = videoContentDescription }
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                videoSurface.updateBounds(
                    left = (position.x / density).toDouble(),
                    top = (position.y / density).toDouble(),
                    width = (coordinates.size.width / density).toDouble(),
                    height = (coordinates.size.height / density).toDouble(),
                )
            },
    )
}

/**
 * Owns the DOM `<video>` element and keeps it aligned with the composable's viewport bounds.
 */
private class NativeVideoSurface(
    private val videoElementObserver: WebVideoElementObserver?,
) {
    private var element: HTMLVideoElement? = null
    private var mediaId: Int = 0
    private var left = 0.0
    private var top = 0.0
    private var width = 0.0
    private var height = 0.0

    fun attach(mediaId: Int, videoUrl: String, areControlsVisible: Boolean, startPlaying: Boolean) {
        this.mediaId = mediaId
        val video = createNativeVideoElement(
            mediaId = mediaId,
            url = videoUrl,
            areControlsVisible = areControlsVisible,
            autoPlay = startPlaying,
        )
        element = video
        videoElementObserver?.onVideoElementCreated(mediaId, video)
        document.body?.appendChild(video)
        applyBounds(video, left, top, width, height)
        if (startPlaying) {
            startPlayback(mediaId, video)
        }
    }

    fun updateBounds(left: Double, top: Double, width: Double, height: Double) {
        this.left = left
        this.top = top
        this.width = width
        this.height = height
        element?.let { applyBounds(it, left, top, width, height) }
    }

    fun pausePlayback() {
        element?.pause()
    }

    fun hasEnded(): Boolean = element?.ended == true

    /** Elapsed fraction of the video, or null while its duration is still unknown. */
    fun readProgress(): Float? {
        val video = element ?: return null
        val duration = video.duration
        if (duration.isNaN() || duration <= 0.0) return null
        return (video.currentTime / duration).toFloat().coerceIn(0f, 1f)
    }

    fun seekToFraction(fraction: Float) {
        val video = element ?: return
        val duration = video.duration
        if (duration.isNaN() || duration <= 0.0) return
        video.currentTime = fraction.coerceIn(0f, 1f).toDouble() * duration
    }

    /** Bidirectional counterpart to [pausePlayback], used when a reels page regains focus. */
    fun resumePlayback() {
        element?.let { startPlayback(mediaId, it) }
    }

    fun detach() {
        element?.let { releaseElement(it) }
        element = null
    }
}

private fun createNativeVideoElement(
    mediaId: Int,
    url: String,
    areControlsVisible: Boolean,
    autoPlay: Boolean,
): HTMLVideoElement =
    (document.createElement("video") as HTMLVideoElement).apply {
        src = url
        controls = areControlsVisible
        autoplay = autoPlay
        setAttribute("playsinline", "")
        setAttribute("preload", "metadata")
        style.position = "fixed"
        style.margin = "0px"
        style.objectFit = "contain"
        style.background = "black"
        style.zIndex = "1"
        // The element sits in front of the Compose canvas (see the class doc), so with native
        // controls hidden it would otherwise still catch every pointer event over its bounds and
        // the canvas underneath — the pager in a reels feed, or a tap-catcher like Moments' — would
        // never see them. Visible controls need real hit-testing to be clickable, so this only
        // applies when they're off.
        style.setProperty("pointer-events", if (areControlsVisible) "auto" else "none")
        setAttribute("data-media-id", mediaId.toString())
    }

private fun applyBounds(
    video: HTMLVideoElement,
    left: Double,
    top: Double,
    width: Double,
    height: Double,
): Unit = js(
    """
{
    video.style.left = left + 'px';
    video.style.top = top + 'px';
    video.style.width = width + 'px';
    video.style.height = height + 'px';
}
"""
)

private fun startPlayback(mediaId: Int, video: HTMLVideoElement): Unit = js(
    """
{
    var attempt = video.play();
    if (attempt && attempt.catch) {
        attempt.catch(function (err) {
            console.log('[NativeVideo] mediaId=' + mediaId + ' play() rejected: ' + err);
        });
    }
}
"""
)

private fun releaseElement(video: HTMLVideoElement): Unit = js(
    """
{
    video.pause();
    video.removeAttribute('src');
    video.load();
    if (video.parentNode) {
        video.parentNode.removeChild(video);
    }
}
"""
)

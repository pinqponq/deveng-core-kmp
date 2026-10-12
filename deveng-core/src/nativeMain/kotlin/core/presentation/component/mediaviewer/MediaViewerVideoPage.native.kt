package core.presentation.component.mediaviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
) = CmpMediaViewerVideoPage(
    mediaId = mediaId,
    videoUrl = videoUrl,
    videoContentDescription = videoContentDescription,
    modifier = modifier,
    arePlayerControlsVisible = arePlayerControlsVisible,
    isActive = isActive,
    isPlaybackPaused = isPlaybackPaused,
    seekRequest = seekRequest,
    onPlaybackProgress = onPlaybackProgress,
    onPlaybackEnded = onPlaybackEnded,
)

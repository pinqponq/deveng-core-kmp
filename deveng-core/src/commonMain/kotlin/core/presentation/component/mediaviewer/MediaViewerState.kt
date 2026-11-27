package core.presentation.component.mediaviewer

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import core.presentation.component.mediaviewer.zoom.ZoomableState

@Stable
class MediaViewerState internal constructor(val pagerState: PagerState) {

    val currentPage: Int get() = pagerState.currentPage

    /** The page a fling/drag has come to rest on, as opposed to [currentPage] which updates mid-gesture. */
    val settledPage: Int get() = pagerState.settledPage

    var isZoomed: Boolean by mutableStateOf(false)
        internal set

    var isDismissing: Boolean by mutableStateOf(false)
        internal set

    internal var currentZoomableState: ZoomableState? by mutableStateOf(null)

    suspend fun animateToPage(page: Int) {
        pagerState.animateScrollToPage(page)
    }

    suspend fun resetZoom() {
        currentZoomableState?.resetZoom()
    }
}

@Composable
fun rememberMediaViewerState(
    initialPage: Int = 0,
    pageCount: () -> Int = { 0 },
): MediaViewerState {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = pageCount)
    return remember(pagerState) { MediaViewerState(pagerState = pagerState) }
}

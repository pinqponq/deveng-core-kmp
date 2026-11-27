package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomIconButton
import core.presentation.component.mediaviewer.MediaViewer
import core.presentation.component.mediaviewer.rememberMediaViewerState
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.presentation.theme.CoreSemiBoldTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

/** One color + label per demo page, standing in for a reels-style feed's items (e.g. video URLs). */
private data class ReelDemoPage(val label: String, val color: Color)

private val REEL_DEMO_PAGES = listOf(
    ReelDemoPage("Page 1", Color(0xFF7C3AED)),
    ReelDemoPage("Page 2", Color(0xFF0EA5E9)),
    ReelDemoPage("Page 3", Color(0xFFEF4444)),
    ReelDemoPage("Page 4", Color(0xFF16A34A)),
    ReelDemoPage("Page 5", Color(0xFFF59E0B)),
)

/**
 * Playground for [MediaViewer]'s vertical (reels-style) mode: swipe up/down to page, swipe right
 * to dismiss. Each page reports whether it's the one settled on screen — the same
 * [core.presentation.component.mediaviewer.MediaViewerState.settledPage] a real video page would
 * use to decide whether to play or pause.
 */
@Composable
internal fun MediaViewerDemoScreen(onBack: () -> Unit) {
    val state = rememberMediaViewerState(pageCount = { REEL_DEMO_PAGES.size })

    Box(modifier = Modifier.fillMaxSize()) {
        MediaViewer(
            items = REEL_DEMO_PAGES,
            orientation = Orientation.Vertical,
            state = state,
            isPageZoomEnabled = { false },
            onDismiss = onBack,
            topBar = { currentPage, totalPages ->
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    color = Color.White,
                    style = CoreSemiBoldTextStyle().copy(fontSize = 14.sp),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            },
            content = { page, item ->
                val reelPage = item as ReelDemoPage
                val isSettled = page == state.settledPage
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(reelPage.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = reelPage.label,
                            color = Color.White,
                            style = CoreSemiBoldTextStyle().copy(fontSize = 28.sp),
                        )
                        Text(
                            // Mirrors what a video page would use to start/stop playback.
                            text = if (isSettled) "settledPage = true (playing)" else "settledPage = false (paused)",
                            color = Color.White.copy(alpha = 0.85f),
                            style = CoreRegularTextStyle().copy(fontSize = 14.sp),
                        )
                    }
                }
            },
        )

        CustomIconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(all = 16.dp),
            icon = Res.drawable.ic_arrow_left,
            iconDescription = "Back",
            onClick = onBack,
        )

        Text(
            text = "Swipe up/down to page · swipe right to dismiss",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = CoreMediumTextStyle().copy(fontSize = 12.sp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomIconButton
import core.presentation.component.scrolltoedgebutton.ScrollTarget
import core.presentation.component.scrolltoedgebutton.ScrollToEdgeButton
import core.presentation.theme.CoreMediumTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

private const val SCROLL_TO_EDGE_DEMO_ITEM_COUNT = 100
private val SCROLL_TO_EDGE_DEMO_THRESHOLD = 300.dp

/**
 * Simple playground for [ScrollToEdgeButton]: a long scrollable list with a scroll-to-top button in
 * the bottom-end corner and a scroll-to-bottom button in the bottom-start corner, so both directions
 * can be tried on one screen.
 */
@Composable
internal fun ScrollToEdgeDemoScreen(onBack: () -> Unit) {
    val listState = rememberLazyListState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            items(count = SCROLL_TO_EDGE_DEMO_ITEM_COUNT) { index ->
                Text(
                    text = "Item #$index",
                    style = CoreMediumTextStyle().copy(fontSize = 18.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
            }
        }

        ScrollToEdgeButton(
            listState = listState,
            target = ScrollTarget.Top,
            threshold = SCROLL_TO_EDGE_DEMO_THRESHOLD,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(all = 16.dp),
        )

        ScrollToEdgeButton(
            listState = listState,
            target = ScrollTarget.Bottom,
            threshold = SCROLL_TO_EDGE_DEMO_THRESHOLD,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(all = 16.dp),
        )

        CustomIconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(all = 16.dp),
            icon = Res.drawable.ic_arrow_left,
            iconDescription = "Back",
            onClick = onBack,
        )
    }
}

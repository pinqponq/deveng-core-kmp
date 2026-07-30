package core.presentation.component.scrolltoedgebutton

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Observes [listState] and emits true while it is still more than [threshold] away from the given
 * [target] edge (i.e. while a jump toward that edge is still meaningful).
 *
 * The distance is estimated from the first visible item's height, so it honors the dp threshold
 * regardless of which item is on top. Assumes a roughly uniform item height (e.g. a photo grid).
 *
 * @param listState the list whose scroll position is observed.
 * @param target the edge the distance is measured against.
 * @param threshold the scroll distance beyond which the returned state becomes true.
 */
@Composable
fun rememberIsScrolledAwayFromEdge(
    listState: LazyListState,
    target: ScrollTarget,
    threshold: Dp,
): State<Boolean> {
    val thresholdPx = with(LocalDensity.current) { threshold.roundToPx() }
    return remember(listState, target, thresholdPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstVisibleItem == null || layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val itemSize = firstVisibleItem.size
                val scrolledFromTopPx =
                    listState.firstVisibleItemIndex * itemSize + listState.firstVisibleItemScrollOffset
                when (target) {
                    ScrollTarget.Top -> scrolledFromTopPx > thresholdPx
                    ScrollTarget.Bottom -> {
                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                        val remainingPx = layoutInfo.totalItemsCount * itemSize - scrolledFromTopPx - viewportHeight
                        remainingPx > thresholdPx
                    }
                }
            }
        }
    }
}
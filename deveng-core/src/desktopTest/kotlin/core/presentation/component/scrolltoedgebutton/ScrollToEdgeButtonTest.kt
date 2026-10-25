package core.presentation.component.scrolltoedgebutton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScrollToEdgeButtonTest {

    @Test
    fun scrolledAway_withUnreadCount_showsBadgeWithCount() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        setContent { ButtonHost(unreadCount = 5, onState = { listState = it }) }
        runOnIdle { runBlocking { listState.scrollToItem(index = FAR_INDEX) } }
        waitForIdle()

        onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun scrolledAway_withUnreadCountOverCap_showsCappedBadge() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        setContent { ButtonHost(unreadCount = 1500, onState = { listState = it }) }
        runOnIdle { runBlocking { listState.scrollToItem(index = FAR_INDEX) } }
        waitForIdle()

        onNodeWithText("999+").assertIsDisplayed()
    }

    @Test
    fun scrolledAway_withZeroUnreadCount_showsButtonWithoutBadge() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        setContent { ButtonHost(unreadCount = 0, onState = { listState = it }) }
        runOnIdle { runBlocking { listState.scrollToItem(index = FAR_INDEX) } }
        waitForIdle()

        onNodeWithContentDescription(BUTTON_DESCRIPTION).assertIsDisplayed()
        onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun scrolledAway_withoutUnreadCountArgument_showsButtonWithoutBadge() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        setContent {
            AppTheme(darkTheme = false) {
                val state = rememberLazyListState()
                listState = state
                Box(modifier = Modifier.fillMaxSize()) {
                    ItemsList(state = state)
                    ScrollToEdgeButton(
                        listState = state,
                        target = ScrollTarget.Top,
                        threshold = THRESHOLD,
                        contentDescription = BUTTON_DESCRIPTION,
                    )
                }
            }
        }
        runOnIdle { runBlocking { listState.scrollToItem(index = FAR_INDEX) } }
        waitForIdle()

        onNodeWithContentDescription(BUTTON_DESCRIPTION).assertIsDisplayed()
        onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun clickButton_withUnreadCount_scrollsListToTarget() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        setContent { ButtonHost(unreadCount = 3, onState = { listState = it }) }
        runOnIdle { runBlocking { listState.scrollToItem(index = FAR_INDEX) } }
        waitForIdle()

        onNodeWithContentDescription(BUTTON_DESCRIPTION).performClick()
        waitUntil(timeoutMillis = 5_000) { listState.firstVisibleItemIndex == 0 }

        assertEquals(0, listState.firstVisibleItemIndex)
    }

    @Test
    fun unreadCountChange_fromZeroToPositive_addsBadge() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        var count by mutableStateOf(0)
        setContent { ButtonHost(unreadCount = count, onState = { listState = it }) }
        runOnIdle { runBlocking { listState.scrollToItem(index = FAR_INDEX) } }
        waitForIdle()
        onNodeWithText("2").assertDoesNotExist()

        count = 2
        waitForIdle()

        onNodeWithText("2").assertIsDisplayed()
    }

    @Composable
    private fun ButtonHost(unreadCount: Int, onState: (LazyListState) -> Unit) {
        AppTheme(darkTheme = false) {
            val state = rememberLazyListState()
            onState(state)
            Box(modifier = Modifier.fillMaxSize()) {
                ItemsList(state = state)
                ScrollToEdgeButton(
                    listState = state,
                    target = ScrollTarget.Top,
                    threshold = THRESHOLD,
                    contentDescription = BUTTON_DESCRIPTION,
                    unreadCount = unreadCount,
                )
            }
        }
    }

    @Composable
    private fun ItemsList(state: LazyListState) {
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            items(items = (0 until ITEM_COUNT).toList()) { Spacer(modifier = Modifier.height(ITEM_HEIGHT)) }
        }
    }

    private companion object {
        const val BUTTON_DESCRIPTION = "Scroll up"
        const val ITEM_COUNT = 100
        const val FAR_INDEX = 60
        val ITEM_HEIGHT = 48.dp
        val THRESHOLD = 100.dp
    }
}

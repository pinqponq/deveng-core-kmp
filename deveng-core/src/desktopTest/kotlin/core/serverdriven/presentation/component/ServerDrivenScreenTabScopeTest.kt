package core.serverdriven.presentation.component

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import core.serverdriven.presentation.engine.DayNightTestLayout
import core.serverdriven.presentation.engine.ServerDrivenEngine
import kotlinx.coroutines.launch
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenTabScopeTest {

    @Test
    fun clickScheduleSwitch_inDayTab_locksOnlyDayTabTimes() = runDesktopComposeUiTest {
        showDayNightLayout()

        onNode(isToggleable()).performClick()
        onNode(hasText(DayNightTestLayout.DAY_START_TIME)).assertIsNotEnabled()

        onNodeWithText(NIGHT_TAB_TITLE).performClick()
        onNode(isToggleable()).assertIsOn()
        onNode(hasSetTextAction() and hasText(DayNightTestLayout.NIGHT_START_TIME)).assertIsEnabled()

        onNodeWithText(DAY_TAB_TITLE).performClick()
        onNode(isToggleable()).assertIsOff()
    }

    private fun ComposeUiTest.showDayNightLayout() {
        val engine = ServerDrivenEngine().apply { setLayout(json = DayNightTestLayout.json()) }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(
                    engine = engine,
                    onComponentEvent = { event -> coroutineScope.launch { engine.handleEvent(event) } }
                )
            }
        }
    }

    private companion object {
        const val DAY_TAB_TITLE = "Day Profile (Config 0)"
        const val NIGHT_TAB_TITLE = "Night Profile (Config 1)"
    }
}

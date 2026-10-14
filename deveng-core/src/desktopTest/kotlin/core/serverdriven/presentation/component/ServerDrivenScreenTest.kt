package core.serverdriven.presentation.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import core.serverdriven.presentation.engine.ServerDrivenEngine
import core.serverdriven.presentation.model.ComponentEvent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenTest {

    @Test
    fun clickShutterHeader_sendsClickShutterEvent() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = false)

        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()

        assertEquals(
            listOf(ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID)),
            sentEvents
        )
    }

    @Test
    fun clickSwitch_sendsNewCheckedState() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = true)

        onNode(isToggleable()).performClick()

        assertEquals(
            listOf(ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = true)),
            sentEvents
        )
    }

    @Test
    fun clickTab_sendsSelectedTabId() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = false)

        onNodeWithText(ServerDrivenScreenTestLayout.NETWORK_TAB_TITLE).performClick()

        assertEquals(
            listOf(
                ComponentEvent.ClickTabItem(
                    componentId = ServerDrivenScreenTestLayout.TABS_ID,
                    tabId = ServerDrivenScreenTestLayout.NETWORK_TAB_ID
                )
            ),
            sentEvents
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.setServerDrivenScreen(
        isShutterExpanded: Boolean
    ): List<ComponentEvent> {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = ServerDrivenScreenTestLayout.JSON)
        if (isShutterExpanded) {
            runBlocking {
                engine.handleEvent(ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID))
            }
        }
        val sentEvents = mutableListOf<ComponentEvent>()

        setContent {
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(
                    engine = engine,
                    onComponentEvent = { event -> sentEvents += event }
                )
            }
        }

        return sentEvents
    }
}

package core.serverdriven.presentation.component

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import core.serverdriven.presentation.engine.ServerDrivenEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenEngineTest {

    @Test
    fun clickShutterHeaderTwice_showsThenHidesShutterContent() = runDesktopComposeUiTest {
        setServerDrivenScreenWithEngine()
        onNodeWithText(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE).assertDoesNotExist()

        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE).assertExists()

        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE).assertDoesNotExist()
    }

    @Test
    fun clickNetworkTab_showsItsContentAndAppliesDependencies() = runDesktopComposeUiTest {
        setServerDrivenScreenWithEngine()
        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.SLIDER_TITLE).assertExists()

        onNodeWithText(ServerDrivenScreenTestLayout.NETWORK_TAB_TITLE).performClick()

        onNodeWithText(ServerDrivenScreenTestLayout.SLIDER_TITLE).assertDoesNotExist()
        onNodeWithText(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE).assertDoesNotExist()
    }

    @Test
    fun clickSwitch_unlocksDropdownAndSelectedOptionIsShown() = runDesktopComposeUiTest {
        val engine = setServerDrivenScreenWithEngine()
        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()

        onNode(isToggleable()).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_VALUE).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION).performClick()

        assertEquals(
            ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION,
            engine.componentValues.value[ServerDrivenScreenTestLayout.DROPDOWN_ID]
        )
        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_VALUE).assertDoesNotExist()
        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION).assertExists()
    }

    @Test
    fun enterTextField_showsEnteredText() = runDesktopComposeUiTest {
        setServerDrivenScreenWithEngine()
        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()

        onNode(hasSetTextAction() and hasText(ServerDrivenScreenTestLayout.TEXT_FIELD_VALUE))
            .performTextReplacement(ENTERED_TEXT)

        onNode(hasSetTextAction() and hasText(ENTERED_TEXT)).assertExists()
    }

    @Test
    fun changeSlider_showsNewValue() = runDesktopComposeUiTest {
        val engine = setServerDrivenScreenWithEngine()

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 85f, range = 0f..100f)))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(SLIDER_TARGET_VALUE) }

        assertEquals(SLIDER_TARGET_VALUE, engine.componentValues.value[ServerDrivenScreenTestLayout.SLIDER_ID])
        onNodeWithText(SLIDER_TARGET_VALUE.toInt().toString()).assertExists()
    }

    private fun ComposeUiTest.setServerDrivenScreenWithEngine(): ServerDrivenEngine {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = ServerDrivenScreenTestLayout.JSON)

        setContent {
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(
                    engine = engine,
                    onComponentEvent = { event -> runBlocking { engine.handleEvent(event) } }
                )
            }
        }

        return engine
    }

    private companion object {
        const val ENTERED_TEXT = "Parking"
        const val SLIDER_TARGET_VALUE = 40f
    }
}

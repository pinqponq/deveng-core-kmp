package core.serverdriven.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Dp
import core.presentation.theme.AppTheme
import core.serverdriven.presentation.engine.ServerDrivenEngine
import core.serverdriven.presentation.model.ComponentEvent
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_dest_icon_star
import global.deveng.deveng_core.generated.resources.shared_ok
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenRobot(private val composeUiTest: ComposeUiTest) {
    val engine = ServerDrivenEngine()
    val sentEvents = mutableListOf<ComponentEvent>()
    val replacedHandlerEvents = mutableListOf<ComponentEvent>()
    private var onComponentEvent by mutableStateOf<(ComponentEvent) -> Unit>({ event -> sentEvents += event })
    private var screenTag by mutableStateOf(SCREEN_TAG)
    private var ratingStarDescription = ""
    private var datePickerConfirmText = ""

    init {
        engine.setLayout(json = ServerDrivenScreenTestLayout.JSON)
    }

    fun setLayout(json: String) = apply {
        engine.setLayout(json = json)
    }

    suspend fun prepareEngine(events: List<ComponentEvent>) = apply {
        events.forEach { event -> engine.handleEvent(event) }
    }

    fun setContent(isForwardingToEngine: Boolean) = apply {
        composeUiTest.setContent {
            val coroutineScope = rememberCoroutineScope()
            val currentEventHandler = onComponentEvent
            ratingStarDescription = stringResource(Res.string.shared_content_dest_icon_star)
            datePickerConfirmText = stringResource(Res.string.shared_ok)
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(
                    engine = engine,
                    modifier = Modifier.testTag(screenTag),
                    onComponentEvent = { event ->
                        if (isForwardingToEngine) {
                            coroutineScope.launch { engine.handleEvent(event) }
                        } else {
                            currentEventHandler(event)
                        }
                    }
                )
            }
        }
    }

    fun replaceEventHandler() = apply {
        onComponentEvent = { event -> replacedHandlerEvents += event }
    }

    fun replaceScreenTag(tag: String) = apply {
        screenTag = tag
    }

    fun clickShutterHeader() = apply {
        composeUiTest.onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()
    }

    fun clickSwitch() = apply {
        composeUiTest.onNode(isToggleable()).performClick()
    }

    fun clickNetworkTab() = apply {
        composeUiTest.onNodeWithText(ServerDrivenScreenTestLayout.NETWORK_TAB_TITLE).performClick()
    }

    fun openDropdown() = apply {
        composeUiTest.onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_VALUE).performClick()
    }

    fun clickDropdownOption(option: String) = apply {
        composeUiTest.onNodeWithText(option).performClick()
    }

    fun enterText(text: String) = apply {
        composeUiTest.onNode(hasSetTextAction() and hasText(ServerDrivenScreenTestLayout.TEXT_FIELD_VALUE))
            .performTextReplacement(text)
    }

    fun moveSlider(value: Float) = apply {
        composeUiTest.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 85f, range = 0f..100f)))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(value) }
    }

    fun clickText(text: String) = apply {
        composeUiTest.onNodeWithText(text).performClick()
    }

    fun clickRatingStar(starIndex: Int) = apply {
        composeUiTest.onAllNodesWithContentDescription(ratingStarDescription)[starIndex].performClick()
    }

    fun confirmDatePicker() = apply {
        composeUiTest.onNodeWithText(datePickerConfirmText).performClick()
    }

    fun assertDatePickerNotShown() = apply {
        composeUiTest.onNodeWithText(datePickerConfirmText).assertDoesNotExist()
    }

    fun assertTextShown(text: String) = apply {
        composeUiTest.onNodeWithText(text).assertExists()
    }

    fun assertTextNotShown(text: String) = apply {
        composeUiTest.onNodeWithText(text).assertDoesNotExist()
    }

    fun assertTextFieldShows(text: String) = apply {
        composeUiTest.onNode(hasSetTextAction() and hasText(text)).assertExists()
    }

    fun assertScreenTag(tag: String) = apply {
        composeUiTest.onNodeWithTag(tag).assertExists()
    }

    fun assertNoScreenTag(tag: String) = apply {
        composeUiTest.onNodeWithTag(tag).assertDoesNotExist()
    }

    fun assertScreenHeight(height: Dp) = apply {
        composeUiTest.onNodeWithTag(screenTag).assertHeightIsEqualTo(height)
    }

    fun assertSentEvents(events: List<ComponentEvent>) = apply {
        assertEquals(events, sentEvents)
    }

    companion object {
        const val SCREEN_TAG = "server_driven_screen"
    }
}

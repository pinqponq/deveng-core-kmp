package core.presentation.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LabeledSwitchTest {

    @Test
    fun clickSwitch_withDefaultEnabledState_sendsNewCheckedState() = runDesktopComposeUiTest {
        val clickedStates = mutableListOf<Boolean>()
        setContent {
            AppTheme(darkTheme = false) {
                LabeledSwitch(label = LABEL, isChecked = false, onSwitchClick = { isChecked -> clickedStates += isChecked })
            }
        }

        onNode(isToggleable()).performClick()

        assertEquals(listOf(true), clickedStates)
    }

    @Test
    fun clickSwitch_whenDisabled_sendsNothing() = runDesktopComposeUiTest {
        val clickedStates = mutableListOf<Boolean>()
        setContent {
            AppTheme(darkTheme = false) {
                LabeledSwitch(
                    label = LABEL,
                    isChecked = false,
                    isEnabled = false,
                    onSwitchClick = { isChecked -> clickedStates += isChecked }
                )
            }
        }

        onNode(isToggleable()).assertIsNotEnabled().performClick()

        assertTrue(clickedStates.isEmpty())
    }

    private companion object {
        const val LABEL = "Recording"
    }
}

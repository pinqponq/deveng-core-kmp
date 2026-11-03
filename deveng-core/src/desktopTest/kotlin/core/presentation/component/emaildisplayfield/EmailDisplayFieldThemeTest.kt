package core.presentation.component.emaildisplayfield

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EmailDisplayFieldThemeTest {

    @Test
    fun showEditText_withLargerEndPadding_movesEditTextTowardStart() = runComposeUiTest {
        var componentTheme by mutableStateOf(DefaultComponentTheme)
        setContent {
            AppTheme(darkTheme = false, componentTheme = componentTheme) {
                EmailDisplayField(
                    email = EMAIL,
                    editText = EDIT_TEXT,
                    isEditTextVisible = true
                )
            }
        }
        waitForIdle()
        val defaultEditTextRight = onNodeWithText(EDIT_TEXT).getUnclippedBoundsInRoot().right

        componentTheme = CUSTOM_THEME
        waitForIdle()
        val customEditTextRight = onNodeWithText(EDIT_TEXT).getUnclippedBoundsInRoot().right

        val paddingChange = CUSTOM_THEME.emailDisplayField.editTextEndPadding -
            DefaultComponentTheme.emailDisplayField.editTextEndPadding
        val shift = defaultEditTextRight - customEditTextRight
        assertTrue(shift.value >= MINIMUM_SHIFT_DP, "expected the edit text to move toward the start but it moved $shift")
        assertTrue(shift.value <= paddingChange.value + DP_TOLERANCE, "expected at most $paddingChange but was $shift")
    }

    private companion object {
        const val DP_TOLERANCE = 0.5f
        const val MINIMUM_SHIFT_DP = 8f
        const val EMAIL = "person@example.com"
        const val EDIT_TEXT = "Edit"
        val CUSTOM_THEME = DefaultComponentTheme.copy(
            emailDisplayField = DefaultComponentTheme.emailDisplayField.copy(editTextEndPadding = 31.dp)
        )
    }
}

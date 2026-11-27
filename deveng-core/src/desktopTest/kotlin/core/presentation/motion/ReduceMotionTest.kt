package core.presentation.motion

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ReduceMotionTest {

    @Test
    fun appTheme_withReduceMotion_providesItToContent() = runDesktopComposeUiTest {
        var providedReduceMotion: Boolean? = null
        setContent {
            AppTheme(darkTheme = false, reduceMotion = true) {
                providedReduceMotion = LocalReduceMotion.current
            }
        }

        assertEquals(true, providedReduceMotion)
    }

    @Test
    fun appTheme_onDesktopByDefault_allowsMotion() = runDesktopComposeUiTest {
        var providedReduceMotion: Boolean? = null
        setContent {
            AppTheme(darkTheme = false) {
                providedReduceMotion = LocalReduceMotion.current
            }
        }

        assertEquals(false, providedReduceMotion)
    }
}

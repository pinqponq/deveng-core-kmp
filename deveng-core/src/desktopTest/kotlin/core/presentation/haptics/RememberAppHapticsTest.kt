package core.presentation.haptics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.haptics.fake.FakeHapticFeedback
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RememberAppHapticsTest {

    @Test
    fun rememberAppHaptics_tap_usesCurrentHapticFeedback() = runDesktopComposeUiTest {
        val hapticFeedback = FakeHapticFeedback()
        var appHaptics: AppHaptics? = null
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                appHaptics = rememberAppHaptics()
            }
        }

        checkNotNull(appHaptics).tap()

        assertEquals(listOf(HapticFeedbackType.VirtualKey), hapticFeedback.performedTypes)
    }
}

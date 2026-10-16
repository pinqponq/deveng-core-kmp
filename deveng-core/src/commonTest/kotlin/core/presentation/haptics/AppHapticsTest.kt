package core.presentation.haptics

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import core.presentation.haptics.fake.FakeHapticFeedback
import kotlin.test.Test
import kotlin.test.assertEquals

class AppHapticsTest {
    private val hapticFeedback = FakeHapticFeedback()
    private val appHaptics = AppHaptics(hapticFeedback = hapticFeedback)

    @Test
    fun tap_performsVirtualKeyFeedback() {
        appHaptics.tap()

        assertEquals(listOf(HapticFeedbackType.VirtualKey), hapticFeedback.performedTypes)
    }

    @Test
    fun success_performsConfirmFeedback() {
        appHaptics.success()

        assertEquals(listOf(HapticFeedbackType.Confirm), hapticFeedback.performedTypes)
    }

    @Test
    fun warning_performsRejectFeedback() {
        appHaptics.warning()

        assertEquals(listOf(HapticFeedbackType.Reject), hapticFeedback.performedTypes)
    }
}

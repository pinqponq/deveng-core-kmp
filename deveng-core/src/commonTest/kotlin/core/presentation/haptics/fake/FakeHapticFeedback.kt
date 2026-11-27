package core.presentation.haptics.fake

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class FakeHapticFeedback : HapticFeedback {
    val performedTypes = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        performedTypes += hapticFeedbackType
    }
}

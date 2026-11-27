package core.presentation.component.textfield

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

private const val LINE_BREAK = "\n"

// Compose maps only a plain Enter to a new line on desktop and web, so Shift+Enter does nothing there by default.
internal fun KeyEvent.isShiftEnterPress(): Boolean {
    val isEnterKey = key == Key.Enter || key == Key.NumPadEnter
    val isOtherModifierPressed = isCtrlPressed || isAltPressed || isMetaPressed
    return type == KeyEventType.KeyDown && isEnterKey && isShiftPressed && !isOtherModifierPressed
}

internal fun TextFieldValue.withLineBreakAtSelection(): TextFieldValue {
    val textWithLineBreak = text.replaceRange(selection.min, selection.max, LINE_BREAK)
    return TextFieldValue(text = textWithLineBreak, selection = TextRange(selection.min + LINE_BREAK.length))
}

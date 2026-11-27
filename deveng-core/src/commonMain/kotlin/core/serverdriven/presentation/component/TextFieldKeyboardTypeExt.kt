package core.serverdriven.presentation.component

import androidx.compose.ui.text.input.KeyboardType
import core.serverdriven.presentation.model.TextFieldKeyboardType

internal fun TextFieldKeyboardType.toComposeKeyboardType(): KeyboardType {
    return when (this) {
        TextFieldKeyboardType.TEXT -> KeyboardType.Text
        TextFieldKeyboardType.NUMERIC -> KeyboardType.Number
        TextFieldKeyboardType.PASSWORD -> KeyboardType.Password
        TextFieldKeyboardType.EMAIL -> KeyboardType.Email
    }
}

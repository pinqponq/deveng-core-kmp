package core.presentation.component.otpview

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import core.presentation.component.textfield.CustomTextField
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.util.EMPTY
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * @param allowLetters When false (default), only digits are accepted and a numeric keyboard is shown.
 *   When true, only letters and digits ([Char.isLetterOrDigit]) are accepted, a text keyboard is shown,
 *   and letters are normalized to uppercase ([Char.uppercaseChar]) before [onDigitChanged].
 *   Invalid characters are rejected the same way in both modes (callback is not invoked, field stays in sync with [otpDigits]).
 */
@Composable
fun OtpView(
    modifier: Modifier = Modifier,
    otpDigits: String,
    isError: Boolean = false,
    onDigitChanged: (String) -> Unit,
    otpSize: OtpSize = OtpSize.SIX,
    textStyle: TextStyle? = null,
    requestFocusOnFirstDisplay: Boolean = true,
    allowLetters: Boolean = false
) {
    val componentTheme = LocalComponentTheme.current
    val otpViewTheme = componentTheme.otpView

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (requestFocusOnFirstDisplay) {
            focusRequester.requestFocus()
        }
    }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(otpDigits) {
        if (otpDigits.length == otpSize.size) {
            focusManager.clearFocus()
        }
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(otpViewTheme.digitSpacing)
        ) {
            repeat(otpSize.size) { index ->
                val char = otpDigits.getOrNull(index)?.toString() ?: String.EMPTY
                OtpDigit(
                    modifier = Modifier.weight(1f),
                    value = char,
                    isError = isError,
                    isFocused = index == otpDigits.length,
                    onDigitClick = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                )
            }
        }

        CustomTextField(
            textFieldModifier = modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            value = otpDigits,
            onValueChange = { text ->
                val isAllowedCharacter: (Char) -> Boolean =
                    if (allowLetters) Char::isLetterOrDigit else Char::isDigit
                if (text.length <= otpSize.size && text.all(isAllowedCharacter)) {
                    onDigitChanged(
                        if (allowLetters) {
                            text
                                .map { character ->
                                    if (character.isLetter()) character.uppercaseChar() else character
                                }
                                .joinToString(separator = "")
                        } else {
                            text
                        }
                    )
                }
            },
            containerModifier = Modifier
                .size(1.dp)
                .alpha(0f),
            keyboardType = if (allowLetters) KeyboardType.Text else KeyboardType.Number,
            maxLength = otpSize.size,
            textStyle = textStyle,
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            isBorderActive = false
        )
    }
}

@Preview
@Composable
fun OtpViewPreview() {
    AppTheme {
        BoxWithConstraints(
            modifier = Modifier.offset(y = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            OtpView(
                modifier = Modifier,
                otpDigits = String.EMPTY,
                onDigitChanged = {}
            )
        }
    }
}

package core.presentation.component.emaildisplayfield

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import core.presentation.component.textfield.CustomTextField
import core.presentation.theme.LocalComponentTheme
import core.util.EMPTY
import core.util.debouncedCombinedClickable
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_edit
import global.deveng.deveng_core.generated.resources.shared_email
import global.deveng.deveng_core.generated.resources.shared_please_enter_a_valid_email_address
import org.jetbrains.compose.resources.stringResource

private const val EMAIL_MAX_LENGTH = 254

/**
 * Email text field with an optional title, an optional "edit" text action and a validation error message.
 *
 * @param modifier Modifier to be applied to the field container.
 * @param email Current email value.
 * @param hint Placeholder text displayed when the field is empty.
 * @param title Title displayed above the field when [isTitleVisible] is true.
 * @param editText Text of the edit action displayed when [isEditTextVisible] is true.
 * @param errorMessage Error message displayed when [isErrorMessageVisible] is true.
 * @param titleColor Color of the title. If null, uses the text field theme default.
 * @param containerColor Background color of the field. If null, uses the text field theme default.
 * @param disabledContainerColor Background color of the disabled field. If null, uses the text field theme default.
 * @param focusedBorderColor Border color when focused. If null, uses the text field theme default.
 * @param unfocusedBorderColor Border color when not focused. If null, uses the text field theme default.
 * @param isChangeable Whether the email can be typed into; otherwise the field is read-only.
 * @param isEditTextVisible Whether the edit action is displayed at the end of the field.
 * @param isErrorMessageVisible Whether [errorMessage] is displayed under the field.
 * @param isTitleVisible Whether [title] is displayed above the field.
 * @param onEmailChange Callback invoked with the new email when the user types.
 * @param onEditTextClick Callback invoked when the edit action is clicked.
 */
@Composable
fun EmailDisplayField(
    modifier: Modifier = Modifier,
    email: String,
    hint: String = String.EMPTY,
    title: String = stringResource(Res.string.shared_email),
    editText: String = stringResource(Res.string.shared_edit),
    errorMessage: String = stringResource(Res.string.shared_please_enter_a_valid_email_address),
    titleColor: Color? = null,
    containerColor: Color? = null,
    disabledContainerColor: Color? = null,
    focusedBorderColor: Color? = null,
    unfocusedBorderColor: Color? = null,
    isChangeable: Boolean = false,
    isEditTextVisible: Boolean = false,
    isErrorMessageVisible: Boolean = false,
    isTitleVisible: Boolean = true,
    onEmailChange: (String) -> Unit = {},
    onEditTextClick: () -> Unit = {}
) {
    val emailDisplayFieldTheme = LocalComponentTheme.current.emailDisplayField

    CustomTextField(
        containerModifier = modifier,
        value = email,
        textColor = emailDisplayFieldTheme.textColor,
        title = if (isTitleVisible) title else null,
        titleColor = titleColor,
        hint = hint,
        containerColor = containerColor,
        disabledContainerColor = disabledContainerColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        readOnly = !isChangeable,
        onValueChange = onEmailChange,
        trailingSlot = {
            if (isEditTextVisible) {
                Text(
                    text = editText,
                    modifier = Modifier
                        .padding(end = 15.dp)
                        .debouncedCombinedClickable { onEditTextClick() },
                    style = emailDisplayFieldTheme.editTextStyle
                )
            }
        },
        maxLength = EMAIL_MAX_LENGTH,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        errorMessage = if (isErrorMessageVisible) errorMessage else null
    )
}

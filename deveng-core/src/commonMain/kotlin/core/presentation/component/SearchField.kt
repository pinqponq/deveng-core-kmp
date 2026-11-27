package core.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.textfield.CustomTextField
import core.presentation.theme.CoreCustomDividerColor
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_find
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource

/**
 * A search field component combining a text field with a search action button.
 * The button can be positioned at the start or end of the text field.
 *
 * @param modifier Modifier to be applied to the row container.
 * @param searchText Current search text value.
 * @param onSearchTextChange Callback invoked when the search text changes.
 * @param onSearchButtonClick Callback invoked when the search button is clicked.
 * @param onTypingStop Callback invoked when the user stops typing.
 * @param searchBarHint Placeholder text for the search field.
 * @param textFieldContainerModifier Modifier to be applied to the text field container.
 * @param textFieldModifier Modifier to be applied to the text field itself.
 * @param textFieldShape Shape of the text field. Default is RoundedCornerShape(16.dp).
 * @param textFieldBorder Border stroke of the text field. Default is 1.dp divider color.
 * @param maxLines Maximum number of lines for the text field. Default is Int.MAX_VALUE.
 * @param singleLine Whether the text field is single line. Default is true.
 * @param isEditable Whether the text field is editable. Default is true.
 * @param readOnly Whether the text field is read-only. Default is false.
 * @param maxLength Maximum character length allowed. Default is 254.
 * @param keyboardType Keyboard type for text input. Default is KeyboardType.Text.
 * @param errorMessage Optional error message displayed below the text field.
 * @param title Optional title text displayed above the text field.
 * @param leadingSlot Optional composable slot for leading content in the text field.
 * @param trailingSlot Optional composable slot for trailing content in the text field.
 * @param isTextCharCountVisible Whether to display character count. Default is false.
 * @param onDone Callback invoked when the done action is triggered.
 * @param onFocusCleared Callback invoked when focus is cleared from the text field.
 * @param enabled Whether the text field is enabled. Default is true.
 * @param requestFocus Whether to request focus when the component is composed. Default is false.
 * @param textStyle Text style for the text field. If null, uses theme default.
 * @param hintTextStyle Text style for the hint text. If null, uses theme default.
 * @param containerColor Background color of the text field when enabled. If null, uses theme default.
 * @param disabledContainerColor Background color when disabled. If null, uses theme default.
 * @param textColor Text color when enabled. If null, uses theme default.
 * @param disabledTextColor Text color when disabled. If null, uses theme default.
 * @param isButtonEnabled Whether the search button is enabled. Default is true.
 * @param buttonSize Size of the search button. If null, uses theme default.
 * @param buttonShape Shape of the search button. If null, uses theme default.
 * @param buttonBackgroundColor Background color of the search button. If null, uses theme default.
 * @param buttonIcon Drawable resource for the search button icon.
 * @param buttonIconDescription Content description for the search button icon.
 * @param buttonIconTint Color tint for the search button icon. If null, uses theme default.
 * @param buttonShadowElevation Shadow elevation of the search button. If null, uses theme default.
 * @param isButtonAtEnd Whether the button is positioned at the end (true) or start (false). Default is true.
 * @param isButtonVisible Whether the button is visible. Default is true.
 * @param debounceTime Time in milliseconds to debounce text changes. If no text is entered before this time expires, searchText will be triggered. Default is 500ms.
 */
@Composable
fun SearchField(
    modifier: Modifier = Modifier,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearchButtonClick: () -> Unit = {},
    onTypingStop: () -> Unit = {},
    searchBarHint: String,
    textFieldContainerModifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    textFieldShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    textFieldBorder: BorderStroke = BorderStroke(1.dp, CoreCustomDividerColor),
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    isEditable: Boolean = true,
    readOnly: Boolean = false,
    maxLength: Int = 254,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null,
    title: String? = null,
    leadingSlot: Slot? = null,
    trailingSlot: Slot? = null,
    isTextCharCountVisible: Boolean = false,
    onDone: (() -> Unit)? = null,
    onFocusCleared: (() -> Unit)? = null,
    enabled: Boolean = true,
    requestFocus: Boolean = false,
    textStyle: TextStyle? = null,
    hintTextStyle: TextStyle? = null,
    containerColor: Color? = null,
    disabledContainerColor: Color? = null,
    textColor: Color? = null,
    disabledTextColor: Color? = null,
    isButtonEnabled: Boolean = true,
    buttonSize: Dp? = null,
    buttonShape: Shape? = null,
    buttonBackgroundColor: Color? = null,
    buttonIcon: DrawableResource = Res.drawable.shared_ic_find,
    buttonIconDescription: String? = null,
    buttonIconTint: Color? = null,
    buttonShadowElevation: Dp? = null,
    isButtonAtEnd: Boolean = true,
    isButtonVisible: Boolean = true,
    debounceTime: Long = 500
) {
    val componentTheme = LocalComponentTheme.current
    val searchFieldTheme = componentTheme.searchField
    val customTextFieldTheme = componentTheme.customTextField

    val finalButtonSize = buttonSize ?: searchFieldTheme.buttonSize
    val finalButtonShape = buttonShape ?: searchFieldTheme.buttonShape
    val finalButtonBackgroundColor = buttonBackgroundColor ?: searchFieldTheme.buttonBackgroundColor
    val finalButtonIconTint = buttonIconTint ?: searchFieldTheme.buttonIconTint
    val finalButtonShadowElevation = buttonShadowElevation ?: searchFieldTheme.buttonShadowElevation
    val finalTextFieldShape = textFieldShape
    val finalTextFieldBorder = textFieldBorder
    val finalTextStyle = textStyle ?: customTextFieldTheme.textStyle
    val finalHintTextStyle = hintTextStyle ?: customTextFieldTheme.hintTextStyle
    val finalContainerColor = containerColor ?: customTextFieldTheme.containerColor
    val finalDisabledContainerColor =
        disabledContainerColor ?: customTextFieldTheme.disabledContainerColor
    val finalTextColor = textColor ?: customTextFieldTheme.textColor
    val finalDisabledTextColor = disabledTextColor ?: customTextFieldTheme.disabledTextColor

    LaunchedEffect(searchText) {
        if (searchText.isNotBlank()) {
            delay(debounceTime)
            onTypingStop()
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val actionButton = @Composable {
            CustomIconButton(
                modifier = Modifier.size(finalButtonSize),
                isEnabled = isButtonEnabled,
                shape = finalButtonShape,
                backgroundColor = finalButtonBackgroundColor,
                icon = buttonIcon,
                iconDescription = buttonIconDescription
                    ?: searchFieldTheme.defaultButtonIconDescription,
                iconTint = finalButtonIconTint,
                shadowElevation = finalButtonShadowElevation,
                onClick = onSearchButtonClick
            )
        }

        if (!isButtonAtEnd && isButtonVisible) {
            actionButton()
        }

        CustomTextField(
            containerModifier = textFieldContainerModifier.weight(1f),
            textFieldModifier = textFieldModifier,
            borderStroke = finalTextFieldBorder,
            value = searchText,
            hint = searchBarHint,
            shape = finalTextFieldShape,
            maxLines = maxLines,
            singleLine = singleLine,
            isEditable = isEditable,
            readOnly = readOnly,
            maxLength = maxLength,
            keyboardType = keyboardType,
            errorMessage = errorMessage,
            title = title,
            leadingSlot = leadingSlot,
            trailingSlot = trailingSlot,
            isTextCharCountVisible = isTextCharCountVisible,
            onDone = onDone,
            onFocusCleared = onFocusCleared,
            enabled = enabled,
            requestFocus = requestFocus,
            textStyle = finalTextStyle,
            hintTextStyle = finalHintTextStyle,
            containerColor = finalContainerColor,
            disabledContainerColor = finalDisabledContainerColor,
            textColor = finalTextColor,
            disabledTextColor = finalDisabledTextColor,
            onValueChange = onSearchTextChange
        )

        if (isButtonAtEnd && isButtonVisible) {
            actionButton()
        }
    }
}


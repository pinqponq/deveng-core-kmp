package core.presentation.component.textfield

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.RoundedSurface
import core.presentation.component.Slot
import core.presentation.theme.AppTheme
import core.presentation.theme.CoreCustomGrayHintColor
import core.presentation.theme.LocalComponentTheme
import core.util.EMPTY
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.char_count
import global.deveng.deveng_core.generated.resources.shared_cont_desc_icon_password_invisible
import global.deveng.deveng_core.generated.resources.shared_cont_desc_icon_password_visible
import global.deveng.deveng_core.generated.resources.shared_ic_password_invisible
import global.deveng.deveng_core.generated.resources.shared_ic_password_visible
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A comprehensive customizable text field component with extensive styling and functionality options.
 * Supports password visibility toggle, inline suffix, character counting, and various states.
 *
 * @param textFieldModifier Modifier to be applied to the text field itself.
 * @param value Current text value of the field.
 * @param hint Placeholder text displayed when the field is empty.
 * @param containerModifier Modifier to be applied to the container (includes title and error message).
 * @param leadingSlot Optional composable slot for leading content (e.g., icon).
 * @param trailingSlot Optional composable slot for trailing content (e.g., action icon).
 * @param suffixSlot Optional composable slot for suffix content.
 * @param titleTrailingSlot Optional composable slot displayed after the title.
 * @param textStyle Text style for the input text. If null, uses theme default.
 * @param isBorderActive Whether the border is visible. If null, uses theme default.
 * @param shape Shape of the text field container. If null, uses theme default.
 * @param borderStroke Border stroke configuration. If null, uses theme default.
 * @param focusedBorderWidth Width of the border when focused. If null, uses borderStroke width.
 * @param unfocusedBorderWidth Width of the border when not focused. If null, uses borderStroke width.
 * @param maxLines Maximum number of lines for multi-line input. Default is Int.MAX_VALUE.
 * @param singleLine Whether the field is single line. Default is true.
 * @param isEditable Whether the field is editable. Default is true.
 * @param readOnly Whether the field is read-only. Default is false.
 * @param maxLength Maximum character length allowed. Default is 254.
 * @param keyboardType Keyboard type for text input. Default is KeyboardType.Text.
 * @param keyboardOptions Keyboard options including IME action. Default is Done action.
 * @param isPasswordToggleDisplayed Whether to show password visibility toggle. Default is true if keyboardType is Password.
 * @param isPasswordVisible Current password visibility state. Default is false.
 * @param onPasswordToggleClick Callback invoked when password toggle is clicked, receives new visibility state.
 * @param inlineSuffix Optional inline suffix text displayed at the end of the input text.
 * @param errorMessage Optional error message displayed below the field.
 * @param title Optional title text displayed above the field.
 * @param titleColor Color of the title text. If null, uses theme default.
 * @param titleTextStyle Text style for the title. If null, uses theme default.
 * @param charCountTextStyle Text style for the character count. If null, uses theme default.
 * @param isTextCharCountVisible Whether to display character count. Default is false.
 * @param onDone Callback invoked when the done/IME action is triggered.
 * @param onFocusCleared Callback invoked when focus is cleared from the field.
 * @param enabled Whether the field is enabled. Default is true.
 * @param requestFocus Whether to request focus when the component is composed. Default is false.
 * @param containerColor Background color of the field when enabled. If null, uses theme default.
 * @param disabledContainerColor Background color when disabled. If null, uses theme default.
 * @param textColor Text color when enabled. If null, uses theme default.
 * @param disabledTextColor Text color when disabled. If null, uses theme default.
 * @param readOnlyTextColor Text color when read-only. If null, uses theme default.
 * @param hintTextStyle Text style for the hint/placeholder. If null, uses theme default.
 * @param errorTextStyle Text style for the error message. If null, uses theme default.
 * @param focusedBorderColor Border color when focused. If null, uses borderStroke color.
 * @param unfocusedBorderColor Border color when not focused. If null, uses borderStroke color.
 * @param cursorColor Color of the text cursor. If null, uses theme default.
 * @param visualTransformation Visual transformation applied to the text (e.g., password masking). Default is None.
 * @param onValueChange Callback invoked when the text value changes.
 */
@Composable
fun CustomTextField(
    textFieldModifier: Modifier = Modifier,
    value: String,
    hint: String = String.EMPTY,
    containerModifier: Modifier = Modifier,
    leadingSlot: Slot? = null,
    trailingSlot: Slot? = null,
    suffixSlot: Slot? = null,
    titleTrailingSlot: Slot? = null,
    textStyle: TextStyle? = null,
    isBorderActive: Boolean? = null,
    shape: CornerBasedShape? = null,
    borderStroke: BorderStroke? = null,
    focusedBorderWidth: Dp? = null,
    unfocusedBorderWidth: Dp? = null,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    isEditable: Boolean = true,
    readOnly: Boolean = false,
    maxLength: Int = 254,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = keyboardType, imeAction = ImeAction.Done
    ),
    isPasswordToggleDisplayed: Boolean = keyboardType == KeyboardType.Password,
    isPasswordVisible: Boolean = false,
    onPasswordToggleClick: (Boolean) -> Unit = {},
    inlineSuffix: String? = null,
    errorMessage: String? = null,
    title: String? = null,
    titleColor: Color? = null,
    titleTextStyle: TextStyle? = null,
    charCountTextStyle: TextStyle? = null,
    isTextCharCountVisible: Boolean = false,
    onDone: (() -> Unit)? = null,
    onFocusCleared: (() -> Unit)? = null,
    enabled: Boolean = true,
    requestFocus: Boolean = false,
    containerColor: Color? = null,
    disabledContainerColor: Color? = null,
    textColor: Color? = null,
    disabledTextColor: Color? = null,
    readOnlyTextColor: Color? = null,
    hintTextStyle: TextStyle? = null,
    errorTextStyle: TextStyle? = null,
    focusedBorderColor: Color? = null,
    unfocusedBorderColor: Color? = null,
    cursorColor: Color? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onValueChange: (String) -> Unit,
) {
    val componentTheme = LocalComponentTheme.current
    val customTextFieldTheme = componentTheme.customTextField

    val finalTextStyle = textStyle ?: customTextFieldTheme.textStyle
    val finalTitleTextStyleBase = titleTextStyle ?: customTextFieldTheme.titleTextStyle
    val finalTitleTextStyle = titleColor?.let { finalTitleTextStyleBase.copy(color = it) }
        ?: finalTitleTextStyleBase
    val finalCharCountTextStyle =
        charCountTextStyle ?: customTextFieldTheme.charCountTextStyle
    val finalHintStyle = hintTextStyle ?: customTextFieldTheme.hintTextStyle
    val finalErrorStyle = errorTextStyle ?: customTextFieldTheme.errorTextStyle
    val finalShape = shape ?: customTextFieldTheme.containerShape
    val finalBorderStroke = borderStroke ?: customTextFieldTheme.borderStroke
    val borderEnabled = isBorderActive ?: customTextFieldTheme.isBorderActive
    val finalContainerColor = containerColor ?: customTextFieldTheme.containerColor
    val finalDisabledContainerColor = disabledContainerColor ?: customTextFieldTheme.disabledContainerColor
    val finalEnabledTextColor = textColor ?: customTextFieldTheme.textColor
    val finalDisabledTextColor = disabledTextColor ?: customTextFieldTheme.disabledTextColor
    val finalReadOnlyTextColor = readOnlyTextColor ?: customTextFieldTheme.readOnlyTextColor
    val finalCursorColor = cursorColor ?: customTextFieldTheme.cursorColor

    val resolvedTextColor = when {
        !enabled -> finalDisabledTextColor
        readOnly -> finalReadOnlyTextColor
        else -> finalEnabledTextColor
    }

    val textFieldColors = TextFieldDefaults.colors(
        disabledIndicatorColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledTextColor = finalDisabledTextColor,
        disabledContainerColor = finalDisabledContainerColor,
        focusedContainerColor = finalContainerColor,
        unfocusedContainerColor = finalContainerColor,
        cursorColor = finalCursorColor
    )

    val inlineSuffixTransformation = remember(
        inlineSuffix,
        resolvedTextColor,
        finalTextStyle
    ) {
        inlineSuffix?.takeIf { it.isNotEmpty() }?.let {
            InlineSuffixTransformation(it, finalTextStyle.copy(color = resolvedTextColor))
        }
    }

    val shouldShowInlineSuffix = inlineSuffixTransformation != null && value.isNotBlank()

    val finalVisualTransformation = when {
        visualTransformation != VisualTransformation.None -> visualTransformation
        !isPasswordVisible && isPasswordToggleDisplayed -> PasswordVisualTransformation()
        shouldShowInlineSuffix -> inlineSuffixTransformation!!
        else -> VisualTransformation.None
    }

    val focusRequester = remember { FocusRequester() }

    var isFocused by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }

    // The field renders from an internal TextFieldValue so that the cursor/selection is owned here,
    // not reset by Material's String-based TextField overload. Callers still hoist a plain String;
    // we only pull an external value back in when the field is NOT being actively edited (programmatic
    // reset / prefill). While focused, the internal buffer is authoritative — this is what prevents a
    // stale value echoed back through async hoisted state (e.g. an MVI StateFlow round-trip) from
    // snapping the cursor to the end while the user types quickly.
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    if (!isFocused && value != textFieldValueState.text) {
        textFieldValueState = TextFieldValue(text = value, selection = TextRange(value.length))
    }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }

    val focusedBorderBrushOverride = focusedBorderColor?.let { SolidColor(it) }
    val unfocusedBorderBrushOverride = unfocusedBorderColor?.let { SolidColor(it) }
    val focusedStroke = BorderStroke(
        width = focusedBorderWidth ?: finalBorderStroke.width,
        brush = focusedBorderBrushOverride ?: finalBorderStroke.brush
    )
    val unfocusedStroke = BorderStroke(
        width = unfocusedBorderWidth ?: finalBorderStroke.width,
        brush = unfocusedBorderBrushOverride ?: finalBorderStroke.brush
    )
    val appliedBorderStroke = when {
        !borderEnabled -> BorderStroke(0.dp, Color.Transparent)
        isFocused -> focusedStroke
        else -> unfocusedStroke
    }

    Column(modifier = containerModifier) {
        if (title != null || titleTrailingSlot != null || isTextCharCountVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = finalTitleTextStyle
                        )
                    }

                    if (titleTrailingSlot != null) {
                        if (title != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        titleTrailingSlot()
                    }
                }

                if (isTextCharCountVisible) {
                    Text(
                        text = stringResource(Res.string.char_count, textFieldValueState.text.length, maxLength),
                        style = finalCharCountTextStyle
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        RoundedSurface(
            borderStroke = appliedBorderStroke,
            color = if (enabled) finalContainerColor else finalDisabledContainerColor,
            shape = finalShape
        ) {
            Box {
                TextField(
                    value = textFieldValueState,
                    onValueChange = { newValue ->
                        val accepted = isEditable && newValue.text.length <= maxLength
                        if (accepted) {
                            val textChanged = newValue.text != textFieldValueState.text
                            // Keep the user's caret/selection exactly where they put it; only report
                            // the plain-text change upward when the text actually changed.
                            textFieldValueState = newValue
                            if (textChanged) {
                                onValueChange(newValue.text)
                            }
                        }
                        // Rejected (maxLength / not editable): leave textFieldValueState untouched so
                        // the extra character is dropped and the caret does not move.
                    },
                    modifier = textFieldModifier
                        .fillMaxWidth()
                        .onFocusEvent {
                            isFocused = it.isFocused
                            if (wasFocused && !it.isFocused) {
                                onFocusCleared?.invoke()
                            }
                            wasFocused = it.isFocused
                        }
                        .align(Alignment.CenterStart)
                        .focusRequester(focusRequester),
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = finalTextStyle.copy(color = resolvedTextColor),
                    placeholder = {
                        Text(
                            text = hint,
                            style = finalHintStyle,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = leadingSlot,
                    trailingIcon = if (isPasswordToggleDisplayed) {
                        {
                            IconButton(
                                onClick = {
                                    onPasswordToggleClick(!isPasswordVisible)
                                }) {
                                Icon(
                                    painter = painterResource(
                                        if (isPasswordVisible) Res.drawable.shared_ic_password_visible else Res.drawable.shared_ic_password_invisible
                                    ),
                                    tint = CoreCustomGrayHintColor,
                                    contentDescription = stringResource(
                                        if (isPasswordVisible) Res.string.shared_cont_desc_icon_password_visible else Res.string.shared_cont_desc_icon_password_invisible
                                    )
                                )
                            }
                        }
                    } else trailingSlot,
                    suffix = suffixSlot,
                    visualTransformation = finalVisualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onDone?.invoke()
                        }),
                    maxLines = maxLines,
                    singleLine = singleLine,
                    colors = textFieldColors,
                )
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = it,
                style = finalErrorStyle
            )
        }
    }
}

@Preview
@Composable
fun CustomTextFieldPreview() {
    AppTheme {
        CustomTextField(
            value = "test", onValueChange = {})
    }
}

@Preview
@Composable
fun CustomTextFieldWithErrorPreview() {
    AppTheme {
        CustomTextField(
            value = "test",
            onValueChange = {},
            errorMessage = "Error message",
        )
    }
}

@Preview
@Composable
fun CustomTextFieldLightSurfacePreview() {
    AppTheme {
        CustomTextField(value = "test", onValueChange = {}, trailingSlot = {
            Icon(
                painter = painterResource(Res.drawable.shared_ic_password_invisible),
                contentDescription = ""
            )
        })
    }
}

@Preview
@Composable
fun CustomTextFieldWithErrorLightSurfacePreview() {
    AppTheme {
        CustomTextField(
            value = "test",
            onValueChange = {},
            errorMessage = "Error message",
            trailingSlot = {
                Icon(
                    painter = painterResource(Res.drawable.shared_ic_password_invisible),
                    contentDescription = ""
                )
            })
    }
}
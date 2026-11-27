package core.presentation.figma

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.figma.code.connect.FigmaConnect
import core.presentation.component.Slot
import core.presentation.component.textfield.CustomTextField
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_password_invisible
import org.jetbrains.compose.resources.painterResource

@FigmaConnect(
    url = "https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=4-41&m=dev"
)
class CustomTextFieldDoc {
    // --- INTERNAL ENUMS MIRRORING FIGMA VARIANTS / PROPS ---

    enum class KeyboardTypeVariant { Text, Password, Number, Email, Phone, Uri }

    enum class ShapeVariant { Rounded, Pill, Square }

    enum class SlotPresence { None, Leading, Trailing, Suffix, TitleTrailing, Multiple }

    // --- FIGMA-PROPERTIES (MAPPED TO FIGMA COMPONENT PROPS) ---

    val value: String = ""

    val hint: String = "Enter text"

    val title: String? = null

    val errorMessage: String? = null

    val enabled: Boolean = true

    val readOnly: Boolean = false

    val isPasswordVisible: Boolean = false

    val isTextCharCountVisible: Boolean = false
    // When enabled, displays character count in format: "current/max" (e.g., "25/254")
    // Shown in the top-right corner, next to the title if present.

    val keyboardTypeVariant: KeyboardTypeVariant = KeyboardTypeVariant.Text

    val shapeVariant: ShapeVariant = ShapeVariant.Rounded

    val isBorderActive: Boolean = true

    val slotPresence: SlotPresence = SlotPresence.None

    val maxLength: Int = 254

    val inlineSuffix: String? = null
    // Optional suffix text displayed inline at the end of the input text.
    // Only visible when value is not blank. Useful for units (e.g., "kg", "cm").

    val requestFocus: Boolean = false
    // When true, automatically requests focus when the component is first composed.
    // Useful for auto-focusing the first field in a form.

    val maxLines: Int = Int.MAX_VALUE
    // Maximum number of lines for multi-line input. Default is Int.MAX_VALUE (unlimited).
    // Only effective when singleLine is false.

    val singleLine: Boolean = true
    // When true, restricts input to a single line. When false, allows multi-line input.
    // Multi-line mode respects maxLines constraint.

    // --- DERIVED VALUES (NOT DIRECTLY FIGMA PROPS, BUT NEEDED FOR FULL API) ---

    // 1. keyboardType
    val keyboardType: KeyboardType
        get() = when (keyboardTypeVariant) {
            KeyboardTypeVariant.Text -> KeyboardType.Text
            KeyboardTypeVariant.Password -> KeyboardType.Password
            KeyboardTypeVariant.Number -> KeyboardType.Number
            KeyboardTypeVariant.Email -> KeyboardType.Email
            KeyboardTypeVariant.Phone -> KeyboardType.Phone
            KeyboardTypeVariant.Uri -> KeyboardType.Uri
        }

    // 2. keyboardOptions
    val keyboardOptions: KeyboardOptions
        get() = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        )

    // 3. shape
    val shape: CornerBasedShape?
        get() = when (shapeVariant) {
            ShapeVariant.Rounded -> RoundedCornerShape(8.dp)
            ShapeVariant.Pill -> RoundedCornerShape(50.dp)
            ShapeVariant.Square -> RoundedCornerShape(0.dp)
        }

    // 4. slots
    val leadingSlot: Slot?
        get() = when (slotPresence) {
            SlotPresence.Leading, SlotPresence.Multiple -> {
                {
                    androidx.compose.material3.Icon(
                        painter = painterResource(Res.drawable.shared_ic_password_invisible),
                        contentDescription = null
                    )
                }
            }
            else -> null
        }

    val trailingSlot: Slot?
        get() = when (slotPresence) {
            SlotPresence.Trailing, SlotPresence.Multiple -> {
                {
                    androidx.compose.material3.Icon(
                        painter = painterResource(Res.drawable.shared_ic_password_invisible),
                        contentDescription = null
                    )
                }
            }
            else -> null
        }

    val suffixSlot: Slot?
        get() = when (slotPresence) {
            SlotPresence.Suffix, SlotPresence.Multiple -> {
                {
                    androidx.compose.material3.Icon(
                        painter = painterResource(Res.drawable.shared_ic_password_invisible),
                        contentDescription = null
                    )
                }
            }
            else -> null
        }

    val titleTrailingSlot: Slot?
        get() = when (slotPresence) {
            SlotPresence.TitleTrailing, SlotPresence.Multiple -> {
                {
                    androidx.compose.material3.Icon(
                        painter = painterResource(Res.drawable.shared_ic_password_invisible),
                        contentDescription = null
                    )
                }
            }
            else -> null
        }

    // 5. password toggle
    // Automatically displayed when keyboardType is Password.
    // Shows visibility toggle icon that allows users to show/hide password text.
    val isPasswordToggleDisplayed: Boolean
        get() = keyboardTypeVariant == KeyboardTypeVariant.Password

    // 6. visualTransformation
    val visualTransformation: VisualTransformation
        get() = VisualTransformation.None

    // 7. modifiers
    val textFieldModifier: Modifier = Modifier
    val containerModifier: Modifier = Modifier

    // 8. text styles (null = use theme defaults)
    val textStyle: TextStyle? = null
    val titleTextStyle: TextStyle? = null
    val charCountTextStyle: TextStyle? = null
    val hintTextStyle: TextStyle? = null
    val errorTextStyle: TextStyle? = null

    // 9. colors (null = use theme defaults)
    val containerColor: Color? = null
    val disabledContainerColor: Color? = null
    val textColor: Color? = null
    val disabledTextColor: Color? = null
    val readOnlyTextColor: Color? = null
    val titleColor: Color? = null
    val focusedBorderColor: Color? = null
    val unfocusedBorderColor: Color? = null
    val cursorColor: Color? = null

    // 10. border
    val borderStroke: BorderStroke? = null
    val focusedBorderWidth: Dp? = null
    val unfocusedBorderWidth: Dp? = null

    // 11. other properties
    // isEditable: Controls whether text can be edited. Default is true.
    // When false, text input is blocked but field remains interactive.
    val isEditable: Boolean = true

    // 12. callbacks
    // onDone: Invoked when user presses the IME action (Done) button on keyboard.
    // onFocusCleared: Invoked when focus is removed from the field.
    val onValueChange: (String) -> Unit = {}
    val onPasswordToggleClick: (Boolean) -> Unit = {}
    val onDone: (() -> Unit)? = null
    val onFocusCleared: (() -> Unit)? = null

    // --- THE COMPOSABLE SNIPPET (USES *ALL* PARAMETERS EXPLICITLY) ---

    @Composable
    fun Component() {
        CustomTextField(
            textFieldModifier = textFieldModifier,
            value = value,
            hint = hint,
            containerModifier = containerModifier,
            leadingSlot = leadingSlot,
            trailingSlot = trailingSlot,
            suffixSlot = suffixSlot,
            titleTrailingSlot = titleTrailingSlot,
            textStyle = textStyle,
            isBorderActive = isBorderActive,
            shape = shape,
            borderStroke = borderStroke,
            focusedBorderWidth = focusedBorderWidth,
            unfocusedBorderWidth = unfocusedBorderWidth,
            maxLines = maxLines,
            singleLine = singleLine,
            isEditable = isEditable,
            readOnly = readOnly,
            maxLength = maxLength,
            keyboardType = keyboardType,
            keyboardOptions = keyboardOptions,
            isPasswordToggleDisplayed = isPasswordToggleDisplayed,
            isPasswordVisible = isPasswordVisible,
            onPasswordToggleClick = onPasswordToggleClick,
            inlineSuffix = inlineSuffix,
            errorMessage = errorMessage,
            title = title,
            titleColor = titleColor,
            titleTextStyle = titleTextStyle,
            charCountTextStyle = charCountTextStyle,
            isTextCharCountVisible = isTextCharCountVisible,
            onDone = onDone,
            onFocusCleared = onFocusCleared,
            enabled = enabled,
            requestFocus = requestFocus,
            containerColor = containerColor,
            disabledContainerColor = disabledContainerColor,
            textColor = textColor,
            disabledTextColor = disabledTextColor,
            readOnlyTextColor = readOnlyTextColor,
            hintTextStyle = hintTextStyle,
            errorTextStyle = errorTextStyle,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            cursorColor = cursorColor,
            visualTransformation = visualTransformation,
            onValueChange = onValueChange
        )
    }
}

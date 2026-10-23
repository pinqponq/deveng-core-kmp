package core.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import core.presentation.theme.CoreSemiBoldTextStyle
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable button component with support for leading/trailing icons and various styling options.
 *
 * @param modifier Modifier to be applied to the button container. Default height is 50dp and width is 200dp.
 * @param text Optional text to display inside the button.
 * @param textStyle Optional text style. If null, uses the theme's default button text style.
 * @param textModifier Modifier to be applied to the text component.
 * @param enabled Whether the button is enabled and can be clicked. Default is true.
 * @param shape The shape of the button. Default is RoundedCornerShape(8.dp).
 * @param containerColor The background color of the button when enabled. If null, uses theme default.
 * @param disabledContainerColor The background color of the button when disabled. If null, uses theme default.
 * @param disabledContentColor The content (text/icon) color when disabled. If null, uses theme default.
 * @param contentColor The content (text/icon) color when enabled. If null, uses theme default.
 * @param elevation The elevation/shadow of the button. Default is 1.dp elevation.
 * @param trailingIconModifier Modifier to be applied to the trailing icon.
 * @param trailingIcon Optional drawable resource for an icon displayed after the text.
 * @param trailingIconTint Color tint for the trailing icon. Default is Color.Black.
 * @param trailingIconContentDescription Content description for accessibility for the trailing icon.
 * @param leadingIconModifier Modifier to be applied to the leading icon.
 * @param leadingIcon Optional drawable resource for an icon displayed before the text.
 * @param leadingIconTint Color tint for the leading icon. Default is Color.Black.
 * @param leadingIconContentDescription Content description for accessibility for the leading icon.
 * @param contentArrangement Horizontal arrangement of button content (icons and text). Default is Center.
 * @param contentPadding Space between the button's edge and its content. Default is Material's ButtonDefaults.ContentPadding
 * (24.dp horizontal, 8.dp vertical). Pass a smaller value so the label fits on a narrow or short button.
 * @param onClick Callback invoked when the button is clicked.
 */
@Composable
fun CustomButton(
    modifier: Modifier = Modifier
        .height(50.dp)
        .width(200.dp),
    text: String? = null,
    textStyle: TextStyle? = null,
    secondaryText: String? = null,
    secondaryTextStyle: TextStyle? = null,
    secondaryTextModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(8.dp),
    border: BorderStroke? = null,
    containerColor: Color? = null,
    disabledContainerColor: Color? = null,
    disabledContentColor: Color? = null,
    contentColor: Color? = null,
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
    trailingIconModifier: Modifier = Modifier,
    trailingIcon: DrawableResource? = null,
    trailingIconTint: Color = Color.Black,
    trailingIconContentDescription: String? = null,
    leadingIconModifier: Modifier = Modifier,
    leadingIcon: DrawableResource? = null,
    leadingIconTint: Color = Color.Black,
    leadingIconContentDescription: String? = null,
    contentArrangement: Arrangement.Horizontal = Arrangement.Center,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    onClick: () -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val buttonTheme = componentTheme.button

    val finalTextStyle = textStyle ?: CoreSemiBoldTextStyle().copy(
        fontSize = buttonTheme.defaultTextStyle.fontSize,
        textAlign = buttonTheme.defaultTextStyle.textAlign
    )
    val finalContainerColor = containerColor ?: buttonTheme.containerColor
    val finalDisabledContainerColor = disabledContainerColor ?: buttonTheme.disabledContainerColor
    val finalContentColor = contentColor ?: buttonTheme.contentColor
    val finalDisabledContentColor = disabledContentColor ?: buttonTheme.disabledContentColor
    Button(
        modifier = modifier,
        enabled = enabled,
        colors = ButtonColors(
            containerColor = finalContainerColor,
            contentColor = finalContentColor,
            disabledContentColor = finalDisabledContentColor,
            disabledContainerColor = finalDisabledContainerColor
        ),
        elevation = elevation,
        shape = shape,
        border = border,
        contentPadding = contentPadding,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = contentArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    modifier = leadingIconModifier,
                    painter = painterResource(leadingIcon),
                    contentDescription = leadingIconContentDescription,
                    tint = leadingIconTint
                )

                Spacer(modifier = Modifier.width(15.dp))
            }

            if (text != null) {
                Text(
                    modifier = textModifier,
                    text = text,
                    style = finalTextStyle
                )
            }

            if (secondaryText != null) {
                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    modifier = secondaryTextModifier,
                    text = secondaryText,
                    style = secondaryTextStyle ?: finalTextStyle
                )
            }

            trailingIcon?.let {
                Spacer(modifier = Modifier.width(18.dp))

                Icon(
                    modifier = trailingIconModifier,
                    painter = painterResource(trailingIcon),
                    contentDescription = trailingIconContentDescription,
                    tint = trailingIconTint
                )
            }
        }
    }
}

@Preview
@Composable
fun CustomButtonPreview() {
    CustomButton(
        text = "Custom Button",
        onClick = {}
    )
}
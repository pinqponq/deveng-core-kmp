package core.presentation.component.alertdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import core.presentation.component.CustomButton
import core.presentation.component.Slot
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_left
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable alert dialog component with header, body, and action buttons.
 * Supports optional icon, title, description, and custom content.
 *
 * @param contentModifier Modifier to be applied to the dialog content container.
 * @param dialogModifier Modifier to be applied to the dialog container.
 * @param headerModifier Modifier to be applied to the header container.
 * @param buttonContainerModifier Modifier to be applied to the button container. Default height is 50.dp.
 * @param positiveButtonModifier Modifier to be applied to the positive button. Default height is 50.dp.
 * @param negativeButtonModifier Modifier to be applied to the negative button. Default height is 50.dp.
 * @param isDialogVisible Whether the dialog is visible.
 * @param headerIcon Optional drawable resource for an icon displayed in the header.
 * @param iconDescription Content description for accessibility for the header icon.
 * @param iconTint Color tint for the header icon. If null, uses theme default.
 * @param title Optional title text displayed in the header.
 * @param titleColor Color of the title text. If null, uses theme default.
 * @param titleTextStyle Text style for the title. If null, uses theme default.
 * @param headerColor Background color of the header. If null, uses theme default.
 * @param descriptionAnnotated Optional annotated description text displayed in the body.
 * @param description Optional description text displayed in the body.
 * @param descriptionColor Color of the description text. If null, uses theme default.
 * @param descriptionTextStyle Text style for the description. If null, uses theme default.
 * @param bodyColor Background color of the body. If null, uses theme default.
 * @param positiveButtonText Optional text for the positive/confirm button.
 * @param positiveButtonColor Background color of the positive button. If null, uses theme default.
 * @param positiveButtonTextColor Text color of the positive button. If null, uses theme default.
 * @param positiveButtonTextStyle Text style for the positive button. If null, uses theme default.
 * @param onPositiveButtonClick Callback invoked when the positive button is clicked.
 * @param negativeButtonText Optional text for the negative/cancel button.
 * @param negativeButtonColor Background color of the negative button. If null, uses theme default.
 * @param negativeButtonTextColor Text color of the negative button. If null, uses theme default.
 * @param negativeButtonTextStyle Text style for the negative button. If null, uses theme default.
 * @param onNegativeButtonClick Callback invoked when the negative button is clicked.
 * @param onDismissRequest Callback invoked when the dialog should be dismissed.
 * @param content Optional custom composable content displayed in the body.
 */
@Composable
fun CustomAlertDialog(
    contentModifier: Modifier = Modifier,
    dialogModifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    buttonContainerModifier: Modifier = Modifier.height(50.dp),
    positiveButtonModifier: Modifier = Modifier.height(50.dp),
    negativeButtonModifier: Modifier = Modifier.height(50.dp),
    isDialogVisible: Boolean,
    headerIcon: DrawableResource? = null,
    iconDescription: String? = null,
    iconTint: Color? = null,
    title: String? = null,
    titleColor: Color? = null,
    titleTextStyle: TextStyle? = null,
    headerColor: Color? = null,
    descriptionAnnotated: AnnotatedString? = null,
    description: String? = null,
    descriptionColor: Color? = null,
    descriptionTextStyle: TextStyle? = null,
    bodyColor: Color? = null,
    positiveButtonText: String? = null,
    positiveButtonColor: Color? = null,
    positiveButtonTextColor: Color? = null,
    positiveButtonTextStyle: TextStyle? = null,
    onPositiveButtonClick: () -> Unit = {},
    negativeButtonText: String? = null,
    negativeButtonColor: Color? = null,
    negativeButtonTextColor: Color? = null,
    negativeButtonTextStyle: TextStyle? = null,
    onNegativeButtonClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    content: Slot? = null
) {
    val componentTheme = LocalComponentTheme.current
    val alertDialogTheme = componentTheme.alertDialog

    val finalHeaderColor = headerColor ?: alertDialogTheme.headerColor
    val finalBodyColor = bodyColor ?: alertDialogTheme.bodyColor
    val finalTitleColor = titleColor ?: alertDialogTheme.titleColor
    val finalDescriptionColor = descriptionColor ?: alertDialogTheme.descriptionColor
    val finalDividerColor = alertDialogTheme.dividerColor
    val finalPositiveButtonColor = positiveButtonColor ?: alertDialogTheme.positiveButtonColor
    val finalPositiveButtonTextColor =
        positiveButtonTextColor ?: alertDialogTheme.positiveButtonTextColor
    val finalNegativeButtonColor = negativeButtonColor ?: alertDialogTheme.negativeButtonColor
    val finalNegativeButtonTextColor =
        negativeButtonTextColor ?: alertDialogTheme.negativeButtonTextColor
    val finalIconTint = iconTint ?: alertDialogTheme.iconColor
    val finalTitleTextStyle = titleTextStyle ?: alertDialogTheme.titleTextStyle
    val finalDescriptionTextStyle = descriptionTextStyle ?: alertDialogTheme.descriptionTextStyle
    val finalPositiveButtonTextStyle =
        positiveButtonTextStyle ?: alertDialogTheme.buttonTextStyle
    val finalNegativeButtonTextStyle =
        negativeButtonTextStyle ?: alertDialogTheme.buttonTextStyle


    val finalDescriptionText: AnnotatedString? = descriptionAnnotated
        ?: description?.let { AnnotatedString(it) }

    if (isDialogVisible) {
        CustomDialog(
            modifier = dialogModifier,
            header = {
                CustomDialogHeader(
                    modifier = headerModifier
                        .background(color = finalHeaderColor)
                        .padding(top = 15.dp),
                    title = title,
                    texColor = finalTitleColor,
                    icon = headerIcon,
                    iconDescription = iconDescription,
                    iconTint = finalIconTint,
                    style = finalTitleTextStyle
                )
            },
            onDismissRequest = {
                onDismissRequest()
            }
        ) {
            Column(
                modifier = contentModifier
                    .background(color = finalBodyColor)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (finalDescriptionText != null) {
                    Text(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterHorizontally)
                            .padding(
                                vertical = 15.dp,
                                horizontal = 30.dp
                            ),
                        text = finalDescriptionText,
                        style = finalDescriptionTextStyle.copy(
                            fontSize = alertDialogTheme.descriptionTextStyle.fontSize,
                            textAlign = TextAlign.Center
                        ),
                        textAlign = TextAlign.Center,
                        color = finalDescriptionColor
                    )
                }

                if (content != null) {
                    content()
                }

                if (negativeButtonText != null || positiveButtonText != null) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = finalDividerColor
                    )
                }

                Row(
                    modifier = buttonContainerModifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (negativeButtonText != null) {
                        CustomButton(
                            modifier = negativeButtonModifier.weight(1f),
                            text = negativeButtonText,
                            textStyle = finalNegativeButtonTextStyle.copy(
                                fontSize = alertDialogTheme.buttonTextStyle.fontSize,
                                color = finalNegativeButtonTextColor
                            ),
                            containerColor = finalNegativeButtonColor,
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            shape = RoundedCornerShape(0.dp),
                            onClick = { onNegativeButtonClick() }
                        )
                    }

                    if (negativeButtonText != null && positiveButtonText != null) {
                        VerticalDivider(
                            thickness = 1.dp,
                            color = finalDividerColor
                        )
                    }

                    if (positiveButtonText != null) {
                        CustomButton(
                            modifier = positiveButtonModifier.weight(1f),
                            text = positiveButtonText,
                            textStyle = finalPositiveButtonTextStyle.copy(
                                fontSize = alertDialogTheme.buttonTextStyle.fontSize,
                                color = finalPositiveButtonTextColor
                            ),
                            containerColor = finalPositiveButtonColor,
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            shape = RoundedCornerShape(0.dp),
                            onClick = { onPositiveButtonClick() }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun CustomAlertDialogErrorPreview() {
    AppTheme {
        CustomAlertDialog(
            isDialogVisible = true,
            title = "Error",
            description = "Error description",
            onDismissRequest = {},
            content = {
                Icon(
                    modifier = Modifier.size(62.dp),
                    painter = painterResource(Res.drawable.shared_ic_arrow_left),
                    contentDescription = "Icon",
                    tint = Color.Black
                )
            }
        )
    }
}

@Preview
@Composable
fun CustomAlertDialogSuccessPreview() {
    AppTheme {
        CustomAlertDialog(
            isDialogVisible = true,
            title = "Title",
            description = "Description, description, description, description. ",
            positiveButtonText = "Confirm",
            negativeButtonText = "Cancel",
            content = {
                Icon(
                    painter = painterResource(Res.drawable.shared_ic_arrow_left),
                    contentDescription = "Icon",
                    tint = Color.Cyan,
                    modifier = Modifier.size(62.dp)
                )
            }
        )
    }
}
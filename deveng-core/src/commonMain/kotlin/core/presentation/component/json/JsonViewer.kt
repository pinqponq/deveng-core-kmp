package core.presentation.component.json

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.PickerField
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.util.calculateTextWidthAsDp
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_desc_icon_copied
import global.deveng.deveng_core.generated.resources.shared_content_desc_icon_copy
import global.deveng.deveng_core.generated.resources.shared_copied
import global.deveng.deveng_core.generated.resources.shared_copy
import global.deveng.deveng_core.generated.resources.shared_ic_accept
import global.deveng.deveng_core.generated.resources.shared_ic_copy
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A component for displaying JSON text with formatting.
 * Includes a title, formatted JSON display container, and a copy button that changes state when clicked.
 *
 * @param title Title text displayed above the JSON viewer.
 * @param json The JSON string to display and format.
 * @param containerColor Background color of the JSON display container. If null, uses theme default.
 * @param buttonTextColor Text color of the copy button. If null, uses theme default.
 * @param buttonColor Background color of the copy button. If null, uses theme default.
 * @param copyIcon Drawable resource for the icon displayed when JSON is not copied.
 * @param copyIconTint Color tint for the copy icon. If null, uses theme default.
 * @param copyIconDescription Content description for accessibility for the copy icon.
 * @param copiedIcon Drawable resource for the icon displayed when JSON has been copied.
 * @param copiedIconTint Color tint for the copied icon. If null, uses theme default.
 * @param copiedIconDescription Content description for accessibility for the copied icon.
 * @param onClickCopyJsonIcon Callback invoked when the copy button is clicked, receives the JSON string.
 * @param titleTextStyle Text style for the title. If null, uses theme default.
 * @param jsonTextStyle Text style for the formatted JSON text. If null, uses theme default.
 * @param containerShape Shape of the JSON display container. If null, uses theme default.
 * @param containerPadding Padding inside the JSON display container. If null, uses theme default.
 * @param buttonShape Shape of the copy button. If null, uses theme default.
 * @param buttonHeight Height of the copy button. If null, uses theme default.
 * @param buttonIconSize Size of the button icon. If null, uses theme default.
 * @param containerColor Background color of the JSON display container.
 * @param titleTextStyle Text style for the title.
 * @param jsonTextStyle Text style for the formatted JSON text.
 * @param containerShape Shape of the JSON display container.
 * @param containerPadding Padding inside the JSON display container.
 */
@Composable
fun JsonViewer(
    modifier: Modifier = Modifier,
    title: String,
    titleTextStyle: TextStyle? = null,
    json: String,
    jsonTextStyle: TextStyle? = null,
    containerColor: Color? = null,
    containerShape: CornerBasedShape? = null,
    containerPadding: Dp? = null,
    buttonTextColor: Color? = null,
    buttonColor: Color? = null,
    buttonShape: CornerBasedShape? = null,
    buttonHeight: Dp? = null,
    buttonIconSize: Dp? = null,
    copyIcon: DrawableResource = Res.drawable.shared_ic_copy,
    copyIconTint: Color? = null,
    copyIconDescription: String = stringResource(Res.string.shared_content_desc_icon_copy),
    copiedIcon: DrawableResource = Res.drawable.shared_ic_accept,
    copiedIconTint: Color? = null,
    copiedIconDescription: String = stringResource(Res.string.shared_content_desc_icon_copied),
    onClickCopyJsonIcon: (String) -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val jsonViewerTheme = componentTheme.jsonViewer

    val finalTitleTextStyle = titleTextStyle ?: jsonViewerTheme.titleTextStyle
    val finalJsonTextStyle = jsonTextStyle ?: jsonViewerTheme.jsonTextStyle
    val finalContainerColor = containerColor ?: jsonViewerTheme.containerColor
    val finalContainerShape = containerShape ?: jsonViewerTheme.containerShape
    val finalContainerPadding = containerPadding ?: jsonViewerTheme.containerPadding
    val finalButtonBackgroundColor = buttonColor ?: jsonViewerTheme.buttonBackgroundColor
    val finalButtonTextColor = buttonTextColor ?: jsonViewerTheme.buttonTextColor
    val finalButtonTextStyle = jsonViewerTheme.buttonTextStyle
    val finalButtonShape = buttonShape ?: jsonViewerTheme.buttonShape
    val finalButtonHeight = buttonHeight ?: jsonViewerTheme.buttonHeight
    val finalButtonIconSize = buttonIconSize ?: jsonViewerTheme.buttonIconSize
    val finalCopyIconTint = copyIconTint ?: jsonViewerTheme.copyIconTint
    val finalCopiedIconTint = copiedIconTint ?: jsonViewerTheme.copiedIconTint

    var isJsonCopied by remember { mutableStateOf(false) }

    if (isJsonCopied) {
        LaunchedEffect(Unit) {
            /**
             * Provides visual feedback to the user that the copying operation was successful.
             * Temporarily sets the text to "Copied" to confirm the action
             * and returns the button to its default state ("Copy") after a 2-second delay to allow reuse.
             **/
            delay(2000)
            isJsonCopied = false
        }
    }

    val buttonText = if (isJsonCopied)
        stringResource(Res.string.shared_copied)
    else
        stringResource(Res.string.shared_copy)

    val buttonSize = calculateTextWidthAsDp(
        text = buttonText,
        style = finalButtonTextStyle
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = finalTitleTextStyle
            )

            Spacer(modifier = Modifier.weight(1f))

            PickerField(
                modifier = Modifier
                    .width(buttonSize + 70.dp)
                    .height(finalButtonHeight),
                shape = finalButtonShape,
                enabledBackGroundColor = finalButtonBackgroundColor,
                text = buttonText,
                textStyle = finalButtonTextStyle,
                enabledTextColor = finalButtonTextColor,
                trailingSlot = null,
                leadingSlot = {
                    Icon(
                        modifier = Modifier.size(finalButtonIconSize),
                        painter = if (isJsonCopied)
                            painterResource(copiedIcon) else
                            painterResource(copyIcon),
                        contentDescription = if (isJsonCopied)
                            copiedIconDescription else
                            copyIconDescription,
                        tint = if (isJsonCopied)
                            finalCopiedIconTint else
                            finalCopyIconTint
                    )
                },
                onClick = {
                    onClickCopyJsonIcon(json)
                    isJsonCopied = true
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = finalContainerColor,
                    shape = finalContainerShape
                )
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                modifier = modifier.padding(finalContainerPadding),
                text = formatJson(json),
                style = finalJsonTextStyle
            )
        }
    }
}

@Preview
@Composable
fun JsonViewerPreview() {
    AppTheme {
        JsonViewer(
            title = "Input Json",
            json = """[\n  {\n    \"id\": 3,\n    \"currencyCode\": \"TL\",\n    \"currencyName\": \"Türk Lirası\",\n    \"currencySymbolURL\": \"https://giris.turkiye.gov.tr/\"\n  },\n  {\n    \"id\": 4,\n    \"currencyCode\": \"USD\",\n    \"currencyName\": \"Dolar\",\n    \"currencySymbolURL\": \"https://dolar.com/\"\n  }\n]""",
            onClickCopyJsonIcon = {}
        )
    }
}
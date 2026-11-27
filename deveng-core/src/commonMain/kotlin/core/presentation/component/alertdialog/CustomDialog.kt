package core.presentation.component.alertdialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import core.presentation.component.Slot
import core.presentation.theme.CoreCustomBlackColor
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_cancel
import global.deveng.deveng_core.generated.resources.shared_ok
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable dialog wrapper component that provides a base structure for dialogs.
 * Supports optional header and customizable properties.
 *
 * @param modifier Modifier to be applied to the dialog content container.
 * @param onDismissRequest Callback invoked when the dialog should be dismissed.
 * @param properties Dialog properties configuration (dismiss on back press, dismiss on click outside, etc.).
 * @param header Optional composable header content displayed at the top of the dialog.
 * @param backgroundColor Background color of the dialog. Default is Color.Transparent.
 * @param content Composable content displayed in the dialog body.
 */
@Composable
fun CustomDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    header: Slot? = null,
    backgroundColor: Color = Color.Transparent,
    content: Slot
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties.let {
            DialogProperties(
                dismissOnBackPress = it.dismissOnBackPress,
                dismissOnClickOutside = it.dismissOnClickOutside,
                usePlatformDefaultWidth = it.usePlatformDefaultWidth
            )
        },
        content = {
            Surface(
                color = backgroundColor
            ) {
                CustomDialogBody(
                    modifier = modifier,
                    header = header,
                    content = content
                )
            }
        }
    )
}

@Preview
@Composable
fun CustomDialogPreview() {
    CustomDialog(
        modifier = Modifier
            .fillMaxWidth(),
        onDismissRequest = {},
        header = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(Res.string.shared_ok),
                color = CoreCustomBlackColor,
                fontSize = 34.sp
            )
        },
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = Color.Transparent
            ) {
                Text(stringResource(Res.string.shared_cancel))
            }
        }
    )
}
package core.presentation.component.alertdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.presentation.component.RoundedSurface
import core.presentation.component.Slot
import core.presentation.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A dialog body component that wraps content and optional header in a rounded surface.
 *
 * @param modifier Modifier to be applied to the dialog body container.
 * @param header Optional composable header content.
 * @param content Composable content displayed in the dialog body.
 */
@Composable
fun CustomDialogBody(
    modifier: Modifier = Modifier,
    header: Slot? = null,
    content: Slot
) {
    RoundedSurface(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (header != null) {
                header()
            }

            content()
        }
    }
}

@Preview
@Composable
fun CustomDialogBodyPreview() {
    AppTheme {
        CustomDialogBody(
            header = {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            },
            content = {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        )
    }
}

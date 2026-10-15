package core.serverdriven.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import core.presentation.theme.LocalComponentTheme

@Composable
internal fun ServerDrivenTitledContent(
    modifier: Modifier,
    title: String,
    description: String?,
    content: @Composable () -> Unit
) {
    val serverDrivenScreenTheme = LocalComponentTheme.current.serverDrivenScreen

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(serverDrivenScreenTheme.headerSpacing)
    ) {
        Text(
            text = title,
            style = serverDrivenScreenTheme.titleTextStyle
        )

        if (description != null) {
            Text(
                text = description,
                style = serverDrivenScreenTheme.descriptionTextStyle
            )
        }

        content()
    }
}

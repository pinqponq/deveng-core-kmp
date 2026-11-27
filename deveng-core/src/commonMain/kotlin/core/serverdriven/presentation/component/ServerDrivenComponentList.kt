package core.serverdriven.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import core.presentation.theme.LocalComponentTheme
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.ComponentProperty
import core.serverdriven.presentation.model.ComponentUiModel

@Composable
internal fun ServerDrivenComponentList(
    modifier: Modifier,
    components: List<ComponentUiModel>,
    componentValues: Map<String, Any>,
    componentProperties: Map<String, ComponentProperty>,
    scopeKey: String?,
    containerType: ContainerType,
    onComponentEvent: (ComponentEvent) -> Unit
) {
    val serverDrivenScreenTheme = LocalComponentTheme.current.serverDrivenScreen
    when (containerType) {
        ContainerType.COLUMN -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(serverDrivenScreenTheme.columnSpacing)
        ) {
            components.forEach { component ->
                ServerDrivenComponent(
                    modifier = Modifier.fillMaxWidth(),
                    component = component,
                    componentValues = componentValues,
                    componentProperties = componentProperties,
                    scopeKey = scopeKey,
                    onComponentEvent = onComponentEvent
                )
            }
        }

        ContainerType.ROW -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(serverDrivenScreenTheme.rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            components.forEach { component ->
                ServerDrivenComponent(
                    modifier = Modifier.weight(1f),
                    component = component,
                    componentValues = componentValues,
                    componentProperties = componentProperties,
                    scopeKey = scopeKey,
                    onComponentEvent = onComponentEvent
                )
            }
        }
    }
}

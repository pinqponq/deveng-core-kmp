package core.serverdriven.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import core.serverdriven.presentation.engine.ServerDrivenEngine
import core.serverdriven.presentation.model.ComponentEvent

/**
 * Draws the layout held by a [ServerDrivenEngine] with the core components and theme.
 *
 * @param engine Engine that holds the layout, its values and the derived component properties.
 * @param modifier Modifier applied to the outer column.
 * @param onComponentEvent Called for every user action; pass it to [ServerDrivenEngine.handleEvent].
 */
@Composable
fun ServerDrivenScreen(
    engine: ServerDrivenEngine,
    modifier: Modifier = Modifier,
    onComponentEvent: (ComponentEvent) -> Unit
) {
    val components by engine.visibleComponents.collectAsState()
    val componentValues by engine.componentValues.collectAsState()
    val componentProperties by engine.componentProperties.collectAsState()

    ServerDrivenComponentList(
        modifier = modifier,
        components = components,
        componentValues = componentValues,
        componentProperties = componentProperties,
        scopeKey = null,
        containerType = ContainerType.COLUMN,
        onComponentEvent = onComponentEvent
    )
}

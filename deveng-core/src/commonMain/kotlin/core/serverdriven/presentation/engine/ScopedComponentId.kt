package core.serverdriven.presentation.engine

private const val COMPONENT_KEY_SEPARATOR = "/"

/**
 * A component's place in the layout. [scopeKeys] are the keys of the tabs around the component,
 * outermost first; [key] addresses its value and properties.
 */
internal data class ScopedComponentId(
    val key: String,
    val componentId: String,
    val scopeKeys: List<String>
)

/**
 * Ids only need to be unique inside one tab, so a component inside a tab is keyed by the tab's key
 * and its id (`day_tab/schedule_switch`); a component outside every tab is keyed by its id alone.
 */
internal fun scopedComponentKey(
    scopeKey: String?,
    componentId: String
): String {
    return if (scopeKey == null) componentId else "$scopeKey$COMPONENT_KEY_SEPARATOR$componentId"
}

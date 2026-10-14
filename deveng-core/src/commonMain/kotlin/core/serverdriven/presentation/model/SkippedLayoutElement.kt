package core.serverdriven.presentation.model

/**
 * A part of a layout the engine skipped because this library does not know it.
 *
 * @param name The unknown component type, operator or effect; `null` when a component has no `type`.
 * @param componentId The skipped component's id, or the target id of the skipped dependency.
 */
data class SkippedLayoutElement(
    val kind: SkippedElementKind,
    val name: String?,
    val componentId: String
)

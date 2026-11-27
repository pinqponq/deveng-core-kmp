package core.serverdriven.presentation.model

internal data class DependencyUiModel(
    val sourceId: String,
    val targetId: String,
    val operator: DependencyOperator,
    val triggerValue: String,
    val effect: DependencyEffect,
    val effectValue: Boolean
)

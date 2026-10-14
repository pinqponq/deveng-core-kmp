package core.serverdriven.presentation.model

data class TabUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val components: List<ComponentUiModel>
)

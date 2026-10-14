package core.serverdriven.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class LayoutResponse(
    val components: List<ComponentResponse> = emptyList(),
    val dependencies: List<DependencyResponse> = emptyList()
)

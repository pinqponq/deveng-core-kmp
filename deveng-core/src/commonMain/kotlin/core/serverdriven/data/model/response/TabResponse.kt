package core.serverdriven.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class TabResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val components: List<ComponentResponse> = emptyList()
)

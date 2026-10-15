package core.serverdriven.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class DependencyResponse(
    val sourceId: String,
    val targetId: String,
    val operator: String,
    val triggerValue: String,
    val effectType: String,
    val effectValue: Boolean
)

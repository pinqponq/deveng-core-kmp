package core.serverdriven.presentation.model.mapper

import core.serverdriven.data.model.response.DependencyResponse
import core.serverdriven.presentation.model.DependencyEffect
import core.serverdriven.presentation.model.DependencyOperator
import core.serverdriven.presentation.model.DependencyUiModel
import core.util.CustomLogger

class DependencyUiMapper {

    fun mapToUiModels(
        responses: List<DependencyResponse>
    ): List<DependencyUiModel> {
        return responses.mapNotNull { response -> mapToUiModel(response) }
    }

    private fun mapToUiModel(
        response: DependencyResponse
    ): DependencyUiModel? {
        val operator = DependencyOperator.fromValue(response.operator)
        val effect = DependencyEffect.fromValue(response.effectType)

        if (operator == null || effect == null) {
            CustomLogger.log(
                "Server-driven UI: skipped dependency '${response.sourceId}' -> '${response.targetId}' " +
                    "with unknown operator '${response.operator}' or effect '${response.effectType}'"
            )
            return null
        }

        return DependencyUiModel(
            sourceId = response.sourceId,
            targetId = response.targetId,
            operator = operator,
            triggerValue = response.triggerValue,
            effect = effect,
            effectValue = response.effectValue
        )
    }
}

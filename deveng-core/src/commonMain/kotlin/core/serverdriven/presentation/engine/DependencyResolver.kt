package core.serverdriven.presentation.engine

import core.serverdriven.presentation.model.ComponentProperty
import core.serverdriven.presentation.model.DependencyEffect
import core.serverdriven.presentation.model.DependencyOperator
import core.serverdriven.presentation.model.DependencyUiModel

internal class DependencyResolver {

    fun resolveProperties(
        dependencies: List<DependencyUiModel>,
        componentValues: Map<String, Any>,
        componentProperties: Map<String, ComponentProperty>
    ): Map<String, ComponentProperty> {
        val resolvedProperties = componentProperties.mapValues { (_, property) ->
            property.copy(isVisible = true, isEditable = true)
        }.toMutableMap()

        dependencies.forEach { dependency ->
            val sourceValue = componentValues[dependency.sourceId] ?: return@forEach
            val targetProperty = resolvedProperties[dependency.targetId] ?: return@forEach

            resolvedProperties[dependency.targetId] = applyEffect(
                dependency = dependency,
                sourceValue = sourceValue,
                targetProperty = targetProperty
            )
        }

        return resolvedProperties
    }

    private fun applyEffect(
        dependency: DependencyUiModel,
        sourceValue: Any,
        targetProperty: ComponentProperty
    ): ComponentProperty {
        val isTriggered = checkTriggerMatch(
            sourceValue = sourceValue,
            operator = dependency.operator,
            triggerValue = dependency.triggerValue
        )

        val effectValue = if (isTriggered) dependency.effectValue else !dependency.effectValue

        return when (dependency.effect) {
            DependencyEffect.VISIBILITY -> targetProperty.copy(isVisible = effectValue)
            DependencyEffect.EDITABLE -> targetProperty.copy(isEditable = effectValue)
        }
    }

    private fun checkTriggerMatch(
        sourceValue: Any,
        operator: DependencyOperator,
        triggerValue: String
    ): Boolean {
        val numericSourceValue = (sourceValue as? Number)?.toDouble()
        val numericTriggerValue = triggerValue.toDoubleOrNull()
        val isNumericComparison = numericSourceValue != null && numericTriggerValue != null

        return when (operator) {
            DependencyOperator.EQUALS -> if (isNumericComparison) {
                numericSourceValue == numericTriggerValue
            } else {
                sourceValue.toString() == triggerValue
            }

            DependencyOperator.GREATER_THAN ->
                isNumericComparison && numericSourceValue > numericTriggerValue

            DependencyOperator.LESS_THAN ->
                isNumericComparison && numericSourceValue < numericTriggerValue
        }
    }
}

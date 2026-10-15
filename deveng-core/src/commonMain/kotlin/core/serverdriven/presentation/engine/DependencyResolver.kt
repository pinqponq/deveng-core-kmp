package core.serverdriven.presentation.engine

import core.serverdriven.presentation.model.ComponentProperty
import core.serverdriven.presentation.model.DependencyEffect
import core.serverdriven.presentation.model.DependencyOperator
import core.serverdriven.presentation.model.DependencyUiModel

internal class DependencyResolver {

    /**
     * Turns each dependency's ids into component keys. Every component carrying `sourceId` is a source;
     * its target is the `targetId` in the nearest scope around it (its own tab first, then the enclosing
     * tabs, then outside every tab), or every component carrying `targetId` when none of those scopes
     * has one, which keeps a dependency from outside the tabs reaching components inside them.
     */
    fun scopeDependencies(
        dependencies: List<DependencyUiModel>,
        scopedComponentIds: List<ScopedComponentId>
    ): List<DependencyUiModel> {
        return dependencies.flatMap { dependency ->
            val sources = scopedComponentIds.filter { scopedId -> scopedId.componentId == dependency.sourceId }
            val targets = scopedComponentIds.filter { scopedId -> scopedId.componentId == dependency.targetId }

            sources.flatMap { source ->
                findTargetsOf(source, targets).map { target ->
                    dependency.copy(sourceId = source.key, targetId = target.key)
                }
            }
        }
    }

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

    private fun findTargetsOf(
        source: ScopedComponentId,
        targets: List<ScopedComponentId>
    ): List<ScopedComponentId> {
        val nearestTarget = targets
            .filter { target -> source.scopeKeys.take(target.scopeKeys.size) == target.scopeKeys }
            .maxByOrNull { target -> target.scopeKeys.size }

        return nearestTarget?.let { target -> listOf(target) } ?: targets
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

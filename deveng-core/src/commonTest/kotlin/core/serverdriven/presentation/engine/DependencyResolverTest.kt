package core.serverdriven.presentation.engine

import core.serverdriven.presentation.model.DependencyEffect
import core.serverdriven.presentation.model.DependencyOperator
import core.serverdriven.presentation.model.DependencyUiModel
import kotlin.test.Test
import kotlin.test.assertEquals

class DependencyResolverTest {

    private val dependencyResolver = DependencyResolver()

    @Test
    fun scopeDependencies_withSourceAndTargetInEveryTab_pairsThemPerTab() {
        val scopedDependencies = dependencyResolver.scopeDependencies(
            dependencies = listOf(dependency()),
            scopedComponentIds = listOf(
                tabScopedId(DAY_TAB_KEY, SOURCE_ID),
                tabScopedId(DAY_TAB_KEY, TARGET_ID),
                tabScopedId(NIGHT_TAB_KEY, SOURCE_ID),
                tabScopedId(NIGHT_TAB_KEY, TARGET_ID)
            )
        )

        assertEquals(
            listOf(
                "$DAY_TAB_KEY/$SOURCE_ID" to "$DAY_TAB_KEY/$TARGET_ID",
                "$NIGHT_TAB_KEY/$SOURCE_ID" to "$NIGHT_TAB_KEY/$TARGET_ID"
            ),
            scopedDependencies.map { dependency -> dependency.sourceId to dependency.targetId }
        )
    }

    @Test
    fun scopeDependencies_withTargetInOwnTabAndOutsideTabs_picksOwnTab() {
        val scopedDependencies = dependencyResolver.scopeDependencies(
            dependencies = listOf(dependency()),
            scopedComponentIds = listOf(
                globalScopedId(TARGET_ID),
                tabScopedId(DAY_TAB_KEY, SOURCE_ID),
                tabScopedId(DAY_TAB_KEY, TARGET_ID)
            )
        )

        assertEquals(
            listOf("$DAY_TAB_KEY/$SOURCE_ID" to "$DAY_TAB_KEY/$TARGET_ID"),
            scopedDependencies.map { dependency -> dependency.sourceId to dependency.targetId }
        )
    }

    @Test
    fun scopeDependencies_withSourceInNestedTab_picksTargetOfEnclosingTabOverSiblingTab() {
        val nestedTabKey = "$DAY_TAB_KEY/$NESTED_TAB_ID"
        val scopedDependencies = dependencyResolver.scopeDependencies(
            dependencies = listOf(dependency()),
            scopedComponentIds = listOf(
                ScopedComponentId(
                    key = "$nestedTabKey/$SOURCE_ID",
                    componentId = SOURCE_ID,
                    scopeKeys = listOf(DAY_TAB_KEY, nestedTabKey)
                ),
                tabScopedId(DAY_TAB_KEY, TARGET_ID),
                tabScopedId(NIGHT_TAB_KEY, TARGET_ID)
            )
        )

        assertEquals(
            listOf("$nestedTabKey/$SOURCE_ID" to "$DAY_TAB_KEY/$TARGET_ID"),
            scopedDependencies.map { dependency -> dependency.sourceId to dependency.targetId }
        )
    }

    @Test
    fun scopeDependencies_withSourceOutsideTabs_reachesTargetInEveryTab() {
        val scopedDependencies = dependencyResolver.scopeDependencies(
            dependencies = listOf(dependency()),
            scopedComponentIds = listOf(
                globalScopedId(SOURCE_ID),
                tabScopedId(DAY_TAB_KEY, TARGET_ID),
                tabScopedId(NIGHT_TAB_KEY, TARGET_ID)
            )
        )

        assertEquals(
            listOf(SOURCE_ID to "$DAY_TAB_KEY/$TARGET_ID", SOURCE_ID to "$NIGHT_TAB_KEY/$TARGET_ID"),
            scopedDependencies.map { dependency -> dependency.sourceId to dependency.targetId }
        )
    }

    @Test
    fun scopeDependencies_withMissingSourceOrTarget_dropsDependency() {
        val scopedDependencies = dependencyResolver.scopeDependencies(
            dependencies = listOf(dependency(), dependency().copy(sourceId = TARGET_ID, targetId = MISSING_ID)),
            scopedComponentIds = listOf(globalScopedId(TARGET_ID))
        )

        assertEquals(emptyList(), scopedDependencies)
    }

    @Test
    fun scopeDependencies_keepsOperatorTriggerAndEffect() {
        val scopedDependency = dependencyResolver.scopeDependencies(
            dependencies = listOf(dependency()),
            scopedComponentIds = listOf(globalScopedId(SOURCE_ID), globalScopedId(TARGET_ID))
        ).single()

        assertEquals(dependency(), scopedDependency)
    }

    private fun dependency() = DependencyUiModel(
        sourceId = SOURCE_ID,
        targetId = TARGET_ID,
        operator = DependencyOperator.EQUALS,
        triggerValue = "true",
        effect = DependencyEffect.EDITABLE,
        effectValue = true
    )

    private fun globalScopedId(componentId: String) = ScopedComponentId(
        key = componentId,
        componentId = componentId,
        scopeKeys = emptyList()
    )

    private fun tabScopedId(tabKey: String, componentId: String) = ScopedComponentId(
        key = "$tabKey/$componentId",
        componentId = componentId,
        scopeKeys = listOf(tabKey)
    )

    private companion object {
        const val SOURCE_ID = "schedule_switch"
        const val TARGET_ID = "start_time_textfield"
        const val MISSING_ID = "missing_text_field"
        const val DAY_TAB_KEY = "day_tab"
        const val NIGHT_TAB_KEY = "night_tab"
        const val NESTED_TAB_ID = "advanced_tab"
    }
}

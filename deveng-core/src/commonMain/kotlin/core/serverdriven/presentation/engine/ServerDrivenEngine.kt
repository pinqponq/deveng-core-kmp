package core.serverdriven.presentation.engine

import core.serverdriven.data.model.response.LayoutResponse
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.ComponentProperty
import core.serverdriven.presentation.model.ComponentUiModel
import core.serverdriven.presentation.model.DependencyUiModel
import core.serverdriven.presentation.model.mapper.ComponentUiMapper
import core.serverdriven.presentation.model.mapper.DependencyUiMapper
import core.util.CustomLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * Holds a server-driven layout: the component tree, each component's value, and the visible,
 * editable and expanded state that the layout's dependencies derive from those values.
 *
 * Component ids must be unique inside one tab; components outside every tab are unique across the
 * layout, including content loaded later into a shutter. A duplicate fails [setLayout] and
 * [handleEvent] with an [IllegalArgumentException]. Values, properties and [ComponentEvent]s address
 * a component by its key: its id outside every tab, `<tab key>/<id>` inside a tab
 * (`day_tab/schedule_switch`), so two tabs with the same id keep separate state.
 *
 * @param shutterContentLoader Loads a shutter's extra content the first time it expands; its
 * components are appended to the shutter's own and its dependencies join the layout's.
 */
class ServerDrivenEngine(
    private val shutterContentLoader: ShutterContentLoader? = null
) {
    private val componentUiMapper = ComponentUiMapper()
    private val dependencyUiMapper = DependencyUiMapper()
    private val dependencyResolver = DependencyResolver()
    private val layoutJson = Json { ignoreUnknownKeys = true }

    private val _components = MutableStateFlow<List<ComponentUiModel>>(emptyList())
    private val _componentValues = MutableStateFlow<Map<String, Any>>(emptyMap())
    private val _componentProperties = MutableStateFlow<Map<String, ComponentProperty>>(emptyMap())
    private val _visibleComponents = MutableStateFlow<List<ComponentUiModel>>(emptyList())
    private val dependencies = MutableStateFlow<List<DependencyUiModel>>(emptyList())
    private val loadedShutterKeys = MutableStateFlow<Set<String>>(emptySet())

    val components: StateFlow<List<ComponentUiModel>> = _components.asStateFlow()
    val componentValues: StateFlow<Map<String, Any>> = _componentValues.asStateFlow()
    val componentProperties: StateFlow<Map<String, ComponentProperty>> =
        _componentProperties.asStateFlow()
    val visibleComponents: StateFlow<List<ComponentUiModel>> = _visibleComponents.asStateFlow()

    fun setLayout(json: String) {
        setLayout(layout = layoutJson.decodeFromString<LayoutResponse>(json))
    }

    fun setLayout(layout: LayoutResponse) {
        val components = componentUiMapper.mapToUiModels(layout.components)
        val scopedComponentIds = collectScopedComponentIds(components, scopeKeys = emptyList())
        validateUniqueKeys(scopedComponentIds)

        val componentValues = buildMap { collectInitialValues(components, scopeKey = null, componentValues = this) }
        val layoutDependencies = dependencyUiMapper.mapToUiModels(layout.dependencies)
        val componentProperties = scopedComponentIds.associate { scopedId -> scopedId.key to ComponentProperty() }
        logDependenciesWithMissingIds(layoutDependencies, scopedComponentIds)

        loadedShutterKeys.value = emptySet()
        dependencies.value = layoutDependencies
        _components.value = components
        _componentValues.value = componentValues
        _componentProperties.value = dependencyResolver.resolveProperties(
            dependencies = dependencyResolver.scopeDependencies(layoutDependencies, scopedComponentIds),
            componentValues = componentValues,
            componentProperties = componentProperties
        )

        publishVisibleComponents()
    }

    suspend fun handleEvent(event: ComponentEvent) {
        when (event) {
            is ComponentEvent.ChangeSliderValue -> updateValue(event.componentId, event.value)
            is ComponentEvent.EnterTextField -> updateValue(event.componentId, event.value)
            is ComponentEvent.ClickSwitch -> updateValue(event.componentId, event.isChecked)
            is ComponentEvent.ClickDropdownOption -> updateValue(event.componentId, event.option)
            is ComponentEvent.ClickTabItem -> selectTab(event.componentId, event.tabId)
            is ComponentEvent.ClickShutter -> toggleShutter(event.componentId)
        }
    }

    private fun updateValue(
        componentKey: String,
        value: Any
    ) {
        val isEditable = _componentProperties.value[componentKey]?.isEditable ?: false

        if (!isEditable) {
            CustomLogger.log("Server-driven UI: ignored a value for '$componentKey', it is not editable")
            return
        }

        _componentValues.update { componentValues -> componentValues + (componentKey to value) }
        resolveDependencies()
    }

    private fun selectTab(
        tabsKey: String,
        tabId: String
    ) {
        val tabs = findComponent(_components.value, tabsKey, scopeKey = null) as? ComponentUiModel.Tabs
        val isTabOfComponent = tabs?.tabs?.any { tab -> tab.id == tabId } ?: false

        require(isTabOfComponent) { "Server-driven UI: '$tabId' is not a tab of '$tabsKey'" }

        _componentValues.update { componentValues -> componentValues + (tabsKey to tabId) }
        resolveDependencies()
    }

    private suspend fun toggleShutter(shutterKey: String) {
        val shutter = findComponent(_components.value, shutterKey, scopeKey = null)

        require(shutter is ComponentUiModel.Shutter) { "Server-driven UI: '$shutterKey' is not a shutter" }

        val isExpanding = _componentProperties.value[shutterKey]?.isExpanded != true
        updateProperty(shutterKey) { property -> property.copy(isExpanded = isExpanding) }

        val loader = shutterContentLoader ?: return
        if (!isExpanding) return

        val isAlreadyLoaded = loadedShutterKeys.getAndUpdate { shutterKeys -> shutterKeys + shutterKey }
            .contains(shutterKey)
        if (isAlreadyLoaded) return

        val shutterContent = try {
            loader.loadShutterContent(shutter.id)
        } catch (exception: Exception) {
            loadedShutterKeys.update { shutterKeys -> shutterKeys - shutterKey }
            updateProperty(shutterKey) { property -> property.copy(isExpanded = false) }
            throw exception
        }

        appendShutterContent(shutterKey, shutterContent)
    }

    private fun appendShutterContent(
        shutterKey: String,
        shutterContent: LayoutResponse
    ) {
        val shutterScopedId = collectScopedComponentIds(_components.value, scopeKeys = emptyList())
            .firstOrNull { scopedId -> scopedId.key == shutterKey }

        if (shutterScopedId == null) {
            CustomLogger.log("Server-driven UI: dropped the loaded content of '$shutterKey', it is no longer in the layout")
            return
        }

        val shutterScopeKeys = shutterScopedId.scopeKeys
        val loadedComponents = componentUiMapper.mapToUiModels(shutterContent.components)
        val loadedScopedIds = collectScopedComponentIds(loadedComponents, shutterScopeKeys)
        val updatedComponents = appendToShutter(_components.value, shutterKey, loadedComponents, scopeKey = null)
        val scopedComponentIds = collectScopedComponentIds(updatedComponents, scopeKeys = emptyList())
        validateUniqueKeys(scopedComponentIds)

        _components.value = updatedComponents

        _componentValues.update { componentValues ->
            componentValues + buildMap {
                collectInitialValues(loadedComponents, scopeKey = shutterScopeKeys.lastOrNull(), componentValues = this)
            }
        }

        _componentProperties.update { componentProperties ->
            componentProperties + loadedScopedIds.associate { scopedId -> scopedId.key to ComponentProperty() }
        }

        val loadedDependencies = dependencyUiMapper.mapToUiModels(shutterContent.dependencies)
        logDependenciesWithMissingIds(loadedDependencies, scopedComponentIds)
        dependencies.update { layoutDependencies -> layoutDependencies + loadedDependencies }
        resolveDependencies()
    }

    private fun resolveDependencies() {
        val scopedDependencies = dependencyResolver.scopeDependencies(
            dependencies = dependencies.value,
            scopedComponentIds = collectScopedComponentIds(_components.value, scopeKeys = emptyList())
        )

        _componentProperties.update { componentProperties ->
            dependencyResolver.resolveProperties(
                dependencies = scopedDependencies,
                componentValues = _componentValues.value,
                componentProperties = componentProperties
            )
        }

        publishVisibleComponents()
    }

    private fun logDependenciesWithMissingIds(
        newDependencies: List<DependencyUiModel>,
        scopedComponentIds: List<ScopedComponentId>
    ) {
        newDependencies
            .filter { dependency ->
                dependencyResolver.scopeDependencies(listOf(dependency), scopedComponentIds).isEmpty()
            }
            .forEach { dependency ->
                CustomLogger.log(
                    "Server-driven UI: dependency '${dependency.sourceId}' -> '${dependency.targetId}' names an id " +
                        "that is not in the layout; it applies once that id is loaded"
                )
            }
    }

    private fun publishVisibleComponents() {
        _visibleComponents.value = withoutHiddenComponents(_components.value, scopeKey = null)
    }

    private fun withoutHiddenComponents(
        components: List<ComponentUiModel>,
        scopeKey: String?
    ): List<ComponentUiModel> {
        return components
            .filter { component ->
                _componentProperties.value[scopedComponentKey(scopeKey, component.id)]?.isVisible ?: true
            }
            .map { component ->
                when (component) {
                    is ComponentUiModel.Shutter ->
                        component.copy(components = withoutHiddenComponents(component.components, scopeKey))

                    is ComponentUiModel.Row ->
                        component.copy(components = withoutHiddenComponents(component.components, scopeKey))

                    is ComponentUiModel.Tabs -> component.copy(
                        tabs = component.tabs.map { tab ->
                            val tabKey = scopedComponentKey(scopeKey, tab.id)
                            tab.copy(components = withoutHiddenComponents(tab.components, tabKey))
                        }
                    )

                    else -> component
                }
            }
    }

    private fun updateProperty(
        componentKey: String,
        transform: (ComponentProperty) -> ComponentProperty
    ) {
        _componentProperties.update { componentProperties ->
            componentProperties + (componentKey to transform(componentProperties.getValue(componentKey)))
        }
    }

    private fun collectInitialValues(
        components: List<ComponentUiModel>,
        scopeKey: String?,
        componentValues: MutableMap<String, Any>
    ) {
        components.forEach { component ->
            val componentKey = scopedComponentKey(scopeKey, component.id)

            when (component) {
                is ComponentUiModel.Slider -> componentValues[componentKey] = component.currentValue
                is ComponentUiModel.Switch -> componentValues[componentKey] = component.currentValue
                is ComponentUiModel.TextField -> componentValues[componentKey] = component.currentValue
                is ComponentUiModel.Dropdown -> componentValues[componentKey] = component.currentValue
                is ComponentUiModel.Tabs -> {
                    component.tabs.firstOrNull()?.let { firstTab -> componentValues[componentKey] = firstTab.id }
                    component.tabs.forEach { tab ->
                        collectInitialValues(tab.components, scopedComponentKey(scopeKey, tab.id), componentValues)
                    }
                }

                is ComponentUiModel.Shutter -> collectInitialValues(component.components, scopeKey, componentValues)
                is ComponentUiModel.Row -> collectInitialValues(component.components, scopeKey, componentValues)
                is ComponentUiModel.Divider -> Unit
            }
        }
    }

    private fun collectScopedComponentIds(
        components: List<ComponentUiModel>,
        scopeKeys: List<String>
    ): List<ScopedComponentId> {
        val scopeKey = scopeKeys.lastOrNull()

        return components.flatMap { component ->
            val scopedId = ScopedComponentId(
                key = scopedComponentKey(scopeKey, component.id),
                componentId = component.id,
                scopeKeys = scopeKeys
            )
            val nestedScopedIds = when (component) {
                is ComponentUiModel.Tabs -> component.tabs.flatMap { tab ->
                    val tabKey = scopedComponentKey(scopeKey, tab.id)
                    val tabScopedId = ScopedComponentId(key = tabKey, componentId = tab.id, scopeKeys = scopeKeys)
                    listOf(tabScopedId) + collectScopedComponentIds(tab.components, scopeKeys + tabKey)
                }

                is ComponentUiModel.Shutter -> collectScopedComponentIds(component.components, scopeKeys)
                is ComponentUiModel.Row -> collectScopedComponentIds(component.components, scopeKeys)
                else -> emptyList()
            }

            listOf(scopedId) + nestedScopedIds
        }
    }

    private fun validateUniqueKeys(scopedComponentIds: List<ScopedComponentId>) {
        val duplicateKeys = scopedComponentIds.groupingBy { scopedId -> scopedId.key }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys

        require(duplicateKeys.isEmpty()) {
            "Server-driven UI: component ids must be unique inside a tab and outside the tabs, " +
                "duplicated: ${duplicateKeys.joinToString()}"
        }
    }

    private fun findComponent(
        components: List<ComponentUiModel>,
        componentKey: String,
        scopeKey: String?
    ): ComponentUiModel? {
        components.forEach { component ->
            if (scopedComponentKey(scopeKey, component.id) == componentKey) return component

            val nestedComponent = when (component) {
                is ComponentUiModel.Tabs -> component.tabs.firstNotNullOfOrNull { tab ->
                    findComponent(tab.components, componentKey, scopedComponentKey(scopeKey, tab.id))
                }

                is ComponentUiModel.Shutter -> findComponent(component.components, componentKey, scopeKey)
                is ComponentUiModel.Row -> findComponent(component.components, componentKey, scopeKey)
                else -> null
            }

            if (nestedComponent != null) return nestedComponent
        }

        return null
    }

    private fun appendToShutter(
        components: List<ComponentUiModel>,
        shutterKey: String,
        loadedComponents: List<ComponentUiModel>,
        scopeKey: String?
    ): List<ComponentUiModel> {
        return components.map { component ->
            when (component) {
                is ComponentUiModel.Shutter -> if (scopedComponentKey(scopeKey, component.id) == shutterKey) {
                    component.copy(components = component.components + loadedComponents)
                } else {
                    component.copy(
                        components = appendToShutter(component.components, shutterKey, loadedComponents, scopeKey)
                    )
                }

                is ComponentUiModel.Row -> component.copy(
                    components = appendToShutter(component.components, shutterKey, loadedComponents, scopeKey)
                )

                is ComponentUiModel.Tabs -> component.copy(
                    tabs = component.tabs.map { tab ->
                        val tabKey = scopedComponentKey(scopeKey, tab.id)
                        tab.copy(components = appendToShutter(tab.components, shutterKey, loadedComponents, tabKey))
                    }
                )

                else -> component
            }
        }
    }
}

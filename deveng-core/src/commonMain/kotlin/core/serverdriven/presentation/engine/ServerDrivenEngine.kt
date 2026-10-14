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
 * Component ids must be unique across the whole layout, including content loaded later into a
 * shutter, because values and dependencies address components by id; a duplicate id fails
 * [setLayout] and [handleEvent] with an [IllegalArgumentException].
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
    private val dependencies = MutableStateFlow<List<DependencyUiModel>>(emptyList())
    private val loadedShutterIds = MutableStateFlow<Set<String>>(emptySet())

    val components: StateFlow<List<ComponentUiModel>> = _components.asStateFlow()
    val componentValues: StateFlow<Map<String, Any>> = _componentValues.asStateFlow()
    val componentProperties: StateFlow<Map<String, ComponentProperty>> =
        _componentProperties.asStateFlow()

    fun setLayout(json: String) {
        setLayout(layout = layoutJson.decodeFromString<LayoutResponse>(json))
    }

    fun setLayout(layout: LayoutResponse) {
        val components = componentUiMapper.mapToUiModels(layout.components)
        val componentIds = collectComponentIds(components)
        validateUniqueIds(componentIds)

        val componentValues = buildMap { collectInitialValues(components, this) }
        val layoutDependencies = dependencyUiMapper.mapToUiModels(layout.dependencies)
        val componentProperties = componentIds.associateWith { ComponentProperty() }

        loadedShutterIds.value = emptySet()
        dependencies.value = layoutDependencies
        _components.value = components
        _componentValues.value = componentValues
        _componentProperties.value = dependencyResolver.resolveProperties(
            dependencies = layoutDependencies,
            componentValues = componentValues,
            componentProperties = componentProperties
        )
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
        componentId: String,
        value: Any
    ) {
        val isEditable = _componentProperties.value[componentId]?.isEditable ?: false

        if (!isEditable) {
            CustomLogger.log("Server-driven UI: ignored a value for '$componentId', it is not editable")
            return
        }

        _componentValues.update { componentValues -> componentValues + (componentId to value) }
        resolveDependencies()
    }

    private fun selectTab(
        tabsId: String,
        tabId: String
    ) {
        val tabs = findComponent(_components.value, tabsId) as? ComponentUiModel.Tabs
        val isTabOfComponent = tabs?.tabs?.any { tab -> tab.id == tabId } ?: false

        require(isTabOfComponent) { "Server-driven UI: '$tabId' is not a tab of '$tabsId'" }

        _componentValues.update { componentValues -> componentValues + (tabsId to tabId) }
        resolveDependencies()
    }

    private suspend fun toggleShutter(shutterId: String) {
        val shutter = findComponent(_components.value, shutterId)

        require(shutter is ComponentUiModel.Shutter) { "Server-driven UI: '$shutterId' is not a shutter" }

        val isExpanding = _componentProperties.value[shutterId]?.isExpanded != true
        updateProperty(shutterId) { property -> property.copy(isExpanded = isExpanding) }

        val loader = shutterContentLoader ?: return
        if (!isExpanding) return

        val isAlreadyLoaded = loadedShutterIds.getAndUpdate { shutterIds -> shutterIds + shutterId }
            .contains(shutterId)
        if (isAlreadyLoaded) return

        val shutterContent = try {
            loader.loadShutterContent(shutterId)
        } catch (exception: Exception) {
            loadedShutterIds.update { shutterIds -> shutterIds - shutterId }
            updateProperty(shutterId) { property -> property.copy(isExpanded = false) }
            throw exception
        }

        appendShutterContent(shutterId, shutterContent)
    }

    private fun appendShutterContent(
        shutterId: String,
        shutterContent: LayoutResponse
    ) {
        val loadedComponents = componentUiMapper.mapToUiModels(shutterContent.components)
        val loadedComponentIds = collectComponentIds(loadedComponents)
        validateUniqueIds(collectComponentIds(_components.value) + loadedComponentIds)

        _components.update { components ->
            appendToShutter(components, shutterId, loadedComponents)
        }
        _componentValues.update { componentValues ->
            componentValues + buildMap { collectInitialValues(loadedComponents, this) }
        }
        _componentProperties.update { componentProperties ->
            componentProperties + loadedComponentIds.associateWith { ComponentProperty() }
        }
        dependencies.update { layoutDependencies ->
            layoutDependencies + dependencyUiMapper.mapToUiModels(shutterContent.dependencies)
        }
        resolveDependencies()
    }

    private fun resolveDependencies() {
        _componentProperties.update { componentProperties ->
            dependencyResolver.resolveProperties(
                dependencies = dependencies.value,
                componentValues = _componentValues.value,
                componentProperties = componentProperties
            )
        }
    }

    private fun updateProperty(
        componentId: String,
        transform: (ComponentProperty) -> ComponentProperty
    ) {
        _componentProperties.update { componentProperties ->
            componentProperties + (componentId to transform(componentProperties.getValue(componentId)))
        }
    }

    private fun collectInitialValues(
        components: List<ComponentUiModel>,
        componentValues: MutableMap<String, Any>
    ) {
        components.forEach { component ->
            when (component) {
                is ComponentUiModel.Slider -> componentValues[component.id] = component.currentValue
                is ComponentUiModel.Switch -> componentValues[component.id] = component.currentValue
                is ComponentUiModel.TextField -> componentValues[component.id] = component.currentValue
                is ComponentUiModel.Dropdown -> componentValues[component.id] = component.currentValue
                is ComponentUiModel.Tabs -> {
                    component.tabs.firstOrNull()?.let { firstTab -> componentValues[component.id] = firstTab.id }
                    component.tabs.forEach { tab -> collectInitialValues(tab.components, componentValues) }
                }

                is ComponentUiModel.Shutter -> collectInitialValues(component.components, componentValues)
                is ComponentUiModel.Row -> collectInitialValues(component.components, componentValues)
                is ComponentUiModel.Divider -> Unit
            }
        }
    }

    private fun collectComponentIds(components: List<ComponentUiModel>): List<String> {
        return components.flatMap { component ->
            val tabIds = (component as? ComponentUiModel.Tabs)?.tabs.orEmpty().map { tab -> tab.id }
            listOf(component.id) + tabIds + collectComponentIds(childrenOf(component))
        }
    }

    private fun validateUniqueIds(componentIds: List<String>) {
        val duplicateIds = componentIds.groupingBy { componentId -> componentId }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "Server-driven UI: component ids must be unique, duplicated: ${duplicateIds.joinToString()}"
        }
    }

    private fun findComponent(
        components: List<ComponentUiModel>,
        componentId: String
    ): ComponentUiModel? {
        components.forEach { component ->
            if (component.id == componentId) return component
            findComponent(childrenOf(component), componentId)?.let { child -> return child }
        }
        return null
    }

    private fun appendToShutter(
        components: List<ComponentUiModel>,
        shutterId: String,
        loadedComponents: List<ComponentUiModel>
    ): List<ComponentUiModel> {
        return components.map { component ->
            when (component) {
                is ComponentUiModel.Shutter -> if (component.id == shutterId) {
                    component.copy(components = component.components + loadedComponents)
                } else {
                    component.copy(components = appendToShutter(component.components, shutterId, loadedComponents))
                }

                is ComponentUiModel.Row ->
                    component.copy(components = appendToShutter(component.components, shutterId, loadedComponents))

                is ComponentUiModel.Tabs -> component.copy(
                    tabs = component.tabs.map { tab ->
                        tab.copy(components = appendToShutter(tab.components, shutterId, loadedComponents))
                    }
                )

                else -> component
            }
        }
    }

    private fun childrenOf(component: ComponentUiModel): List<ComponentUiModel> {
        return when (component) {
            is ComponentUiModel.Shutter -> component.components
            is ComponentUiModel.Row -> component.components
            is ComponentUiModel.Tabs -> component.tabs.flatMap { tab -> tab.components }
            else -> emptyList()
        }
    }
}

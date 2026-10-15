# Server-driven UI

The backend sends a layout (a tree of components plus a list of dependencies between them) and the app
draws it with `ServerDrivenScreen`. The engine keeps every component's value and derives which components
are visible, editable and expanded from those values.

Package: `core.serverdriven` — `data.model.response` (wire format), `presentation.model` (UI models and
mappers), `presentation.engine` (`ServerDrivenEngine`), `presentation.component` (`ServerDrivenScreen`).
Networking and endpoints stay in the app.

## Usage

```kotlin
class ConfigurationViewModel(
    private val fetchLayoutUseCase: FetchLayoutUseCase,
    private val fetchShutterContentUseCase: FetchShutterContentUseCase
) : BaseViewModel<State, Event, Effect>(State()) {

    val engine = ServerDrivenEngine(
        shutterContentLoader = { shutterId -> fetchShutterContentUseCase(shutterId) }
    )

    private fun loadLayout() {
        launch { engine.setLayout(layout = fetchLayoutUseCase()) }   // or engine.setLayout(json = "...")
    }

    private fun handleComponentEvent(componentEvent: ComponentEvent) {
        launch { engine.handleEvent(componentEvent) }   // loader errors arrive here
    }
}

// Screen
ServerDrivenScreen(
    engine = viewModel.engine,
    onComponentEvent = { componentEvent -> viewModel.setEvent(Event.HandleComponentEvent(componentEvent)) }
)
```

Observe the result with `engine.componentValues` (id → value) and `engine.componentProperties`
(id → `ComponentProperty(isVisible, isEditable, isExpanded)`); `engine.components` is the whole component tree
and `engine.visibleComponents` the same tree without the hidden components, which `ServerDrivenScreen` draws. The layout's response classes are
`@Serializable`, so the app's service can return `LayoutResponse` directly. The `Json` that decodes it must
use `ignoreUnknownKeys = true` (deveng-networking-kmp and the engine's own parser do).

## Format

```json
{
  "components": [
    {
      "type": "shutter",
      "id": "camera_shutter",
      "title": "Camera settings",
      "description": "Basic camera settings",
      "components": [
        {
          "type": "textfield",
          "id": "camera_name_text_field",
          "title": "Camera name",
          "description": "",
          "currentValue": "Entrance",
          "defaultValue": "",
          "factoryValue": "",
          "hintMessage": "Parking 1",
          "errorMessage": null,
          "format": "TIME",
          "keyboardType": "TEXT"
        },
        { "type": "divider", "id": "camera_divider" },
        {
          "type": "row",
          "id": "record_row",
          "components": [
            { "type": "switch", "id": "record_switch", "title": "Recording", "description": "", "currentValue": true },
            {
              "type": "dropdown",
              "id": "resolution_dropdown",
              "title": "Resolution",
              "description": "",
              "currentValue": "1080p",
              "defaultValue": "1080p",
              "factoryValue": "720p",
              "options": ["480p", "720p", "1080p", "4K"]
            }
          ]
        }
      ]
    },
    {
      "type": "tabs",
      "id": "mode_tabs",
      "tabs": [
        {
          "type": "tab",
          "id": "day_tab",
          "title": "Day",
          "description": "",
          "components": [
            {
              "type": "slider",
              "id": "day_brightness_slider",
              "title": "Brightness",
              "description": "",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 85,
              "defaultValue": 50,
              "factoryValue": 50
            }
          ]
        },
        {
          "type": "tab",
          "id": "night_tab",
          "title": "Night",
          "description": "",
          "components": [
            {
              "type": "slider",
              "id": "night_brightness_slider",
              "title": "Brightness",
              "description": "",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 30,
              "defaultValue": 30,
              "factoryValue": 30
            }
          ]
        }
      ]
    }
  ],
  "dependencies": [
    {
      "sourceId": "record_switch",
      "targetId": "resolution_dropdown",
      "operator": "EQUALS",
      "triggerValue": "false",
      "effectType": "EDITABLE",
      "effectValue": false
    }
  ]
}
```

### Components

| `type` | Fields | Value in `componentValues` | Drawn with |
|---|---|---|---|
| `shutter` | `title`, `description`, `components` | — (expanded state is `ComponentProperty.isExpanded`) | `Shutter` |
| `tabs` | `tabs`: list of tab objects | id of the selected tab, the first tab at start | `TabRow` |
| tab (inside `tabs` only) | `id`, `title`, `description`, `components` | — | content of `TabRow` |
| `row` | `components` | — | `Row`, children share the width equally |
| `slider` | `title`, `description`, `minValue`, `maxValue`, `currentValue`, `defaultValue`, `factoryValue` (integers) | `Float` | `CustomSlider` |
| `switch` | `title`, `description`, `currentValue` (boolean) | `Boolean` | `LabeledSwitch` |
| `textfield` | `title`, `description`, `currentValue`, `defaultValue`, `factoryValue`, `hintMessage`, `errorMessage`, `format`, `keyboardType` | `String` | `CustomTextField` |
| `dropdown` | `title`, `description`, `currentValue`, `defaultValue`, `factoryValue`, `options` | `String` | `CustomDropDownMenu` |
| `divider` | — | — | `HorizontalDivider` |

- `tabs` holds tab objects, not components. A tab object's `"type": "tab"` is accepted and ignored; a tab
  object placed in a `components` list is an unknown component.
- `description` is optional everywhere. The shutter shows it under its title; the other components do not
  draw it.
- `keyboardType`: `TEXT` (default), `NUMERIC`, `PASSWORD`, `EMAIL`. `format`: `PHONE_NUMBER`, `TIME`, `DATE`,
  `DATE_AND_TIME`; it is parsed into `TextField.maskType`, the screen does not apply a mask yet.
- Colors, text styles and spacing come from the app's `AppTheme`/`ComponentTheme`
  (`serverDrivenScreen` holds the column/row spacing and the divider). All texts come from the layout.

### Dependencies

Each dependency reads the value of `sourceId` and sets one property of `targetId`:

- `operator`: `EQUALS`, `GREATER_THAN`, `LESS_THAN`. When both sides are numbers they are compared as numbers
  (slider `50.0` equals `"50"`); otherwise `EQUALS` compares the text (`true` equals `"true"`) and the other two
  never match.
- `effectType`: `VISIBILITY` or `EDITABLE`. A match sets the property to `effectValue`, no match sets it to the
  opposite.
- Dependencies are applied in list order to every change and to the initial values, so a layout opens in its
  correct state. When two dependencies set the same property of the same target, the later one wins.
- A hidden container hides its children. A component that is not editable is drawn disabled, and the engine
  ignores value events for it.
- The selected tab is a value too, so a dependency can use a `tabs` id as its source.

## Rules

- **Ids are unique in the whole layout**, including content a shutter loads later. Values and dependencies
  address components by id; a duplicate would make both ambiguous. `setLayout` (and a shutter load that
  brings a duplicate) fails with an `IllegalArgumentException` naming the duplicate ids. Two tabs that show the
  same setting use two ids (`day_brightness_slider`, `night_brightness_slider`).
- **Unknown values do not break the layout.** A component with an unknown `type` is skipped, a dependency
  with an unknown `operator` or `effectType` is skipped, an unknown `keyboardType` falls back to `TEXT` and an
  unknown `format` to no mask. A dependency whose `sourceId` or `targetId` is not in the layout waits until
  that id is loaded (for example by a shutter). Each case is logged through `CustomLogger`, so an older app
  keeps working when the backend adds something new.
- **Lazy shutter content:** with a `ShutterContentLoader`, the first expand of a shutter calls the loader with
  the shutter id. The returned components are appended after the shutter's own components and the returned
  dependencies join the layout's. Each shutter loads once per `setLayout`; when the loader throws, the shutter
  collapses, the exception reaches the caller of `handleEvent`, and the next expand tries again.

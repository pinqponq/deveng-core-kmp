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
        shutterContentLoader = { shutterId -> fetchShutterContentUseCase(shutterId) },
        onLayoutElementSkip = { skippedElement -> analytics.logSkippedLayoutElement(skippedElement) }
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

Observe the result with `engine.componentValues` (component key → value) and `engine.componentProperties`
(component key → `ComponentProperty(isVisible, isEditable, isExpanded)`). A component's key is its id
outside every tab and `<tab key>/<id>` inside a tab (`day_tab/schedule_switch`, `day_tab/inner_tab/hue_slider`
for a tab inside a tab); `ComponentEvent.componentId` carries the same key; `engine.components` is the whole component tree
and `engine.visibleComponents` the same tree without the hidden components, which `ServerDrivenScreen` draws. The layout's response classes are
`@Serializable`, so the app's service can return `LayoutResponse` directly. The `Json` that decodes it must
use `ignoreUnknownKeys = true` (deveng-networking-kmp and the engine's own parser do).

## Format

The day/night camera profile from the original design document: both tabs use the same ids, and the
two dependencies lock each tab's start and end time while that tab's schedule switch is off.

```json
{
  "components": [
    {
      "type": "tabs",
      "id": "tab_id",
      "tabs": [
        {
          "id": "day_tab",
          "title": "Day Profile (Config 0)",
          "components": [
            {
              "type": "slider",
              "id": "brightness_slider",
              "title": "Brightness",
              "description": "Configure brightness setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "slider",
              "id": "contrast_slider",
              "title": "Contrast",
              "description": "Configure contrast setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "slider",
              "id": "hue_slider",
              "title": "Hue",
              "description": "Configure hue setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "slider",
              "id": "saturation_slider",
              "title": "Saturation",
              "description": "Configure saturation setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "divider",
              "id": "divider_id"
            },
            {
              "type": "switch",
              "id": "schedule_switch",
              "title": "Schedule Settings",
              "description": "Enable Schedule for this Profile",
              "currentValue": true
            },
            {
              "type": "row",
              "id": "row_id",
              "components": [
                {
                  "type": "textfield",
                  "id": "start_time_textfield",
                  "title": "Start Time",
                  "description": "Configure start time settings, default value is 06:00:00",
                  "currentValue": "06:30:00",
                  "defaultValue": "06:00:00",
                  "factoryValue": "06:00:00",
                  "errorMessage": "There is an error",
                  "hintMessage": "Enter Start Time"
                },
                {
                  "type": "textfield",
                  "id": "end_time_textfield",
                  "title": "End Time",
                  "description": "Configure end time settings, default value is 18:00:00",
                  "currentValue": "18:30:00",
                  "defaultValue": "18:00:00",
                  "factoryValue": "18:00:00",
                  "errorMessage": "There is an error",
                  "hintMessage": "Enter End Time"
                }
              ]
            }
          ]
        },
        {
          "id": "night_tab",
          "title": "Night Profile (Config 1)",
          "components": [
            {
              "type": "slider",
              "id": "brightness_slider",
              "title": "Brightness",
              "description": "Configure brightness setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "slider",
              "id": "contrast_slider",
              "title": "Contrast",
              "description": "Configure contrast setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "slider",
              "id": "hue_slider",
              "title": "Hue",
              "description": "Configure hue setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "slider",
              "id": "saturation_slider",
              "title": "Saturation",
              "description": "Configure saturation setting, default value is 40",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 43,
              "defaultValue": 40,
              "factoryValue": 50
            },
            {
              "type": "divider",
              "id": "divider_2_id"
            },
            {
              "type": "switch",
              "id": "schedule_switch",
              "title": "Schedule Settings",
              "description": "Enable Schedule for this Profile",
              "currentValue": true
            },
            {
              "type": "row",
              "id": "row_2_id",
              "components": [
                {
                  "type": "textfield",
                  "id": "start_time_textfield",
                  "title": "Start Time",
                  "description": "Configure start time settings, default value is 18:00:00",
                  "currentValue": "18:30:00",
                  "defaultValue": "18:00:00",
                  "factoryValue": "18:00:00",
                  "errorMessage": "There is an error",
                  "hintMessage": "Enter Start Time"
                },
                {
                  "type": "textfield",
                  "id": "end_time_textfield",
                  "title": "End Time",
                  "description": "Configure end time settings, default value is 06:00:00",
                  "currentValue": "06:30:00",
                  "defaultValue": "06:00:00",
                  "factoryValue": "06:00:00",
                  "errorMessage": "There is an error",
                  "hintMessage": "Enter End Time"
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "dependencies": [
    {
      "sourceId": "schedule_switch",
      "targetId": "start_time_textfield",
      "operator": "EQUALS",
      "triggerValue": "true",
      "effectType": "EDITABLE",
      "effectValue": true
    },
    {
      "sourceId": "schedule_switch",
      "targetId": "end_time_textfield",
      "operator": "EQUALS",
      "triggerValue": "true",
      "effectType": "EDITABLE",
      "effectValue": true
    }
  ]
}
```

A shutter (whose content can also be loaded later, see the rules) and a dropdown:

```json
{
  "type": "shutter",
  "id": "camera_shutter",
  "title": "Camera settings",
  "description": "Basic camera settings",
  "components": [
    {
      "type": "dropdown",
      "id": "resolution_dropdown",
      "title": "Resolution",
      "currentValue": "1080p",
      "defaultValue": "1080p",
      "factoryValue": "720p",
      "options": ["480p", "720p", "1080p", "4K"]
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
| `label` | `text` | — | `Text` with `serverDrivenScreen.labelTextStyle` |
| `section` | `title`, `description`, `components` | — | `RoundedSurface` with a title, does not collapse |
| `segmented` | `title`, `description`, `currentValue`, `defaultValue`, `factoryValue`, `options` | `String` | `ChipsMenu` |
| `radio` | `title`, `description`, `currentValue`, `defaultValue`, `factoryValue`, `options` | `String` | `OptionItemList` with check icons |
| `rating` | `title`, `description`, `maxValue` (integer, default 5), `currentValue`, `defaultValue`, `factoryValue` (numbers) | `Float` (whole stars) | `RatingRow` |
| `date` | `title`, `description`, `currentValue`, `defaultValue`, `factoryValue` (ISO-8601 dates; missing or `""` = no date), `targetDates` | `String` (ISO-8601, `2026-10-15`), no entry while no date is selected | `CustomDatePicker` (`PickerField` + date dialog) |

- Write the tab component's type as `tabs`. `tabs` holds tab objects, not components; a tab object needs no
  `type` (a `"type": "tab"` on it is ignored). A component written as `"type": "tab"` (as in parts of the
  original design document) is read as `tabs` and logged once as deprecated.
- `description` is optional everywhere. The shutter, section, segmented, radio and rating components show it
  under their title; the other components do not draw it.
- Value events: `segmented` and `radio` send `ClickDropdownOption`, `rating` sends `ChangeSliderValue` (the star
  count; tapping the selected star again sends `0`), `date` sends `EnterTextField` with the ISO-8601 date. A
  `segmented` or `radio` that is not editable looks the same but sends nothing.
- A `section` is not a scope: its children follow the id rules of the scope around it, like a `row` or `shutter`.
- `date`: `targetDates` is `PAST` (default) or `FUTURE` and limits the days the dialog offers. A `currentValue`
  that is not an ISO-8601 date opens with no date selected and is logged; an unknown `targetDates` falls back to
  `PAST`.
- `keyboardType`: `TEXT` (default), `NUMERIC`, `PASSWORD`, `EMAIL`. `format`: `PHONE_NUMBER`, `TIME`, `DATE`,
  `DATE_AND_TIME`; it is parsed into `TextField.maskType`, the screen does not apply a mask yet.
- Colors, text styles and spacing come from the app's `AppTheme`/`ComponentTheme`
  (`serverDrivenScreen` holds the column/row spacing, the divider, the label/title/description text styles,
  the header spacing and the section padding). All texts come from the layout.

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
- Dependencies name ids, not keys. Every component with the `sourceId` is a source. Its target is the
  `targetId` in the nearest scope: the source's own tab, then the tabs around it, then outside every tab. When
  none of those has the id, every component with the `targetId` is a target, so a switch outside the tabs can
  hide the same slider in every tab. In the example above `schedule_switch` in the day tab locks only the day
  tab's time fields.

### Version

`version` is an optional integer at the top of the layout (`{ "version": 2, "components": [...] }`);
without it the layout is version 1 (`LayoutResponse.INITIAL_VERSION`). Shutter content uses the same
`LayoutResponse`; its `version` is not used.

Compatibility rule: a layout of a newer version only adds. New optional fields, new component types, new
operators and new effects are additive; an older app ignores unknown fields and skips unknown types,
operators and effects (see Rules). Renaming or removing a field, or changing what an existing value means, is
not allowed under a version bump either: add a new field or type instead.

**Every new component type or operator (and every new effect) is announced with a version bump**, so the
backend can tell from an app's skip reports which layout version it is behind.

| Version | Adds |
|---|---|
| 1 | `shutter`, `tabs`, `row`, `slider`, `switch`, `textfield`, `dropdown`, `divider`; `EQUALS`, `GREATER_THAN`, `LESS_THAN`; `VISIBILITY`, `EDITABLE` |
| 2 | `label`, `section`, `segmented`, `radio`, `rating`, `date` |

## Rules

- **Ids are unique inside one tab.** Two tabs may use the same ids and keep separate values and properties.
  Ids outside every tab (components and tab objects) are unique across the layout. Content a shutter loads
  later joins the shutter's tab and follows the same rule. A duplicate makes `setLayout` (or the
  shutter load that brings it) fail with an `IllegalArgumentException` naming the duplicate keys; a failed load
  leaves the layout unchanged.
- **Unknown values do not break the layout.** A component with an unknown `type` is skipped, a dependency with
  an unknown `operator` or `effectType` is skipped, an unknown `keyboardType` falls back to `TEXT` and an
  unknown `format` to no mask, an unknown `targetDates` to `PAST`. A dependency whose `sourceId` or `targetId`
  is not in the layout waits until that id is loaded (for example by a shutter). Each case is logged through
  `CustomLogger`, so an older app keeps working when the backend adds something new.
- **Skipped types are reported.** `onLayoutElementSkip` receives a `SkippedLayoutElement(kind, name,
  componentId)` for each skipped component type (`COMPONENT_TYPE`, `componentId` = the component's id, `name`
  is `null` when the component has no `type`), operator (`OPERATOR`) or effect (`EFFECT`; for both,
  `componentId` = the dependency's `targetId`). A dependency with an unknown operator and an unknown effect
  reports both. The same element is reported once per `setLayout`, including content shutters load later;
  the next `setLayout` reports again. Reports are sent after the layout (or the loaded content) is applied; a
  layout or content rejected for duplicate ids reports nothing. Send it to analytics so the backend learns which apps miss what.
- **Lazy shutter content:** with a `ShutterContentLoader`, the first expand of a shutter calls the loader with
  the shutter id (not its key). The returned components are appended after the shutter's own components and the returned
  dependencies join the layout's. Each shutter loads once per `setLayout`; when the loader throws, the shutter
  collapses, the exception reaches the caller of `handleEvent`, and the next expand tries again. When `setLayout`
  replaces the layout while a load is running, the loaded content is dropped (and logged); a loader exception
  still reaches the caller.

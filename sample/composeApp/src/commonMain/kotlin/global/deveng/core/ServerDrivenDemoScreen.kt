package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.serverdriven.data.model.response.LayoutResponse
import core.serverdriven.presentation.component.ServerDrivenScreen
import core.serverdriven.presentation.engine.ServerDrivenEngine
import core.serverdriven.presentation.model.SkippedLayoutElement
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val SHUTTER_CONTENT_LOAD_DELAY_MILLIS = 600L
private const val NETWORK_SHUTTER_ID = "network_shutter"

private const val EMPTY_LAYOUT_JSON = """{ "components": [], "dependencies": [] }"""

private const val SERVER_DRIVEN_DEMO_LAYOUT_JSON = """
{
  "components": [
    {
      "type": "shutter",
      "id": "camera_shutter",
      "title": "Camera settings",
      "description": "Name, recording and resolution",
      "components": [
        {
          "type": "textfield",
          "id": "camera_name_text_field",
          "title": "Camera name",
          "currentValue": "Entrance",
          "defaultValue": "",
          "factoryValue": "",
          "hintMessage": "Parking 1",
          "keyboardType": "TEXT"
        },
        { "type": "divider", "id": "camera_divider" },
        {
          "type": "row",
          "id": "record_row",
          "components": [
            { "type": "switch", "id": "record_switch", "title": "Recording", "currentValue": false },
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
      ]
    },
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
    },
    { "type": "switch", "id": "advanced_switch", "title": "Advanced settings", "currentValue": false },
    { "type": "chart", "id": "traffic_chart", "title": "Traffic" },
    {
      "type": "shutter",
      "id": "$NETWORK_SHUTTER_ID",
      "title": "Network",
      "description": "Loaded from the server on first open",
      "components": []
    },
    {
      "type": "section",
      "id": "feedback_section",
      "title": "Feedback",
      "description": "A titled group that does not collapse",
      "components": [
        { "type": "label", "id": "feedback_label", "text": "Tell us how the camera setup went." },
        {
          "type": "segmented",
          "id": "mode_segmented",
          "title": "Mode",
          "currentValue": "Day",
          "defaultValue": "Day",
          "factoryValue": "Day",
          "options": ["Day", "Night", "Auto"]
        },
        {
          "type": "radio",
          "id": "quality_radio",
          "title": "Stream quality",
          "description": "Higher quality uses more data",
          "currentValue": "High",
          "defaultValue": "High",
          "factoryValue": "Low",
          "options": ["Low", "Medium", "High"]
        },
        {
          "type": "rating",
          "id": "setup_rating",
          "title": "Setup",
          "currentValue": 3,
          "defaultValue": 0,
          "factoryValue": 0
        },
        { "type": "label", "id": "thanks_label", "text": "Thanks, glad it went well!" },
        {
          "type": "date",
          "id": "install_date",
          "title": "Installed on",
          "currentValue": "2026-10-01",
          "defaultValue": "",
          "factoryValue": ""
        }
      ]
    }
  ],
  "dependencies": [
    {
      "sourceId": "mode_segmented",
      "targetId": "install_date",
      "operator": "EQUALS",
      "triggerValue": "Night",
      "effectType": "VISIBILITY",
      "effectValue": false
    },
    {
      "sourceId": "setup_rating",
      "targetId": "thanks_label",
      "operator": "GREATER_THAN",
      "triggerValue": "3",
      "effectType": "VISIBILITY",
      "effectValue": true
    },
    {
      "sourceId": "quality_radio",
      "targetId": "setup_rating",
      "operator": "EQUALS",
      "triggerValue": "Low",
      "effectType": "EDITABLE",
      "effectValue": false
    },
    {
      "sourceId": "record_switch",
      "targetId": "resolution_dropdown",
      "operator": "EQUALS",
      "triggerValue": "false",
      "effectType": "EDITABLE",
      "effectValue": false
    },
    {
      "sourceId": "advanced_switch",
      "targetId": "$NETWORK_SHUTTER_ID",
      "operator": "EQUALS",
      "triggerValue": "true",
      "effectType": "VISIBILITY",
      "effectValue": true
    },
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
"""

private const val NETWORK_SHUTTER_CONTENT_JSON = """
{
  "components": [
    {
      "type": "textfield",
      "id": "port_text_field",
      "title": "Port",
      "currentValue": "554",
      "defaultValue": "554",
      "factoryValue": "554",
      "hintMessage": "554",
      "keyboardType": "NUMERIC"
    },
    {
      "type": "dropdown",
      "id": "protocol_dropdown",
      "title": "Protocol",
      "currentValue": "RTSP",
      "defaultValue": "RTSP",
      "factoryValue": "RTSP",
      "options": ["RTSP", "HLS", "WebRTC"]
    }
  ],
  "dependencies": []
}
"""

private val shutterContentJson = Json { ignoreUnknownKeys = true }

private fun formatSkippedElement(skippedElement: SkippedLayoutElement): String {
    return "Skipped ${skippedElement.kind}: '${skippedElement.name}' (${skippedElement.componentId})"
}

/**
 * Playground for [ServerDrivenScreen]: a layout with every component type, a visibility dependency
 * (the advanced shutter only shows while "Advanced settings" is on), an editable dependency
 * (resolution is locked while recording is off), the design document's day/night tabs that repeat
 * the same ids (each tab's schedule switch locks only that tab's times) and a shutter whose content is
 * loaded on its first expand, as a backend would send it. A feedback section shows the label, segmented,
 * radio, rating and date components with their dependencies (night mode hides the date, a rating above
 * three shows a thank-you label, low quality locks the rating). A component of the unknown type `chart` is
 * skipped; the skip reports and the live values are printed under the layout.
 */
@Composable
internal fun ServerDrivenDemoScreen(onBack: () -> Unit) {
    val skippedElements = remember { mutableStateListOf<SkippedLayoutElement>() }
    val engine = remember {
        ServerDrivenEngine(
            shutterContentLoader = { shutterId ->
                delay(SHUTTER_CONTENT_LOAD_DELAY_MILLIS)
                shutterContentJson.decodeFromString(
                    LayoutResponse.serializer(),
                    if (shutterId == NETWORK_SHUTTER_ID) NETWORK_SHUTTER_CONTENT_JSON else EMPTY_LAYOUT_JSON
                )
            },
            onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement }
        ).apply { setLayout(json = SERVER_DRIVEN_DEMO_LAYOUT_JSON) }
    }
    val coroutineScope = rememberCoroutineScope()
    val componentValues by engine.componentValues.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        CustomIconButton(
            icon = Res.drawable.ic_arrow_left,
            iconDescription = "Back",
            onClick = onBack,
        )

        Text(
            text = "Server-driven UI",
            style = CoreMediumTextStyle().copy(fontSize = 20.sp),
        )

        ServerDrivenScreen(
            engine = engine,
            modifier = Modifier.fillMaxWidth(),
            onComponentEvent = { componentEvent ->
                coroutineScope.launch { engine.handleEvent(componentEvent) }
            },
        )

        Text(
            text = skippedElements.joinToString(separator = "\n", transform = ::formatSkippedElement),
            style = CoreRegularTextStyle().copy(fontSize = 12.sp),
        )

        Text(
            text = componentValues.entries.joinToString(separator = "\n") { (componentId, value) ->
                "$componentId = $value"
            },
            style = CoreRegularTextStyle().copy(fontSize = 12.sp),
        )
    }
}

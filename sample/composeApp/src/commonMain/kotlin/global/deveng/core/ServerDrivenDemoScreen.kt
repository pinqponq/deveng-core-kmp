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
      "id": "mode_tabs",
      "tabs": [
        {
          "type": "tab",
          "id": "day_tab",
          "title": "Day",
          "components": [
            {
              "type": "slider",
              "id": "day_brightness_slider",
              "title": "Brightness",
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
          "components": [
            {
              "type": "slider",
              "id": "night_brightness_slider",
              "title": "Brightness",
              "minValue": 0,
              "maxValue": 100,
              "currentValue": 30,
              "defaultValue": 30,
              "factoryValue": 30
            }
          ]
        }
      ]
    },
    { "type": "switch", "id": "advanced_switch", "title": "Advanced settings", "currentValue": false },
    {
      "type": "shutter",
      "id": "$NETWORK_SHUTTER_ID",
      "title": "Network",
      "description": "Loaded from the server on first open",
      "components": []
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
    },
    {
      "sourceId": "advanced_switch",
      "targetId": "$NETWORK_SHUTTER_ID",
      "operator": "EQUALS",
      "triggerValue": "true",
      "effectType": "VISIBILITY",
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

/**
 * Playground for [ServerDrivenScreen]: a layout with every component type, a visibility dependency
 * (the advanced shutter only shows while "Advanced settings" is on), an editable dependency
 * (resolution is locked while recording is off) and a shutter whose content is loaded on its first
 * expand, as a backend would send it. The live values are printed under the layout.
 */
@Composable
internal fun ServerDrivenDemoScreen(onBack: () -> Unit) {
    val engine = remember {
        ServerDrivenEngine(
            shutterContentLoader = { shutterId ->
                delay(SHUTTER_CONTENT_LOAD_DELAY_MILLIS)
                shutterContentJson.decodeFromString(
                    LayoutResponse.serializer(),
                    if (shutterId == NETWORK_SHUTTER_ID) NETWORK_SHUTTER_CONTENT_JSON else EMPTY_LAYOUT_JSON
                )
            }
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
            text = componentValues.entries.joinToString(separator = "\n") { (componentId, value) ->
                "$componentId = $value"
            },
            style = CoreRegularTextStyle().copy(fontSize = 12.sp),
        )
    }
}

package core.serverdriven.presentation.component

internal object ServerDrivenScreenTestLayout {
    const val SHUTTER_ID = "camera_shutter"
    const val SHUTTER_TITLE = "Camera settings"
    const val SWITCH_ID = "record_switch"
    const val TABS_ID = "main_tabs"
    const val NETWORK_TAB_ID = "network_tab"
    const val NETWORK_TAB_TITLE = "Network"

    const val JSON = """
        {
          "components": [
            {
              "type": "shutter", "id": "$SHUTTER_ID", "title": "$SHUTTER_TITLE", "description": "Basic camera settings",
              "components": [
                {
                  "type": "textfield", "id": "camera_name_text_field", "title": "Camera name",
                  "currentValue": "Entrance", "defaultValue": "", "factoryValue": "", "hintMessage": "Parking 1"
                },
                {
                  "type": "row", "id": "record_row",
                  "components": [
                    { "type": "switch", "id": "$SWITCH_ID", "title": "Recording", "currentValue": false },
                    {
                      "type": "dropdown", "id": "resolution_dropdown", "title": "Resolution",
                      "currentValue": "1080p", "defaultValue": "1080p", "factoryValue": "720p",
                      "options": ["720p", "1080p", "4K"]
                    }
                  ]
                }
              ]
            },
            { "type": "divider", "id": "camera_divider" },
            {
              "type": "tabs", "id": "$TABS_ID",
              "tabs": [
                {
                  "type": "tab", "id": "image_tab", "title": "Image",
                  "components": [
                    {
                      "type": "slider", "id": "brightness_slider", "title": "Brightness",
                      "minValue": 0, "maxValue": 100, "currentValue": 85, "defaultValue": 50, "factoryValue": 50
                    }
                  ]
                },
                { "type": "tab", "id": "$NETWORK_TAB_ID", "title": "$NETWORK_TAB_TITLE", "components": [] }
              ]
            }
          ],
          "dependencies": [
            {
              "sourceId": "$SWITCH_ID", "targetId": "resolution_dropdown", "operator": "EQUALS",
              "triggerValue": "false", "effectType": "EDITABLE", "effectValue": false
            }
          ]
        }
    """
}
